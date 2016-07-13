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
package com.vimeo.turnstile.connectivity;


import com.vimeo.turnstile.BaseTaskManager;

/**
 * An interface that NetworkUtil can implement if it supports a callback
 * method when network state is changed. This is not mandatory but highly
 * suggested so that implementations of {@link BaseTaskManager} can avoid
 * busy loops when there is a job waiting for network and there is no
 * network available.
 * <p/>
 * This is taken from the android-priority-jobqueue framework
 *
 * @see <a href="https://github.com/yigit/android-priority-jobqueue">android-priority-jobqueue</a>
 */
public interface NetworkEventProvider {

    void setListener(Listener listener);

    interface Listener {

        /**
         * Called when the network connection status changes.
         * can be as simple as having an internet connection
         * or can also be customized (e.g. if your servers are down).
         *
         * @param isConnected true if there the network is
         *                    connected, false otherwise.
         */
        void onNetworkChange(boolean isConnected);
    }
}