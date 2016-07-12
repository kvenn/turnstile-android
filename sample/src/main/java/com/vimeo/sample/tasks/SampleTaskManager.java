package com.vimeo.sample.tasks;

import android.support.annotation.NonNull;

import com.vimeo.taskqueue.BaseTaskManager;
import com.vimeo.taskqueue.BaseTaskService;
import com.vimeo.taskqueue.TaskManagerBuilder;

public class SampleTaskManager extends BaseTaskManager<SampleTask> {

    private static SampleTaskManager sInstance;

    public synchronized static SampleTaskManager getInstance() {
        if (sInstance == null) {
            throw new IllegalStateException("Must be initialized first");
        }
        return sInstance;
    }

    public static void initialize(TaskManagerBuilder<SampleTask> taskManagerBuilder) {
        sInstance = new SampleTaskManager(taskManagerBuilder);
    }

    protected SampleTaskManager(@NonNull TaskManagerBuilder<SampleTask> taskManagerBuilder) {
        super(taskManagerBuilder);
    }

    @Override
    protected Class<? extends BaseTaskService> getServiceClass() {
        return SampleTaskService.class;
    }

    @NonNull
    @Override
    protected String getManagerName() {
        return "SampleTaskManager";
    }

    @NonNull
    @Override
    protected Class<SampleTask> getTaskClass() {
        return SampleTask.class;
    }

}
