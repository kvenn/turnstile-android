package com.vimeo.turnstile;

import android.os.Looper;

import com.vimeo.turnstile.utils.Assertion;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

/**
 * Created by restainoa on 8/3/16.
 */
public class BroadcastHandlerTest extends BaseUnitTest {

    @Before
    public void setup() {
        if (Looper.getMainLooper() == null) {
            Looper.prepareMainLooper();
        }
    }

    @Test
    public void testPost() throws Exception {
        final Assertion<Boolean> isThreadCorrect = new Assertion<>(false);
        BroadcastHandler.post(new Runnable() {
            @Override
            public void run() {
                isThreadCorrect.set(Looper.getMainLooper().equals(Looper.myLooper()));
            }
        });

        Assert.assertTrue(isThreadCorrect.get());
    }
}