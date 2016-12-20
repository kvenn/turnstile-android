package com.vimeo.download_sample.downloadqueue;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.WorkerThread;
import android.text.TextUtils;
import android.text.format.Formatter;

import com.google.gson.annotations.SerializedName;
import com.vimeo.turnstile.BaseTask;
import com.vimeo.turnstile.TaskError;
import com.vimeo.turnstile.utils.TaskLogger;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.InterruptedIOException;
import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;
import okhttp3.internal.Util;
import okio.BufferedSink;
import okio.BufferedSource;
import okio.Okio;

/**
 * Handles the <strike>resumable</strike> task of downloading a remote Vimeo video file to the device's
 * external storage directory
 * <p/>
 * Created by kylevenn on 9/16/15.
 */
public class DownloadTask extends BaseTask {

    public static final String FILE_ADDED = "FILE_ADDED";
    private static final String DOWNLOAD_TASK_LOG = "DOWNLOAD_TASK";
    private static final long serialVersionUID = -6127718047690918603L;

    // -----------------------------------------------------------------------------------------------------
    // Fields
    // -----------------------------------------------------------------------------------------------------
    // <editor-fold desc="Fields">
    @SerializedName("file_url")
    private String mRemoteFileUrl;
    @SerializedName("file_path")
    private String mAbsoluteFilePath;
    @SerializedName("file_size")
    private long mFileSize = -1;
    // </editor-fold>

    // -----------------------------------------------------------------------------------------------------
    // Constructor
    // -----------------------------------------------------------------------------------------------------
    // <editor-fold desc="Constructor">

    /**
     * This constructor is only used when we first construct this class before adding it to the task pool.
     * After first creation, we'll always rely on the persisted value, which requires no constructor.
     */
    public DownloadTask(@NonNull String remoteFileUrl) {
        // Pass the url as the unique id
        super(remoteFileUrl);
        mRemoteFileUrl = remoteFileUrl;
    }
    // </editor-fold>

    // -----------------------------------------------------------------------------------------------------
    // State Modification
    // Any public setters that affect state must be synchronized since they can possibly be modified from
    // multiple different threads.
    // -----------------------------------------------------------------------------------------------------
    // <editor-fold desc="State Modification">

    // TODO: Add in some state modification that would change how it works or the flow 12/20/16 [KV]
    // </editor-fold>

    // -----------------------------------------------------------------------------------------------------
    // Getters
    // -----------------------------------------------------------------------------------------------------
    // <editor-fold desc="Getters">
    @Nullable
    public String getRemoteFileUri() {
        return mRemoteFileUrl;
    }

    /**
     * Return the file size or -1 if one has not been found yet
     */
    public long getFileSize() {
        return mFileSize;
    }

    /**
     * @return the valid {@link File} associated with this task or <code>null</code> if the file is invalid
     * @see #isInvalidFile(File)
     */
    @Nullable
    public File getLocalFile() {
        if (mAbsoluteFilePath != null && !mAbsoluteFilePath.trim().isEmpty()) {
            File file = new File(mAbsoluteFilePath);
            return isInvalidFile(file) ? null : file;
        }
        return null;
    }
    // </editor-fold>

    // -----------------------------------------------------------------------------------------------------
    // File Helpers
    // -----------------------------------------------------------------------------------------------------
    // <editor-fold desc="File Helpers">
    @WorkerThread
    public boolean deleteFile() {
        TaskLogger.getLogger().d("Attempting to delete file for " + getId());
        boolean deleted = false;
        File localFile = getLocalFile();
        if (localFile != null) {
            deleted = localFile.delete();
        }
        TaskLogger.getLogger().d("File Deletion: " + (deleted ? "Success" : "Failure"));
        return deleted;
    }

    public static boolean isInvalidFile(File file) {
        return file == null || !file.isFile();
    }
    // </editor-fold>


    // -----------------------------------------------------------------------------------------------------
    // Task Execution
    // -----------------------------------------------------------------------------------------------------
    // <editor-fold desc="Task Execution">

