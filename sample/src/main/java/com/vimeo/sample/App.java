package com.vimeo.sample;

import android.app.Application;
import android.content.Intent;

import com.vimeo.sample.tasks.SampleLoggingInterface;
import com.vimeo.sample.tasks.SampleNetworkUtil;
import com.vimeo.sample.tasks.SampleTask;
import com.vimeo.sample.tasks.SampleTaskManager;
import com.vimeo.taskqueue.TaskManagerBuilder;

public class App extends Application {

    public static final String NOTIFICATION_INTENT_KEY = "NOTIFICATION";

    @Override
    public void onCreate() {
        super.onCreate();

        TaskManagerBuilder<SampleTask> taskTaskManagerBuilder = new TaskManagerBuilder<>(this);
        taskTaskManagerBuilder.setLoggingInterface(new SampleLoggingInterface());
        taskTaskManagerBuilder.setNetworkUtil(new SampleNetworkUtil(this));

        Intent intent = new Intent(this, MainActivity.class);
        intent.setAction(NOTIFICATION_INTENT_KEY);

        taskTaskManagerBuilder.setNotificationIntent(intent);

        SampleTaskManager.initialize(taskTaskManagerBuilder);
    }
}
