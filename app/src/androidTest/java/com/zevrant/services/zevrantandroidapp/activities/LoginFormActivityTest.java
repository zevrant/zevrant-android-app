package com.zevrant.services.zevrantandroidapp.activities;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.intent.Intents.intended;
import static androidx.test.espresso.intent.matcher.IntentMatchers.hasComponent;
import static androidx.test.espresso.matcher.ViewMatchers.withId;

import androidx.test.espresso.intent.Intents;
import androidx.test.ext.junit.rules.ActivityScenarioRule;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.zevrant.services.zevrantandroidapp.R;

import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
@Ignore //not using regular instrumentation tests
public class LoginFormActivityTest {


    @Rule
    public ActivityScenarioRule<ZevrantServices> activityRule
            = new ActivityScenarioRule<>(ZevrantServices.class);

    @Before
    public void setup() {
        Intents.init();
    }

    @After
    public void tearDown() {
        Intents.release();
    }

    @Test
    public void loginTest() {
        onView(withId(R.id.loginButton))
                .perform(click());
        intended(hasComponent(LoginFormActivity.class.getName()));
    }

    @Test
    public void loginInvalidCredentialsTest() {
        onView(withId(R.id.loginButton))
                .perform(click());
        intended(hasComponent(LoginFormActivity.class.getName()));
    }
}
