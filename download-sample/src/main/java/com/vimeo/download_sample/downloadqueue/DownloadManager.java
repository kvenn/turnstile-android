package com.vimeo.download_sample.downloadqueue;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Build.VERSION;
import android.os.Build.VERSION_CODES;
import android.os.Environment;
import android.os.StatFs;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.content.ContextCompat;
import android.text.TextUtils;
import android.text.format.Formatter;

import com.vimeo.download_sample.downloadqueue.DownloadTask.DownloadError;
import com.vimeo.download_sample.downloadqueue.exception.DownloadException;
import com.vimeo.download_sample.downloadqueue.exception.NullRemoteFileUrlException;
import com.vimeo.turnstile.BaseTaskManager;
import com.vimeo.turnstile.utils.TaskLogger;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * A task manager that manages {@link DownloadTask}s for downloading videos
 * <p/>
 * Created by kylevenn on 9/16/15.
 */
public final class DownloadManager extends BaseTaskManager<DownloadTask> {

    public enum DownloadState {
        COMPLETE,
        DOES_NOT_EXIST,
        DOWNLOADING,
        ERROR_GENERIC,
        ERROR_OUT_OF_SPACE,
        MISSING_FILE,
        PAUSED_FOR_WIFI,
        PAUSED_NO_CONNECTION
    }

    private static final String LOG_TAG = "DownloadManager";
    private static final String DOWNLOAD_MANAGER_NAME = "dl_manager";
    // Something nice and inconspicuous
    private static final String DOWNLOAD_STORAGE_DIRECTORY_NAME = "example_downloads";
    private static final String ALLOW_HD_DOWNLOADS = "ALLOW_HD_DOWNLOADS";
    private static final String PREF_FILE_NAME = "DL_PREF";
    private static DownloadManager sInstance;
    private final SharedPreferences mSharedPreferences;

    // -----------------------------------------------------------------------------------------------------
    // Initialization
    // -----------------------------------------------------------------------------------------------------
    // <editor-fold desc="Initialization">
    private DownloadManager(Context context) {
        this(new Builder(context));
    }

    private DownloadManager(Builder builder) {
        super(builder);
        mSharedPreferences = mContext.getSharedPreferences(PREF_FILE_NAME, Context.MODE_PRIVATE);
    }

    public static void initialize(Context context) {
        if (sInstance == null) {
            sInstance = new DownloadManager(context);
        }
    }

    public static void initialize(Builder taskManagerBuilder) {
        if (sInstance == null) {
            sInstance = new DownloadManager(taskManagerBuilder);
        }
    }

    public static synchronized DownloadManager getInstance() {
        if (sInstance == null) {
            throw new AssertionError("Instance must be configured before use");
        }
        return sInstance;
    }
    // </editor-fold>

    // -----------------------------------------------------------------------------------------------------
    // Preferences
    // -----------------------------------------------------------------------------------------------------
    // <editor-fold desc="Preferences">
    public boolean canDownloadHd() {
        return mSharedPreferences.getBoolean(ALLOW_HD_DOWNLOADS, false);
    }

    public void setCanDownloadHd(boolean canDownloadHd) {
        mSharedPreferences.edit().putBoolean(ALLOW_HD_DOWNLOADS, canDownloadHd).apply();
    }
    // </editor-fold>

    // -----------------------------------------------------------------------------------------------------
    // Download Task
    // -----------------------------------------------------------------------------------------------------
    // <editor-fold desc="Download Task">

    /**
     * Returns if the task was able to be started
     */
    public void startDownloadTask(String remoteFileUrl) throws DownloadException {
        if (remoteFileUrl == null || TextUtils.isEmpty(remoteFileUrl)) {
            throw new NullRemoteFileUrlException("Provided remoteFileUrl is null or is empty");
        }


//        if (getAvailableSpaceInStorageDirectory() < videoFile.getSize()) {
//            // As of right now, it's possible for the best storage directory to change from this call to the
//            // actual directory selection in the task. This will be handled by an out of space which would
//            // happen anyways.
//            throw new OutOfSpaceException(
//                    "The file you're trying to download is larger than the available space on disk");
//        }
        addTask(new DownloadTask(remoteFileUrl));
    }

    /** For when a file is moved/corrupted and we need to remove it and add it again */
    public void redownloadFile(String remoteFileUrl) throws DownloadException {
        cancelTask(remoteFileUrl);
        startDownloadTask(remoteFileUrl);
    }
    // </editor-fold>

