/*
 * The MIT License (MIT)
 * <p/>
 * Copyright (c) 2016 Vimeo
 * <p/>
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * <p/>
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 * <p/>
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package com.vimeo.turnstile;

import android.support.annotation.Nullable;
import android.util.Log;

/**
 * A wrapper class used for task logging. Logging
 * can be turned on and off (e.g. on in debug,
 * off in release mode) by using the static method
 * {@link #setShouldLog(boolean)}.
 * <p/>
 * Created by kylevenn on 6/4/15.
 */
@SuppressWarnings("unused")
public final class TaskLogger {

    private static final String LOG_TAG = "TaskLogger";
    private static boolean sShouldLog;

    public static void setShouldLog(boolean shouldLog) {
        sShouldLog = shouldLog;
    }

    private TaskLogger() {
    }

    /**
     * A specifically for debug purposes - these should never be sent to analytics
     *
     * @param msg The message for the console
     */
    public static void d(String msg) {
        if (shouldLog()) {
            Log.d(LOG_TAG, msg);
        }
    }

    /**
     * A specifically for debug purposes - these should never be sent to analytics
     *
     * @param eventContext The context for the event (for grouping)
     * @param msg          The message for the console
     */
    public static void d(String eventContext, String msg) {
        if (shouldLog()) {
            Log.d(LOG_TAG, eventContext + " - " + msg);
        }
    }

    /**
     * For logging event information - this can be sent for analytics
     *
     * @param msg The message for the console
     */
    public static void i(String msg) {
        if (shouldLog()) {
            Log.i(LOG_TAG, msg);
        }
    }

    /**
     * A log for caught warnings - things that shouldn't happen but aren't TERRIBLE if they do happen
     * (console/analytics)
     *
     * @param msg The message to log to the
     */
    public static void w(String msg) {
        if (shouldLog()) {
            Log.w(LOG_TAG, msg);
        }
    }

    /**
     * A log for errors that shouldn't be happening at all - just the message
     * (console/analytics)
     *
     * @param msg The message associated with the mError
     */
    public static void e(String msg) {
        if (shouldLog()) {
            Log.e(LOG_TAG, msg);
        }
    }

    /**
     * A log for errors that shouldn't be happening at all - context and message.
     * </p>
     * Useful for grouping events (for analytics)
     *
     * @param eventContext The context/label for where the mError is happening
     * @param msg          The message associated with the mError
     */
    public static void e(String eventContext, String msg) {
        if (shouldLog()) {
            Log.e(LOG_TAG, eventContext + " - " + msg);
        }
    }

    /**
     * A log for errors that shouldn't be happening at all - context and message.
     * (console)
     *
     * @param msg       The message associated with the mError
     * @param exception The exception from the mError (for catches)
     */
    private static void e(@Nullable String msg, RuntimeException exception) {
        if (shouldLog() && exception != null) {
            String exceptionMessage = "";
            if (exception.getMessage() != null && !exception.getMessage().isEmpty()) {
                exceptionMessage = exception.getMessage();
            }

            if (msg != null && !msg.isEmpty()) {
                exceptionMessage = msg + " - " + exceptionMessage;
            }
            if (!exceptionMessage.isEmpty()) {
                Log.e(LOG_TAG, exceptionMessage);
            }
        }
    }

    /**
     * A log for errors that shouldn't be happening at all - context and message.
     * (console)
     *
     * @param exception The exception from the mError (for catches)
     */
    public static void e(RuntimeException exception) {
        TaskLogger.e(null, exception);
    }

    private static boolean shouldLog() {
        return sShouldLog;
    }
}
