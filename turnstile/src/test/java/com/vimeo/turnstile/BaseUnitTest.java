package com.vimeo.turnstile;

import org.junit.Before;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricGradleTestRunner;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowLog;


@RunWith(RobolectricGradleTestRunner.class)
@Config(constants = BuildConfig.class)
public abstract class BaseUnitTest {

    @SuppressWarnings("AssignmentToStaticFieldFromInstanceMethod")
    @Before
    public void setupBaseUnitTest() {
        // Sets up debug logging
        ShadowLog.stream = System.out;
    }

}