    @WorkerThread
    @Override
    protected void retry() {
        updateStateForRetry();

        File file = getLocalFile();
        if (file == null) {
            // We haven't actually gotten any bytes yet (or the partially done file was moved). Start over.
            execute();
        } else {
            execute(file.length());
        }
    }

    @WorkerThread
    @Override
    protected void execute() {
        execute(0);
    }

    @WorkerThread
    private void execute(long bytesOnDevice) {

        // If there is no remote file url, we can't execute. We could send a specific failure here.
        if (TextUtils.isEmpty(mRemoteFileUrl)) {
            TaskLogger.getLogger().e("No video file or video in execute!");
            onTaskFailure(DownloadError.GENERIC_ERROR.toTaskError(null));
            return;
        }

        if (mConditions != null && !mConditions.areConditionsMet()) {
            // This task requires network. Don't even bother starting if there is no network [KV] 3/30/16
            TaskLogger.getLogger().d(DownloadError.NETWORK_ERROR.getMessage());
            // We just return because the download manager will know to reflect the state as network failure
            return;
        }

        final int DOWNLOAD_CHUNK_SIZE = 2048; // Same as Okio Segment.SIZE
        final int HTTP_TIMEOUT_SECONDS = 60;

        boolean isRetry = bytesOnDevice != 0;

        BufferedSink localFileSink = null;
        BufferedSource remoteFileSource = null;
        ResponseBody body = null;
        long contentLength = 0;
        File localFile = null;
        try {
            // Replace the slash in the URI because it should just be the file name (not a new directory)
            localFile = getLocalFile();
            boolean invalidFile = localFile == null;
            String remoteFileUri = getRemoteFileUri();
            if (remoteFileUri == null) {
                throw new Exception("No Video File!");
            }
            Request request = new Request.Builder().url(remoteFileUri)
                    .addHeader("Range", "bytes=" + bytesOnDevice + "-")
                    .build();

            OkHttpClient client =
                    new OkHttpClient.Builder().connectTimeout(HTTP_TIMEOUT_SECONDS, TimeUnit.SECONDS)
                            .writeTimeout(HTTP_TIMEOUT_SECONDS, TimeUnit.SECONDS)
                            .readTimeout(HTTP_TIMEOUT_SECONDS, TimeUnit.SECONDS)
                            .build();

            Response response = client.newCall(request).execute();

            if (!response.isSuccessful()) {
                throw new Exception("Bad status code");
            }

            body = response.body();
            contentLength = body.contentLength() + bytesOnDevice;
            remoteFileSource = body.source();

            if (invalidFile && !isRetry) {
                // This isn't a retry and we need to create a file
                TaskLogger.getLogger().d("No local file, creating one now");
                // If we haven't already assigned a directory/file to this task, let's do that now [KV] 3/24/16
                localFile = new File(DownloadManager.getStorageDirectoryWithMostSpace(),
                                     getId().replace("/", ""));
                mAbsoluteFilePath = localFile.getAbsolutePath();
                // Now since we've changed the task, let's persist this change to cache and disk [KV] 3/24/16
                onTaskChange();
            } else if (invalidFile) {
                // This is a retry and the original directory can't be found. This could be because we didn't
                // properly persist the absolute path (unlikely) OR because the file/folder was deleted or removed while
                // we were downloading. [KV] 3/24/16
                throw new FileNotFoundException("The download retry couldn't find the original file. " +
                                                "The directory was either deleted or moved.");
            } else {
                TaskLogger.getLogger().d("File already existed");
            }

            TaskLogger.getLogger().d("Content Length: " + contentLength +
                                     " (" + Formatter.formatShortFileSize(mContext, contentLength) + ")");
            TaskLogger.getLogger().d("File Size: " + localFile.length() +
                                     " (" + Formatter.formatShortFileSize(mContext, localFile.length()) +
                                     ")");

            if (contentLength != 0 && (contentLength == localFile.length())) {
                // If the local file is the size of the remote file, we're done!
                onTaskCompleted();
                return;
            }

            if (bytesOnDevice > 0) {
                localFileSink = Okio.buffer(Okio.appendingSink(localFile));
            } else {
                localFileSink = Okio.buffer(Okio.sink(localFile));
            }

            // Hold on to last reported progress to see if it's necessary to broadcast again
            int lastProgress = -1;
            long count;
            long bytesRead = bytesOnDevice;
            //noinspection NestedAssignment
            while ((count = remoteFileSource.read(localFileSink.buffer(), DOWNLOAD_CHUNK_SIZE)) != -1) {
                if (Thread.currentThread().isInterrupted()) {
                    // Check periodically if thread is interrupted. If so, stop the current thread
                    // This will occur for a cancellation/pause/loss of network
                    throw new InterruptedIOException("Thread manually interrupted");
                }


                localFileSink.flush();
                bytesRead += count; // after flush in case of IOException
                int progress = (int) ((bytesRead * 100) / contentLength);
                if (progress != lastProgress) {
                    lastProgress = progress;
                    if (progress % 5 == 0) {
                        if (progress != 0) {
                            // 0 could be a video that failed at 0% - don't reset retry in that instance
                            // But we reset the retry count every 5 successful percent to accommodate for
                            // videos that have hiccups at multiple points (due to poor network or otherwise)
                            mRetryCount = 0;
                        }
                        TaskLogger.getLogger().d(DOWNLOAD_TASK_LOG + ": " + mId + " at: " + progress + "%");
                    }
                    onTaskProgress(progress);
                }
            }
            // Write the remaining bytes from the source to the sink
            localFileSink.writeAll(remoteFileSource);
            localFileSink.flush();
            onTaskCompleted(); // TODO: check if bytes read == file size or w/e [KV] 3/7/16
        } catch (InterruptedIOException ioex) {
            TaskLogger.getLogger().d("Interrupt");
            // This was most likely because of network or specifically issued interrupt 3/7/16 [KV]
        } catch (FileNotFoundException fnfe) {
            TaskLogger.getLogger().d("Failure " + fnfe.getMessage());
            onTaskFailure(DownloadError.FILE_NOT_FOUND.toTaskError(fnfe));
        } catch (Exception e) {
            // TODO: Optimization: have a flag set for if the file is removable storage (IS_REMOVABLE) [KV] 3/24/16
            // We can then voice to the user that the unfound file is due (potentially) to removing their SD card
            TaskLogger.getLogger().d("Failure " + e.getMessage());
            if (mConditions != null && !mConditions.areConditionsMet()) {
                TaskLogger.getLogger().d(DownloadError.NETWORK_ERROR.getMessage());
                // no-op for network
            } else {
                long remainingFileSize = localFile == null ? 0 : contentLength - localFile.length();
                if (DownloadManager.getAvailableSpace(localFile) <= remainingFileSize && localFile != null &&
                    contentLength > 0) {
                    // If the remaining bytes are larger than the space available, then they've run out of space
                    // mid download.
                    deleteFile();
                    // java.io.IOException: write failed: ENOSPC (No space left on device)
                    onTaskFailure(DownloadError.OUT_OF_SPACE.toTaskError(e));
                } else if (mRetryCount < DEFAULT_NUMBER_RETRIES) {
                    TaskLogger.getLogger().d(DOWNLOAD_TASK_LOG + ": Download retrying");
                    mRetryCount++;
                    try {
                        // Briefly delay the automatic retry attempt (we're on a worker thread)
                        Thread.sleep(1000);
                    } catch (InterruptedException ie) {
                        // If we get interrupted while sleeping, that means this thread was sent an
                        // interrupt. We'll do the same as the above case, which is nothing
                        return;
                    }
                    // We only retry automatically DEFAULT_NUMBER_RETRIES, then we leave it to the user
                    retry();
                } else {
                    onTaskFailure(DownloadError.GENERIC_ERROR.toTaskError(e));
                }
            }
        } catch (OutOfMemoryError oom) {
            // Catching an OOM is literally the worst thing ever, but this is a last resort to make
            // sure the downloads always reflect the correct state (and that they're logged!) 2/3/16 [KV]
            TaskLogger.getLogger().w("Out of Memory");
            onTaskFailure(DownloadError.OOM.toTaskError(null));
        } finally {
            Util.closeQuietly(localFileSink);
            Util.closeQuietly(remoteFileSource);
            if (body != null) {
                body.close();
            }
        }
    }
    // </editor-fold>

