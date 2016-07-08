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
package com.vimeo.taskqueue;

import android.support.annotation.Nullable;

import com.vimeo.taskqueue.models.TaskError;

/**
 * This is a generic logging interface which can be used for
 * logging specific as well as general events fired throughout
 * the task life cycle.
 * <p/>
 * Created by kylevenn on 12/8/15.
 */
public interface LoggingInterface<T extends BaseTask> {

    // ---- Specific Task Events ----
    void logTaskFailure(@Nullable T task, TaskError error);

    void logTaskSuccess(@Nullable T task);

    void logTaskCancel(@Nullable T task);

    void logTaskRetry(@Nullable T task);

    // ---- Generic Task Manager Events ----
    void e(String error);

    void e(String error, Exception exception);

    void d(String debug);

    void v(String verbose);
}