    // -----------------------------------------------------------------------------------------------------
    // Abstract
    // -----------------------------------------------------------------------------------------------------
    // <editor-fold desc="Abstract">
    @Override
    protected Class<DownloadService> getServiceClass() {
        return DownloadService.class;
    }

    @NonNull
    @Override
    protected String getManagerName() {
        return DOWNLOAD_MANAGER_NAME;
    }

    @NonNull
    @Override
    protected Class<DownloadTask> getTaskClass() {
        return DownloadTask.class;
    }
    // </editor-fold>

    // -----------------------------------------------------------------------------------------------------
    // Getters
    // -----------------------------------------------------------------------------------------------------
    // <editor-fold desc="Getters">

    /**
     * Get the video associated with the given DownloadTask represented by taskId
     *
     * @return null if the task is not in the process of downloading or if there is no video on the task
     */
    @Nullable
    public File getFile(String taskId) {
        DownloadTask task = mTaskCache.get(taskId);
        return task == null ? null : task.getLocalFile();
    }

    /**
     * If the task exists, see if the video is valid
     *
     * @param taskId the video resource_key
     * @return true if the task exists and the Video and VideoFile exist
     */
    public boolean isFileValid(String taskId) {
        DownloadTask task = mTaskCache.get(taskId);
        return !(task == null || task.getLocalFile() == null) && task.getLocalFile().isFile();
    }

    @NonNull
    public ArrayList<File> getFiles() {
        ArrayList<File> files = new ArrayList<>();

        for (DownloadTask task : getDateOrderedTaskList()) {
            files.add(task.getLocalFile());
        }

        return files;
    }
    // </editor-fold>

    // -----------------------------------------------------------------------------------------------------
    // Cancellation
    // -----------------------------------------------------------------------------------------------------
    // <editor-fold desc="Cancellation">

    @Override
    public void cancelTask(@NonNull String id) {
        // Schedule interrupt to thread so it doesn't throw an error when the file is deleted
        removeFromTaskPool(id);
        final DownloadTask task = mTaskCache.get(id);
        if (task != null) {
            new Thread(new Runnable() {
                @Override
                public void run() {
                    task.deleteFile();
                }
            }).start();
        }
        // Super will redundantly try to remove from pool, but that's okay [KV] 3/26/16
        super.cancelTask(id);
    }

    @Override
    public void cancelAll() {
        // Schedule interrupt to all threads so they don't throw an error when the file is deleted
        removeAllFromTaskPool();
        // Make a copy of the task array in the map because we're about clear it out with the call
        // to super [KV] 4/4/16
        final List<DownloadTask> tasksCopy = new ArrayList<>(mTaskCache.getTasks().values());
        new Thread(new Runnable() {
            @Override
            public void run() {
                // Deleting files takes time - this should be done on a background thread
                for (DownloadTask task : tasksCopy) {
                    if (task != null) {
                        task.deleteFile();

                    }
                }
            }
        }).start();
        // Super will redundantly try to remove from pool, but that's okay [KV] 3/26/16
        super.cancelAll();
    }

    // </editor-fold>

    // -----------------------------------------------------------------------------------------------------
    // Download Location
    // -----------------------------------------------------------------------------------------------------
    // <editor-fold desc="Download Location">

    /** Useful for checking if there is available space in the storage directory up front */
    public static long getAvailableSpaceInStorageDirectory() {
        // TODO: This doesn't account for actively downloading videos - implement when we implement resumability (officially..) [KV] 3/17/16
        // http://developer.android.com/guide/topics/data/data-storage.html
        return getAvailableSpace(getStorageDirectoryWithMostSpace());
    }

    /** Returns the storage location for downloads. Manager must be initialized before use */
    @Nullable
    public static File getStorageDirectoryWithMostSpace() {
        return getStorageDirectoryWithMostSpace(getInstance().mContext);
    }

