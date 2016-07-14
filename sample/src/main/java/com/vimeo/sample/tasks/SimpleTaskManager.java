package com.vimeo.sample.tasks;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.vimeo.turnstile.BaseTaskManager;
import com.vimeo.turnstile.BaseTaskService;
import com.vimeo.turnstile.TaskManagerBuilder;

public class SimpleTaskManager extends BaseTaskManager<SimpleTask> {

    @Nullable
    private static SimpleTaskManager sInstance;

    @NonNull
    public synchronized static SimpleTaskManager getInstance() {
        if (sInstance == null) {
            throw new IllegalStateException("Must be initialized first");
        }
        return sInstance;
    }

    public static void initialize(@NonNull TaskManagerBuilder<SimpleTask> taskManagerBuilder) {
        sInstance = new SimpleTaskManager(taskManagerBuilder);
    }

    protected SimpleTaskManager(@NonNull TaskManagerBuilder<SimpleTask> taskManagerBuilder) {
        super(taskManagerBuilder);
    }

    @Override
    protected Class<? extends BaseTaskService> getServiceClass() {
        return SimpleTaskService.class;
    }

    @NonNull
    @Override
    protected String getManagerName() {
        return "SimpleTaskManager";
    }

    @NonNull
    @Override
    protected Class<SimpleTask> getTaskClass() {
        return SimpleTask.class;
    }

}
