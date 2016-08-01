/*
 * The MIT License (MIT)
 * <p/>
 * Copyright (c) 2016 Vimeo
 * <p/>
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * <p/>
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 * <p/>
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package com.vimeo.turnstile;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.IBinder;
import android.support.annotation.DrawableRes;
import android.support.annotation.MainThread;
import android.support.annotation.Nullable;
import android.support.annotation.PluralsRes;
import android.support.annotation.StringRes;

/**
 * The sole purpose of this {@link Service} is to ensure that our application
 * remains in memory and does not get killed by the operating system. This
 * service is tied very closely to the it's respective {@link BaseTaskManager}
 * implementation and will remain running as long as there are still tasks
 * left to run. We call {@link #startForeground(int, Notification)} to reduce
 * the chance that this service/the application will get killed.
 * <p/>
 * The service is started whenever a new upload task is added or whenever the
 * device boots up.
 * <p/>
 * Created by kylevenn on 3/1/16.
 */
public abstract class BaseTaskService<T extends BaseTask> extends Service {

    /**
     * Unique id for the notification. We use it on notification start and to cancel it.
     */
    private int mProgressNotificationId;
    private int mFinishedNotificationId;

    protected BaseTaskManager<T> mTaskManager;

    // ---- Notification Strings ----
    private String mFinishedNotificationTitleString;
    private String mNetworkNotificationMessageString;

    // ---- Notification Building ----
    private NotificationManager mNotificationManager;
    private Notification.Builder mProgressNotificationBuilder;
    private boolean mNotificationShowing;

    // ---- Task Counts ----
    private int mFinishedCount;
    private int mTotalTaskCount;
    @Nullable
    private String mTaskIdToListenOn;

    /*
     * -----------------------------------------------------------------------------------------------------
     * Lifecycle
     * -----------------------------------------------------------------------------------------------------
     */
    // <editor-fold desc="Lifecycle">
    @Override
    public final void onCreate() {
        super.onCreate();
        TaskLogger.getLogger().d("Task Service onCreate");
        // The application will have already initialized the manager at this point 2/29/16 [KV]
        mTaskManager = getManagerInstance();

        mFinishedCount = 0;
        mTotalTaskCount = mTaskManager.getTasksToRun().size();

        registerReceivers();

        // ---- Notification Setup ----
        mProgressNotificationId = getProgressNotificationId();
        mFinishedNotificationId = getFinishedNotificationId();
        mFinishedNotificationTitleString = getString(getFinishedNotificationTitleStringRes());
        mNetworkNotificationMessageString = getString(getNetworkNotificationMessageStringRes());

        mNotificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        setupNotification();
    }

    // Called when there's the possibility of a task running (any call to startService())
    // - BootReceived or TaskAdded
    @Override
    public final int onStartCommand(Intent intent, int flags, int startId) {
        TaskLogger.getLogger().d("Task Service onStartCommand");

        // TODO: This is going to get called A LOT because we issue startService commands for every added task as
        // well as every single state change. Is that bad? We can make optimizations if performance is an issue or
        // our data set increases 3/1/16 [KV]
        if (mTaskManager.tasksRemaining()) {
            // If there are tasks remaining, that means this service should be running and showing the
            // notification
            showNotification();
            // This will tell the upload manager to add any tasks that need to be run that aren't running
            // If tasks are already executing, it will be a no-op 3/1/16 [KV]
            mTaskManager.resumeAllIfNecessary();
        } else {
            // No tasks should be running, so make sure the service dies
            killService();
        }

        // If the service is killed before it's done, this tells the OS to recreate the service after it has
        // enough memory and call onStartCommand() again with a null intent
        return START_STICKY;
    }

    @Override
    public final void onDestroy() {
        stopForeground(true);
        unregisterReceivers();
    }

    @Nullable
    @Override
    public final IBinder onBind(Intent intent) {
        // No one binds to this service
        return null;
    }
    // </editor-fold>

    /*
     * -----------------------------------------------------------------------------------------------------
     * Abstract
     * -----------------------------------------------------------------------------------------------------
     */
    // <editor-fold desc="Abstract">
    protected abstract void handleAdditionalEvents(String event);

    protected abstract BaseTaskManager<T> getManagerInstance();

    // -----------------------------------------------------------------------------------------------------
    // Finished Notification
    // -----------------------------------------------------------------------------------------------------
    // <editor-fold desc="Finished Notification">

