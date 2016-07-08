package com.vimeo.taskqueue.dummy;

import com.vimeo.taskqueue.database.TaskCache;
import com.vimeo.taskqueue.database.TaskDatabase;

import org.robolectric.RuntimeEnvironment;

public final class DummyClassInstances {

    private DummyClassInstances() {
    }

    public static TaskDatabase<UnitTestBaseTask> newDatabase() {
        return new TaskDatabase<>(RuntimeEnvironment.application, "test", UnitTestBaseTask.class);
    }

    public static TaskCache<UnitTestBaseTask> newTaskCache() {
        return new TaskCache<>(newDatabase());
    }

}
