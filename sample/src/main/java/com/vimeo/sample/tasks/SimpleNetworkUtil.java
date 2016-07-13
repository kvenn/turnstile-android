package com.vimeo.sample.tasks;

import android.content.Context;

import com.vimeo.taskqueue.connectivity.NetworkUtil;

public class SimpleNetworkUtil extends NetworkUtil {

    public SimpleNetworkUtil(Context context) {
        super(context);
    }

    @Override
    public boolean isConnected() {
        return true;
    }
}
