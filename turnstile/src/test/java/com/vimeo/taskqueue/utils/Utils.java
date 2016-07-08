package com.vimeo.taskqueue.utils;

import org.robolectric.shadows.ShadowLog;

public final class Utils {

    private static final String LOG_TAG = "TaskQueueTest";

    private Utils() {
    }

    public static void log(CharSequence message) {
        log(LOG_TAG, message);
    }

    public static void log(CharSequence tag, CharSequence message) {
        ShadowLog.stream.println(tag + ": " + message);
    }

}
