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

import android.content.Context;
import android.support.annotation.CallSuper;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.WorkerThread;

import com.google.gson.annotations.SerializedName;
import com.vimeo.taskqueue.connectivity.NetworkUtil;
import com.vimeo.taskqueue.models.TaskError;

import java.io.Serializable;
import java.util.concurrent.Callable;

/**
 * The abstract base class responsible for both managing state as well
 * as executing a specific task. This task should only ever modify its
 * own {@link TaskState}. It runs the task through the {@link Callable#call()}
 * method.
 * <p/>
 * This task is responsible for broadcasting any state changes through
 * the {@link TaskStateListener} so that the TaskManager can persist
 * the changes.
 * <p/>
 * Any extending classes must be sure to implement the {@link #shouldRun()}
 * method which is used by the manager to know when a persisted task is
 * safe to resume/retry in the event of a failure.
 * <p/>
 * Created by kylevenn on 2/9/16.
 */
public abstract class BaseTask implements Serializable, Callable {

    private static final long serialVersionUID = -2051421294839668480L;

    /**
     * An abstract listener class that notifies
     * the implementation for the following events:
     * <ol>
     * <li>When the task state changes.</li>
     * <li>When the task is completed.</li>
     * <li>When the progress reported by the task changes.</li>
     * <li>When the task fails to complete.</li>
     * </ol>
     *
     * @param <T> type that extends {@link BaseTask}
     */
    public static abstract class TaskStateListener<T extends BaseTask> {

        @NonNull
        private final Class<T> mClass;

        public TaskStateListener(@NonNull Class<T> clazz) {
            mClass = clazz;
        }

        abstract void onTaskStateChange(@NonNull T task);

        abstract void onTaskCompleted(@NonNull T task);

        abstract void onTaskProgress(@NonNull T task, int progress);

        abstract void onTaskFailure(@NonNull T task, TaskError taskError);

        public final void notifyTaskStateChange(@NonNull BaseTask task) {
            T safeTask = getFrom(task);
            if (safeTask != null) {
                onTaskStateChange(safeTask);
            }
        }

        public final void notifyTaskCompleted(@NonNull BaseTask task) {
            T safeTask = getFrom(task);
            if (safeTask != null) {
                onTaskCompleted(safeTask);
            }
        }

        public final void notifyOnTaskProgress(@NonNull BaseTask task, int progress) {
            T safeTask = getFrom(task);
            if (safeTask != null) {
                onTaskProgress(safeTask, progress);
            }
        }

        public final void notifyOnTaskFailure(@NonNull BaseTask task, TaskError taskError) {
            T safeTask = getFrom(task);
            if (safeTask != null) {
                onTaskFailure(safeTask, taskError);
            }
        }

        @Nullable
        private T getFrom(@Nullable BaseTask task) {
            if (mClass.isInstance(task)) {
                return mClass.cast(task);
            }
            return null;
        }
    }
    // -----------------------------------------------------------------------------------------------------
    // Enums
    // -----------------------------------------------------------------------------------------------------
    // <editor-fold desc="Enums">

    /**
     * An enum representing the generic state which all sub-classes of {@link BaseTask} will share.
     * This is only for state that needs to be persisted - other transient states should lived elsewhere (transient field).
     */
    public enum TaskState {
        /**
         * Default state for a task. Indicates that it is ready to be executed (unless otherwise specified in {@link #shouldRun()}
         */
        READY,

        /**
         * The task has finished execution and completed its job <b>successfully</b>
         */
        COMPLETE,

        /**
         * The task encountered an error and will need to be manually restarted. The {@link #mError} will be populated when in this state
         */
        ERROR
    }
    // </editor-fold>

    // -----------------------------------------------------------------------------------------------------
    // Fields
    // -----------------------------------------------------------------------------------------------------
    // <editor-fold desc="Fields">

    /**
     * The default number of times that the task will retry in the event of an error
     */
    protected static final int DEFAULT_NUMBER_RETRIES = 3;

    // ---- Transient Fields ----
    /**
     * A context will have to be passed in when the task is first started
     */
    protected transient Context mContext;

    /**
     * A listener for state changes is required when the task is first started
     */
    @Nullable
    protected transient TaskStateListener mStateListener;

    /**
     * An optional network util for network related tasks
     */
    @Nullable
    protected transient NetworkUtil mNetworkUtil;

    /**
     * This marks the number of retries this task has attempted - this is to possibly rebind if there
     * is a server error. We don't persist this since it's the number of retries per resume attempt
     */
    protected transient int mRetryCount;

    /**
     * If this task has run at some point previously. We might want to execute a different task if this is
     * the second attempt at running this task (meaning it failed previously, but might have made progress).
     * This should be set prior to calling the {@link #call()} method.
     */
    private transient boolean mIsRetry;

    /**
     * Progress out of 100. This isn't persisted and is only here for convenience
     */
    private transient int mProgress;

    // ---- Task Specific Fields ----
    /**
     * Unique identifier for this task
     */
    @SerializedName("id")
    protected String mId;

    /**
     * The state this task is currently in. We default it to {@link TaskState#READY}
     */
    @SerializedName("state")
    protected TaskState mState = TaskState.READY;

