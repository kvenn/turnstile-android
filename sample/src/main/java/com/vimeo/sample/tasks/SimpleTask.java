package com.vimeo.sample.tasks;

import android.util.Log;

import com.vimeo.taskqueue.BaseTask;

import java.util.concurrent.TimeUnit;

public class SimpleTask extends BaseTask {

    private static final long serialVersionUID = -2959689752810794783L;

    private final String TAG;

    public SimpleTask(String id) {
        super(id);
        TAG = "SimpleTask - " + id;
    }

    public SimpleTask(String id, TaskState taskState, long createdTimeMillis) {
        super(id, taskState, createdTimeMillis);
        TAG = "SimpleTask - " + id;
    }

    @Override
    protected void execute() {
        Log.d(TAG, "Starting task");
        onTaskProgress(0);
        long time = System.nanoTime();
        long timeCheck = System.nanoTime();
        while (TimeUnit.NANOSECONDS.toSeconds(timeCheck - time) < 3) {
            timeCheck = System.nanoTime();
        }

        Log.d(TAG, "Finishing task");
        onTaskProgress(100);
        onTaskCompleted();
    }

}
