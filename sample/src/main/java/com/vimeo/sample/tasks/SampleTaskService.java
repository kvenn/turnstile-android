package com.vimeo.sample.tasks;

import com.vimeo.taskqueue.BaseTaskManager;
import com.vimeo.taskqueue.BaseTaskService;

public class SampleTaskService extends BaseTaskService<SampleTask> {

    @Override
    protected void handleAdditionalEvents(String event) {

    }

    @Override
    protected BaseTaskManager<SampleTask> getManagerInstance() {
        return SampleTaskManager.getInstance();
    }

    @Override
    protected int getProgressNotificationId() {
        return 0;
    }

    @Override
    protected int getFinishedNotificationId() {
        return 0;
    }

    @Override
    protected int getProgressNotificationTitleStringRes() {
        return 0;
    }

    @Override
    protected int getFinishedNotificationTitleStringRes() {
        return 0;
    }

    @Override
    protected int getNetworkNotificationMessageStringRes() {
        return 0;
    }

    @Override
    protected int getProgressIconDrawable() {
        return 0;
    }

    @Override
    protected int getFinishedIconDrawable() {
        return 0;
    }
}
