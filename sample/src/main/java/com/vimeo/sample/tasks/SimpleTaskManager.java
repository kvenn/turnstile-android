package com.vimeo.sample.tasks;

import android.support.annotation.NonNull;

import com.vimeo.taskqueue.BaseTaskManager;
import com.vimeo.taskqueue.BaseTaskService;
import com.vimeo.taskqueue.TaskManagerBuilder;

public class SimpleTaskManager extends BaseTaskManager<SimpleTask> {

    private static SimpleTaskManager sInstance;

    public synchronized static SimpleTaskManager getInstance() {
        if (sInstance == null) {
            throw new IllegalStateException("Must be initialized first");
        }
        return sInstance;
    }

    public static void initialize(TaskManagerBuilder<SimpleTask> taskManagerBuilder) {
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