    // -----------------------------------------------------------------------------------------------------
    // Download Error
    // -----------------------------------------------------------------------------------------------------
    // <editor-fold desc="Download Error">
    public static final String DOWNLOAD_ERROR_DOMAIN = "DOWNLOAD_ERROR";
    private static final int CODE_NETWORK_ERROR = 0;
    private static final int CODE_INTERRUPT = 1;
    private static final int CODE_GENERIC_ERROR = 2;
    private static final int CODE_BAD_STATUS_CODE = 3;
    private static final int CODE_BAD_URL = 4;
    private static final int CODE_OOM = 5;
    private static final int CODE_OUT_OF_SPACE = 6;
    private static final int CODE_FILE_NOT_FOUND = 7;


    @SuppressWarnings("unused")
    public enum DownloadError {

        // Don't mark as failure
        NETWORK_ERROR(CODE_NETWORK_ERROR, DOWNLOAD_ERROR_DOMAIN, "Network error"),
        INTERRUPT(CODE_INTERRUPT, DOWNLOAD_ERROR_DOMAIN, "Thread interrupted"),

        GENERIC_ERROR(CODE_GENERIC_ERROR, DOWNLOAD_ERROR_DOMAIN, "Generic error"),
        BAD_STATUS_CODE(CODE_BAD_STATUS_CODE, DOWNLOAD_ERROR_DOMAIN, "Bad status code"),
        BAD_URL(CODE_BAD_URL, DOWNLOAD_ERROR_DOMAIN, "Bad url"),
        OOM(CODE_OOM, DOWNLOAD_ERROR_DOMAIN, "Out of memory"),
        OUT_OF_SPACE(CODE_OUT_OF_SPACE, DOWNLOAD_ERROR_DOMAIN, "Out of space"),
        FILE_NOT_FOUND(CODE_FILE_NOT_FOUND, DOWNLOAD_ERROR_DOMAIN, "Retry File Not Found");

