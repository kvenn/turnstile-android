package com.vimeo.turnstile.dummy;

import com.vimeo.turnstile.database.TaskCache;
import com.vimeo.turnstile.database.TaskDatabase;

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
