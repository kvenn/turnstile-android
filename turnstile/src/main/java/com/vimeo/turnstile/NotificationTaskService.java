/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2016 Vimeo
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
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
import android.content.Intent;
import android.support.annotation.DrawableRes;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.PluralsRes;
import android.support.annotation.StringRes;

/**
 * An abstract {@link BaseTaskService} that can handle notifications with minimal setup. If you wish to
 * show simple notifications, you can extend this class rather than BaseTaskService.
 * <p/>
 * Created by zetterstromk on 8/10/16.
 */
public abstract class NotificationTaskService<T extends BaseTask> extends BaseTaskService<T> {

    /**
     * Unique id for the notification. We use it on notification start and to cancel it.
     */
    private int mProgressNotificationId;
    private int mFinishedNotificationId;

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

    // -----------------------------------------------------------------------------------------------------
    // Lifecycle
    // -----------------------------------------------------------------------------------------------------
    // <editor-fold desc="Lifecycle">
    @Override
    public final void onCreate() {
        super.onCreate();

        mFinishedCount = 0;
        mTotalTaskCount = mTaskManager.getTasksToRun().size();

        // ---- Notification Setup ----
        mProgressNotificationId = getProgressNotificationId();
        mFinishedNotificationId = getFinishedNotificationId();
        mFinishedNotificationTitleString = getString(getFinishedNotificationTitleStringRes());
        mNetworkNotificationMessageString = getString(getNetworkNotificationMessageStringRes());

        mNotificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        setupNotification();
    }
    // </editor-fold>

    // -----------------------------------------------------------------------------------------------------
    // Abstract methods
    // -----------------------------------------------------------------------------------------------------
    // <editor-fold desc="Abstract methods">

    /**
     * The title of the notification when the device conditions (e.g.
     * network) are not suitable to complete the task.
     *
     * @return the string resource for the device condition notification.
     */
    @StringRes
    protected abstract int getNetworkNotificationMessageStringRes();

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

    // -----------------------------------------------------------------------------------------------------
    // BaseTaskService Overrides
    // -----------------------------------------------------------------------------------------------------
    // <editor-fold desc="BaseTaskService Overrides">

    @Override
    protected void onStarted() {
        // If there are tasks remaining, that means this service should be running and showing the
        // notification
        showNotification();
        super.onStarted();
    }

    @Override
    protected void onKillService() {
        mNotificationShowing = false;
        super.onKillService();
    }

    @Override
    protected void onTaskConditionsLost() {
        setNotificationLostNetwork();
        super.onTaskConditionsLost();
    }

    @Override
    protected void onTaskConditionsReturned() {
        returnToProgressState();
        super.onTaskConditionsReturned();
    }

    @Override
    protected void onTaskProgress(@NonNull T task, int progress) {
        if (mTaskIdToListenOn == null) {
            mTaskIdToListenOn = task.getId();
        }
        if (mTaskIdToListenOn.equals(task.getId())) {
            updateProgress(progress);
        }
        super.onTaskProgress(task, progress);
    }

    @Override
    protected void onTaskSuccess(@NonNull T task) {
        mFinishedCount++;
        updateProgressContentText();
        if (mTaskIdToListenOn != null && mTaskIdToListenOn.equals(task.getId())) {
            // Null out this task id since we're no longer listening for it 3/2/16 [KV]
            mTaskIdToListenOn = null;
        }
        // Remove any if one is already showing
        mNotificationManager.cancel(mFinishedNotificationId);
        showNotificationFinish();
        super.onTaskSuccess(task);
    }

    @Override
    protected void taskAdded() {
        showNotification();
        mTotalTaskCount++;
        showStartedTicker();
        super.taskAdded();
    }
    // </editor-fold>

    // -----------------------------------------------------------------------------------------------------
    // Notifications
    // -----------------------------------------------------------------------------------------------------
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
    // </editor-fold>
}
