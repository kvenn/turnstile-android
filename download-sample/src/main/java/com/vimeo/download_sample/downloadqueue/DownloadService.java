package com.vimeo.download_sample.downloadqueue;

import android.R.drawable;

import com.vimeo.download_sample.R;
import com.vimeo.turnstile.BaseTaskManager;
import com.vimeo.turnstile.NotificationTaskService;

/**
 * A subclass of {@link NotificationTaskService} which handles download specific events.
 * <p/>
 * Created by kylevenn on 8/19/15.
 */
public class DownloadService extends NotificationTaskService<DownloadTask> {

    private static final int PROGRESS_NOTIFICATION_ID = 6001;
    private static final int FINISHED_NOTIFICATION_ID = 6002;

    @Override
    protected void handleAdditionalEvents(String event) {
    }

    @Override
    protected BaseTaskManager<DownloadTask> getManagerInstance() {
        return DownloadManager.getInstance();
    }

    @Override
    protected int getProgressNotificationId() {
        return PROGRESS_NOTIFICATION_ID;
    }

    @Override
    protected int getFinishedNotificationId() {
        return FINISHED_NOTIFICATION_ID;
    }

    @Override
    protected int getProgressNotificationTitleStringRes() {
        return R.plurals.notification_downloading;
    }

    @Override
    protected int getFinishedNotificationTitleStringRes() {
        return R.string.notification_download_finished;
    }

    @Override
    protected int getNetworkNotificationMessageStringRes() {
        return mTaskManager.wifiOnly() ? R.string.notification_waiting_for_wifi : R.string.notification_waiting_for_network;
    }

    @Override
    protected int getProgressIconDrawable() {
        return drawable.stat_sys_download;
    }

    @Override
    protected int getFinishedIconDrawable() {
        return drawable.stat_sys_download_done;
    }
}
