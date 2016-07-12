package com.vimeo.sample.tasks;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;

import com.vimeo.sample.BuildConfig;
import com.vimeo.taskqueue.LoggingInterface;
import com.vimeo.taskqueue.models.TaskError;

public class SampleLoggingInterface implements LoggingInterface<SampleTask> {

    private static final String TAG = "SampleTaskLogger";

    @NonNull
    private static String id(@Nullable SampleTask task) {
        return (task == null ? "null" : task.getId());
    }

    @Override
    public void logTaskFailure(@Nullable SampleTask task, TaskError error) {
        Log.e(TAG, "Error on task: " + id(task) + ":" + error.getMessage());
    }

    @Override
    public void logTaskSuccess(@Nullable SampleTask task) {
        if (BuildConfig.DEBUG) {
            Log.i(TAG, "Success running task with id: " + id(task));
        }
    }

    @Override
    public void logTaskCancel(@Nullable SampleTask task) {
        if (BuildConfig.DEBUG) {
            Log.i(TAG, "Cancelled task with id: " + id(task));
        }
    }

    @Override
    public void logTaskRetry(@Nullable SampleTask task) {
        if (BuildConfig.DEBUG) {
            Log.i(TAG, "Retrying task with id: " + id(task));
        }
    }

    @Override
    public void e(String error) {
        Log.e(TAG, error);
    }

    @Override
    public void e(String error, Exception exception) {
        Log.e(TAG, error + ":" + exception.getMessage());
    }

    @Override
    public void d(String debug) {
        if (BuildConfig.DEBUG) {
            Log.d(TAG, debug);
        }
    }

    @Override
    public void v(String verbose) {
        if (BuildConfig.DEBUG) {
            Log.v(TAG, verbose);
        }
    }
}
