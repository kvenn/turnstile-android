package com.vimeo.turnstile.dummy;

import com.vimeo.turnstile.BaseTask;

import java.util.concurrent.atomic.AtomicInteger;

public final class UnitTestBaseTask extends BaseTask {

    private static final AtomicInteger idCounter = new AtomicInteger();
    private static final long serialVersionUID = -4922424711410164139L;

    public static UnitTestBaseTask newTask() {
        return new UnitTestBaseTask(String.valueOf(idCounter.incrementAndGet()));
    }

    private UnitTestBaseTask(String id) {
        super(id);
    }

    public void changeState() {
        mState = TaskState.ERROR;
    }

    @Override
    protected void execute() {

    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof UnitTestBaseTask)) {
            return false;
        }
        if (mId == null) {
            return ((UnitTestBaseTask) o).mId == null;
        }

        return ((UnitTestBaseTask) o).mId.equals(mId);
    }

    @Override
    public int hashCode() {
        if (mId == null) {
            return 0;
        }
        return mId.hashCode();
    }
}