        // We can't use ordinal because this object is persisted, which means an ordinal could get out of
        // sync if we change the order at all 2/29/16 [KV]
        private final int mCode;
        private final String mDomain;
        private final String mMessage;

        DownloadError(int code, String domain, String message) {
            mCode = code;
            mDomain = domain;
            mMessage = message;
        }

        public TaskError toTaskError(@Nullable Exception exception) {
            if (exception == null) {
                exception = new Exception(mMessage);
            }
            return new TaskError(mDomain, mCode, mMessage, exception);
        }

        public static DownloadError fromTaskError(TaskError taskError) {
            switch (taskError.getCode()) {
                case CODE_NETWORK_ERROR:
                    return DownloadError.NETWORK_ERROR;
                case CODE_INTERRUPT:
                    return DownloadError.INTERRUPT;
                case CODE_BAD_STATUS_CODE:
                    return DownloadError.BAD_STATUS_CODE;
                case CODE_BAD_URL:
                    return DownloadError.BAD_URL;
                case CODE_OOM:
                    return DownloadError.OOM;
                case CODE_OUT_OF_SPACE:
                    return DownloadError.OUT_OF_SPACE;
                case CODE_FILE_NOT_FOUND:
                    return DownloadError.FILE_NOT_FOUND;
                case CODE_GENERIC_ERROR:
                default:
                    return DownloadError.GENERIC_ERROR;
            }
        }

        public int getCode() {
            return mCode;
        }

        public String getDomain() {
            return mDomain;
        }

        public String getMessage() {
            return mMessage;
        }
    }
    // </editor-fold>
}
