package com.vimeo.sample.tasks;

import android.util.Log;

import com.vimeo.taskqueue.BaseTask;

import java.util.Random;
import java.util.concurrent.TimeUnit;

public class SampleTask extends BaseTask {


    private final String TAG;

    private static final long serialVersionUID = -2959689752810794783L;

    private static final Random sRandom = new Random(System.currentTimeMillis());

    public SampleTask(String id) {
        super(id);
        TAG = "SampleTask - " + id;
    }

    public SampleTask(String id, TaskState taskState, long createdTimeMillis) {
        super(id, taskState, createdTimeMillis);
        TAG = "SampleTask - " + id;
    }


    @Override
    protected void execute() {
        mState = TaskState.READY;
        Log.d(TAG, "Starting task");
        long time = System.nanoTime();
        long timeCheck = System.nanoTime();
        Log.d(TAG, "" + TimeUnit.NANOSECONDS.toSeconds(timeCheck - time));
        while (TimeUnit.NANOSECONDS.toSeconds(timeCheck - time) < 3) {
            timeCheck = System.nanoTime();
        }

        Log.d(TAG, "Finishing task");
        mState = TaskState.COMPLETE;
    }

}
