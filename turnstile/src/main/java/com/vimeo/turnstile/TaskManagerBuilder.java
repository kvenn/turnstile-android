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

import android.content.Context;
import android.content.Intent;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.vimeo.turnstile.conditions.Conditions;
import com.vimeo.turnstile.conditions.network.NetworkConditionsExtended;

/**
 * A class used to pass variables to the
 * constructor of the {@link BaseTaskManager}.
 * These variables are used by the manager
 * internally.
 *
 * @param <T> a type that extends {@link BaseTask}.
 */
@SuppressWarnings("unused")
public final class TaskManagerBuilder<T extends BaseTask> {

    @NonNull
    final Context mContext;
    @NonNull
    Conditions mConditions;
    @Nullable
    Intent mNotificationIntent;

    boolean mStartOnDeviceBoot;

    public TaskManagerBuilder(@NonNull Context context) {
        mContext = context;
        // Set the default to be the extended network util
        mConditions = new NetworkConditionsExtended(mContext);
        mStartOnDeviceBoot = false;
    }

    @NonNull
    public TaskManagerBuilder<T> withConditions(@NonNull Conditions conditions) {
        mConditions = conditions;
        return this;
    }

    @NonNull
    public TaskManagerBuilder<T> withNotificationIntent(@Nullable Intent notificationIntent) {
        mNotificationIntent = notificationIntent;
        return this;
    }

    /**
     * Use this if you'd like the task manager to
     * resume its tasks when the devices first starts.
     *
     * @param startOnDeviceBoot true if you want the task
     *                          manager to start on device
     *                          boot, false otherwise. default
     *                          is false.
     */
    @NonNull
    public TaskManagerBuilder<T> withStartOnDeviceBoot(boolean startOnDeviceBoot) {
        mStartOnDeviceBoot = startOnDeviceBoot;
        return this;
    }
}