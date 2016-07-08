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
package com.vimeo.taskqueue.connectivity;

import android.Manifest;
import android.Manifest.permission;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.support.annotation.RequiresPermission;

import com.vimeo.taskqueue.TaskLogger;

/**
 * Interface which you can implement if you want to provide a custom
 * Network callback. Make sure you also implement {@link NetworkEventProvider}
 * for best performance.
 * <p/>
 * Created by kylevenn on 9/8/2015
 */
public abstract class NetworkUtil implements NetworkEventProvider {

    final Context mContext;
    private Listener mListener;
    final BroadcastReceiver mNetworkChangeReceiver;

    @RequiresPermission(permission.ACCESS_NETWORK_STATE)
    NetworkUtil(Context context) {
        mContext = context;

        // Receiver that just tells this class to tell the listener that something changed
        mNetworkChangeReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context ctx, Intent intent) {
                if (mListener == null) {
                    TaskLogger.w("Null listener in network util extended");
                    return;
                }
                mListener.onNetworkChange(isConnected());
            }
        };

        // Set the receiver to listen for network connectivity change
        context.getApplicationContext()
                .registerReceiver(mNetworkChangeReceiver,
                                  new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION));
    }

    @RequiresPermission(Manifest.permission.ACCESS_NETWORK_STATE)
    public abstract boolean isConnected();


    @Override
    public void setListener(Listener listener) {
        mListener = listener;
    }
}
