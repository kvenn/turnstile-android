package com.vimeo.turnstile;

import com.vimeo.turnstile.TaskPreferences.OnSettingsChangedListener;
import com.vimeo.turnstile.utils.Assertion;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.robolectric.RuntimeEnvironment;

/**
 * Created by restainoa on 8/3/16.
 */
public class TaskPreferencesTest extends BaseUnitTest {

    private TaskPreferences mTaskPreferences;

    @Before
    public void setUp() throws Exception {
        mTaskPreferences = new TaskPreferences(RuntimeEnvironment.application, "test_manager");
    }

    @Test
    public void testIsPaused() throws Exception {
        mTaskPreferences.setIsPaused(true);
        Assert.assertTrue(mTaskPreferences.isPaused());

        mTaskPreferences.setIsPaused(false);
        Assert.assertFalse(mTaskPreferences.isPaused());
    }

    @Test
    public void testWifiOnly() throws Exception {
        mTaskPreferences.setWifiOnly(true);
        Assert.assertTrue(mTaskPreferences.wifiOnly());

        mTaskPreferences.setWifiOnly(false);
        Assert.assertFalse(mTaskPreferences.wifiOnly());
    }

    @Test
    public void testRegisterForSettingsChange() throws Exception {
        final Assertion<Integer> listenerAssertion = new Assertion<>(0);
        mTaskPreferences.setIsPaused(false);
        mTaskPreferences.setWifiOnly(false);
        mTaskPreferences.registerForSettingsChange(new OnSettingsChangedListener() {
            @Override
            public void onSettingChanged() {
                listenerAssertion.set(listenerAssertion.get() + 1);
            }
        });
        int changeCount = 0;
        mTaskPreferences.setIsPaused(true);
        changeCount++;
        mTaskPreferences.setIsPaused(false);
        changeCount++;

        mTaskPreferences.setWifiOnly(true);
        changeCount++;
        mTaskPreferences.setWifiOnly(false);
        changeCount++;

        mTaskPreferences.setWifiOnly(false);
        mTaskPreferences.setWifiOnly(false);
        mTaskPreferences.setWifiOnly(false);
        // No change should occur

        mTaskPreferences.setIsPaused(false);
        mTaskPreferences.setIsPaused(false);
        mTaskPreferences.setIsPaused(false);
        mTaskPreferences.setIsPaused(false);
        // No change should occur

        mTaskPreferences.setWifiOnly(true);
        changeCount++;

        mTaskPreferences.setIsPaused(true);
        changeCount++;

        Assert.assertEquals((Integer) changeCount, listenerAssertion.get());
    }
}