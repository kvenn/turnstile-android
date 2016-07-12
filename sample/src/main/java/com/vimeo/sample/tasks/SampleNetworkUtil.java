package com.vimeo.sample.tasks;

import android.content.Context;

import com.vimeo.taskqueue.connectivity.NetworkUtil;

public class SampleNetworkUtil extends NetworkUtil {

    public SampleNetworkUtil(Context context) {
        super(context);
    }

    @Override
    public boolean isConnected() {
        return true;
    }
}