    /**
     * The error that will be set when this task is in the {@link TaskState#ERROR} state.
     * It isn't set to null when it is executing again - so you can't rely on this having a null value.
     */
    @SerializedName("error")
    protected TaskError mError;

    /**
     * The Unix Timestamp for when this task was first added
     */
    @SerializedName("created_at")
    protected long mCreatedTimeMillis;

    private volatile boolean mIsRunning;
    // </editor-fold>

    // -----------------------------------------------------------------------------------------------------
    // Constructors
    // -----------------------------------------------------------------------------------------------------
    // <editor-fold desc="Constructors">

    /**
     * Most basic constructor with all requirements.
     * If this constructor is used, the Tasks {@link TaskState} will default to {@link TaskState#READY}
     */
    public BaseTask(String id) {
        mId = id;
        mCreatedTimeMillis = System.currentTimeMillis();
    }

    /**
     * Optionally initialize the task with a {@link TaskState} and created time (for initialization from database)
     */
    public BaseTask(String id, TaskState taskState, long createdTimeMillis) {
        mId = id;
        mState = taskState;
        mCreatedTimeMillis = createdTimeMillis;
    }
    // </editor-fold>

    // -----------------------------------------------------------------------------------------------------
    // Task Execution
    // -----------------------------------------------------------------------------------------------------
    // <editor-fold desc="Task Execution">

    /**
     * An overridable method which dictates if a task should be run by the management system. This is an opportunity
     * for any extending tasks to specify that they have additional requirements for if their task should be
     * executed by the {@link BaseTaskManager}.
     *
     * @return If this task is ready to run.
     */
    @CallSuper
    public boolean shouldRun() {
        return mState == TaskState.READY;
    }

    /**
     * The abstract method responsible for running the task.
     */
    @WorkerThread
    protected abstract void execute();

    /**
     * An overridable method responsible for a retry. Defaults to calling execute
     */
    @WorkerThread
    protected void retry() {
        execute();
    }

    @Override
    public Object call() throws Exception {
        mIsRunning = true;
        if (mIsRetry) {
            TaskLogger.d("Task Resumed " + mId);
            retry();
        } else {
            TaskLogger.d("Task Started For First Time " + mId);
            execute();
        }
        mIsRunning = false;
        return null;
    }

    public boolean isRunning() {
        return mIsRunning;
    }
    // </editor-fold>

    // -----------------------------------------------------------------------------------------------------
    // Transient Setters
    // -----------------------------------------------------------------------------------------------------
    // <editor-fold desc="Transient Setters">
    public void setContext(Context context) {
        mContext = context;
    }

    public void setStateListener(@Nullable TaskStateListener stateListener) {
        mStateListener = stateListener;
    }

    public void setNetworkUtil(@Nullable NetworkUtil networkUtil) {
        mNetworkUtil = networkUtil;
    }

    /**
     * Set whether this task should execute as a resume/retry or not. This must be set prior to submitting this
     * {@link Callable}.
     */
    public void setIsRetry(boolean isRetry) {
        mIsRetry = isRetry;
    }
    // </editor-fold>

    // -----------------------------------------------------------------------------------------------------
    // State Modification
    // These should always be protected. This task will only ever modify itself on one thread.
    // -----------------------------------------------------------------------------------------------------
    // <editor-fold desc="State Modification">

    /**
     * Marks task as ready and removes any errors. This happens when we are attempting a retry.
     */
    protected void updateStateForRetry() {
        if (mState == TaskState.ERROR) {
            // If there's an error, remove it to prep the task for retry
            mState = TaskState.READY;
            onTaskChange();
        }
    }

    protected void onTaskChange() {
        if (mStateListener != null) {
            mStateListener.notifyTaskStateChange(this);
        }
    }

    protected void onTaskCompleted() {
        mState = TaskState.COMPLETE;
        if (mStateListener != null) {
            mStateListener.notifyTaskCompleted(this);
        }
    }

    protected void onTaskProgress(int progress) {
        mProgress = progress;
        if (mStateListener != null) {
            mStateListener.notifyOnTaskProgress(this, progress);
        }
    }

    protected void onTaskFailure(TaskError error) {
        mState = TaskState.ERROR;
        mError = error;
        if (mStateListener != null) {
            mStateListener.notifyOnTaskFailure(this, error);
        }
    }
    // </editor-fold>

    // -----------------------------------------------------------------------------------------------------
    // Getters
    // -----------------------------------------------------------------------------------------------------
    // <editor-fold desc="Getters">
    public String getId() {
        return mId;
    }

    public boolean isComplete() {
        return mState == TaskState.COMPLETE;
    }

    public boolean isError() {
        return mState == TaskState.ERROR;
    }

    public boolean isReady() {
        return mState == TaskState.READY;
    }

    public TaskError getTaskError() {
        return mError;
    }

    public TaskState getTaskState() {
        return mState;
    }

    public long getCreatedTimeMillis() {
        return mCreatedTimeMillis;
    }

    /**
     * Get the last progress that was reported for this task. This isn't guaranteed to be correct and
     * exists only for convenience.
     *
     * @return progress out of 100
     */
    public int getProgress() {
        return mProgress;
    }
    // </editor-fold>
}
