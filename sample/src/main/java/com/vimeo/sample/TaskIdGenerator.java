package com.vimeo.sample;

import java.util.concurrent.atomic.AtomicLong;

public class TaskIdGenerator {

    private final AtomicLong mAtomicLong = new AtomicLong(0);

    public TaskIdGenerator() {
    }

    public String getId() {
        return String.valueOf(mAtomicLong.incrementAndGet());
    }

}