    /**
     * The id for the notification of completion. Must not
     * be zero if you wish to show the notification correctly.
     *
     * @return the id of the notification.
     */
    protected abstract int getFinishedNotificationId();

    /**
     * The title of the completed task notification.
     *
     * @return the string resource for the completed task notification.
     */
    @StringRes
    protected abstract int getFinishedNotificationTitleStringRes();

    /**
     * The icon for the finished task notification.
     *
     * @return the id of the drawable to use for the finished notification.
     */
    @DrawableRes
    protected abstract int getFinishedIconDrawable();

    // </editor-fold>

    /**
     * The title of the notification when the device conditions (e.g.
     * network) are not suitable to complete the task.
     *
     * @return the string resource for the device condition notification.
     */
    @StringRes
    protected abstract int getNetworkNotificationMessageStringRes();

    // -----------------------------------------------------------------------------------------------------
    // Progress Notification
    // -----------------------------------------------------------------------------------------------------
    // <editor-fold desc="Progress Notification">

    /**
     * The id for the notification of progress. Must not
     * be zero if you wish to show the notification correctly.
     *
     * @return the id of the notification.
     */
    protected abstract int getProgressNotificationId();

    /**
     * The title of the progress notification. This title will
     * be used in conjunction with the number of tasks. An example
     * string would be "X tasks are running" or "one task running."
     *
     * @return the plural string resource for the progress notification.
     */
    @PluralsRes
    protected abstract int getProgressNotificationTitleStringRes();

    /**
     * The icon for the progress notification.
     *
     * @return the id of the drawable to use for the progress notification.
     */
    @DrawableRes
    protected abstract int getProgressIconDrawable();
    // </editor-fold>

    // </editor-fold>

    /*
     * -----------------------------------------------------------------------------------------------------
     * Service
     * -----------------------------------------------------------------------------------------------------
     */
    // <editor-fold desc="Service">
    private void killService() {
        // Service should be dead
        mNotificationShowing = false;
        stopForeground(true);
        stopSelf();
    }
    // </editor-fold>

    /*
     * -----------------------------------------------------------------------------------------------------
     * Notifications
     * -----------------------------------------------------------------------------------------------------
     */
    // <editor-fold desc="Notifications">
    // TODO: Notifications have no designs or copy - they were like this before but it's ticketed to fix them 3/2/16 [KV]
    // Once we have designs and copy, we can pull these into a reasonable place (like strings). But we need to know
    // what will be in common or different before doing so.
    private static final String STARTED_STRING = "Started";
    private static final String RESUMED_STRING = "Resumed";
    private static final String PAUSED_STRING = "Paused";
    private static final String OF_STRING = " of ";
    private static final String TAP_TO_VIEW = "Tap to view in app";

    private void showNotification() {
        // This is when the user will know their upload should be running
        startForeground(mProgressNotificationId, mProgressNotificationBuilder.build());
        mNotificationShowing = true;
    }

    /**
     * Show a notification while this service is running.
     */
    protected void setupNotification() {
        mProgressNotificationBuilder = new Notification.Builder(this).setSmallIcon(getProgressIconDrawable())
                .setTicker(STARTED_STRING)
                .setProgress(100, 0, true)
                // Example: "Uploading video"
                .setContentTitle(getProgressNotificationString())
                .setContentText(getProgressContentText());

        setIntent(mProgressNotificationBuilder);
    }

    private void showStartedTicker() {
        mProgressNotificationBuilder.setTicker(STARTED_STRING)
                .setContentTitle(getProgressNotificationString())
                .setContentText(getProgressContentText());
        notifyIfShowing();
    }

    /**
     * Just update the progress on the bar
     */
    private void updateProgress(int progress) {
        mProgressNotificationBuilder.setProgress(100, progress, false);
        notifyIfShowing();
    }

    private void updateProgressContentText() {
        mProgressNotificationBuilder.setContentTitle(getProgressNotificationString())
                .setContentText(getProgressContentText());
        notifyIfShowing();
    }

    private void returnToProgressState() {
        mProgressNotificationBuilder.setTicker(RESUMED_STRING)
                .setContentTitle(getProgressNotificationString())
                .setContentText(getProgressContentText());
        notifyIfShowing();
    }

    private void setNotificationLostNetwork() {
        mProgressNotificationBuilder.setTicker(PAUSED_STRING)
                .setContentText(mNetworkNotificationMessageString);
        notifyIfShowing();
    }

