package com.vimeo.turnstile.database;

import com.vimeo.turnstile.dummy.UnitTestBaseTask;

import org.robolectric.RuntimeEnvironment;

/**
 * Created by restainoa on 8/3/16.
 */
class DummyDatabaseInstances {

    public static TaskDatabase<UnitTestBaseTask> newDatabase() {
        return new TaskDatabase<>(RuntimeEnvironment.application, "test", UnitTestBaseTask.class);
    }

}