    /**
     * Returns the storage location for downloads.
     * This could rely on a preference and change where users want their downloads to go
     */
    @Nullable
    public static File getStorageDirectoryWithMostSpace(Context context) {
        File[] externalFilesDirs = ContextCompat.getExternalFilesDirs(context, null);
        File defaultExternalFilesDir = context.getExternalFilesDir(null);
        File largestDir;
        TaskLogger.getLogger().d("External Dir Count: " + externalFilesDirs.length);
        TaskLogger.getLogger().d("Default Emulated?: " + Environment.isExternalStorageEmulated());
        if (defaultExternalFilesDir == null || Environment.isExternalStorageEmulated()) {
            // If the default external storage is non existent or emulated, then it's better to store the file in getFilesDir
            // since emulated storage is backed by private internal storage
            // http://developer.android.com/reference/android/content/Context.html#getExternalFilesDir(java.lang.String)
            largestDir = context.getFilesDir();
            TaskLogger.getLogger().d("Dir Initially Set To Private");
        } else {
            largestDir = defaultExternalFilesDir;
        }
        TaskLogger.getLogger()
                .d("Default Available Space: " +
                   Formatter.formatShortFileSize(context, getAvailableSpace(largestDir)));

        // If there was only one file in the external files dir, then it's just the default one.
        // This means we've already selected the best possible storage directory above and the loop below won't run
        // http://developer.android.com/reference/android/content/Context.html#getExternalFilesDirs(java.lang.String)
        for (int i = 1; i < externalFilesDirs.length; i++) {
            File currentDir = externalFilesDirs[i];
            TaskLogger.getLogger().d(i + " Dir Size: " +
                                     Formatter.formatShortFileSize(context, getAvailableSpace(currentDir)));
            if (largestDir == null) {
                // If default returned null, lets use whatever else there is
                largestDir = currentDir;
                continue;
            }
            if (currentDir != null && getAvailableSpace(currentDir) > getAvailableSpace(largestDir)) {
                // If the current file directory we're iterating over has more available space, let's select
                // that as the preferred location
                if (VERSION.SDK_INT >= VERSION_CODES.LOLLIPOP &&
                    Environment.isExternalStorageEmulated(currentDir)) {
                    TaskLogger.getLogger().d(i + " Dir was emulated");
                    // We can only check if a particular directory is emulated on > api 21. The fallback is to
                    // use the externalFilesDir (which is fine, just less secure)
                    largestDir = context.getFilesDir();
                } else {
                    largestDir = currentDir;
                }
            }
        }
        File chosenDir = null;
        if (largestDir != null) {
            // There was an external directory, so lets create a hidden folder in it
            chosenDir = new File(largestDir + File.separator + DOWNLOAD_STORAGE_DIRECTORY_NAME);
            // Make the hidden folder
            //noinspection ResultOfMethodCallIgnored
            chosenDir.mkdirs();
        }

        if (chosenDir == null || !chosenDir.exists()) {
            chosenDir = largestDir;
        }
        TaskLogger.getLogger()
                .d("Chose directory: " + (chosenDir != null ? chosenDir.getAbsolutePath() : "NULL"));
        return chosenDir;
    }

    public static long getAvailableSpace(@Nullable File file) {
        if (file == null) {
            return 0;
        }
        try {
            StatFs stat = new StatFs(file.getPath());
            long bytesAvailable;
            if (VERSION.SDK_INT >= VERSION_CODES.JELLY_BEAN_MR2) {
                bytesAvailable = stat.getBlockSizeLong() * stat.getAvailableBlocksLong();
            } else {
                bytesAvailable = (long) stat.getBlockSize() * (long) stat.getAvailableBlocks();
            }
            return bytesAvailable;
        } catch (IllegalArgumentException e) {
            TaskLogger.getLogger().e("File was invalid", e);
            return 0;
        }
    }
    // </editor-fold>

    // -----------------------------------------------------------------------------------------------------
    // Helpers
    // -----------------------------------------------------------------------------------------------------
    // <editor-fold desc="Helpers">
    public DownloadState getDownloadStateForVideo(@Nullable String remoteFileUrl) {
        DownloadState downloadState = DownloadState.DOES_NOT_EXIST;
        DownloadTask downloadTask = remoteFileUrl != null ? getInstance().getTask(remoteFileUrl) : null;
        if (downloadTask != null) {
            if (downloadTask.isComplete()) {
                if (downloadTask.getLocalFile() == null) {
                    downloadState = DownloadState.MISSING_FILE;
                } else {
                    downloadState = DownloadState.COMPLETE;
                }
            } else if (downloadTask.isError()) {
                if (DownloadError.fromTaskError(downloadTask.getTaskError()) == DownloadError.OUT_OF_SPACE) {
                    downloadState = DownloadState.ERROR_OUT_OF_SPACE;
                } else {
                    downloadState = DownloadState.ERROR_GENERIC;
                }
            } else {
                if (areDeviceConditionsMet()) {
                    downloadState = DownloadState.DOWNLOADING;
                } else if (wifiOnly()) {
                    downloadState = DownloadState.PAUSED_FOR_WIFI;
                } else {
                    downloadState = DownloadState.PAUSED_NO_CONNECTION;
                }
            }
        }

        return downloadState;
    }
    // </editor-fold>
}