    /**
     * Shows an entirely separate notification
     */
    private void showNotificationFinish() {
        Notification.Builder builder = new Notification.Builder(this)
                // Example: "Upload finished"
                .setTicker(mFinishedNotificationTitleString)
                .setContentTitle(mFinishedNotificationTitleString)
                .setContentText(TAP_TO_VIEW)
                .setSmallIcon(getFinishedIconDrawable())
                .setAutoCancel(true);
        setIntent(builder);
        if (mNotificationShowing) {
            // Only actually call build if it's showing
            mNotificationManager.notify(mFinishedNotificationId, builder.build());
        }
    }

    // ---- Helpers ----
    private void setIntent(Notification.Builder builder) {
        Intent intent = mTaskManager.getNotificationIntent();
        if (intent != null) {
            // The PendingIntent to launch our activity if the user selects this notification
            PendingIntent contentIntent = PendingIntent.getActivity(this, 0, intent, 0);
            // The intent to send when the entry is clicked
            builder.setContentIntent(contentIntent);
        }
    }

    private String getProgressNotificationString() {
        return getResources().getQuantityString(getProgressNotificationTitleStringRes(), mTotalTaskCount);
    }

    private String getProgressContentText() {
        return mFinishedCount + OF_STRING + mTotalTaskCount;
    }

    private void notifyIfShowing() {
        if (mNotificationShowing) {
            // Only actually call build if it's showing
            mNotificationManager.notify(mProgressNotificationId, mProgressNotificationBuilder.build());
        }
    }

    protected void taskAdded() {
        showNotification();
        mTotalTaskCount++;
        showStartedTicker();
    }
    // </editor-fold>

    /*
     * -----------------------------------------------------------------------------------------------------
     * Receivers
     * -----------------------------------------------------------------------------------------------------
     */
    // <editor-fold desc="Receivers">
    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        @MainThread
        public void onReceive(Context context, Intent intent) {
            // Always on ui thread
            // This should only modify the notification, other activities that want to listen to these events
            // can do it themselves
            String event = intent.getStringExtra(TaskConstants.TASK_EVENT);
            String taskId;
            switch (event) {
                case TaskConstants.EVENT_NETWORK_LOST:
                    setNotificationLostNetwork();
                    break;
                case TaskConstants.EVENT_NETWORK_RETURNED:
                    returnToProgressState();
                    break;
                case TaskConstants.EVENT_ADDED:
                    taskId = intent.getStringExtra(TaskConstants.TASK_ID);
                    T task = mTaskManager.getTask(taskId);
                    if (task != null) {
                        if (task.shouldRun()) {
                            // If the task is supposed to be running, represent that in the notification
                            taskAdded();
                        }
                    }
                    // no-op
                    break;
                case TaskConstants.EVENT_PROGRESS:
                    taskId = intent.getStringExtra(TaskConstants.TASK_ID);
                    if (mTaskIdToListenOn == null) {
                        mTaskIdToListenOn = taskId;
                    }
                    if (mTaskIdToListenOn.equals(taskId)) {
                        int progress = intent.getIntExtra(TaskConstants.TASK_PROGRESS, 0);
                        updateProgress(progress);
                    }
                    break;
                case TaskConstants.EVENT_SUCCESS:
                    mFinishedCount++;
                    updateProgressContentText();
                    taskId = intent.getStringExtra(TaskConstants.TASK_ID);
                    if (mTaskIdToListenOn != null && mTaskIdToListenOn.equals(taskId)) {
                        // Null out this task id since we're no longer listening for it 3/2/16 [KV]
                        mTaskIdToListenOn = null;
                    }
                    // Remove any if one is already showing
                    mNotificationManager.cancel(mFinishedNotificationId);
                    showNotificationFinish();
                    break;
                case TaskConstants.EVENT_FAILURE:
                    // TODO: Make this a real notification 3/1/16 [KV]
                    //toast("Upload Failure");
                    break;
                case TaskConstants.EVENT_ALL_TASKS_FINISHED:
                case TaskConstants.EVENT_KILL_SERVICE:
                    killService();
                    break;
                default:
                    handleAdditionalEvents(event);
            }
        }
    };

    private void registerReceivers() {
        mTaskManager.registerReceiver(mReceiver);
    }

    private void unregisterReceivers() {
        LocalBroadcastManager.getInstance(this).unregisterReceiver(mReceiver);
    }
    // </editor-fold>
}
