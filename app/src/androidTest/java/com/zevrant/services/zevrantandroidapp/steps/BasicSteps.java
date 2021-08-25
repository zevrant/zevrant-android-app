package com.zevrant.services.zevrantandroidapp.steps;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

import android.app.Activity;
import android.app.Instrumentation;
import android.content.Context;

import androidx.test.core.app.ActivityScenario;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.espresso.Espresso;
import androidx.test.espresso.assertion.ViewAssertions;
import androidx.test.espresso.matcher.ViewMatchers;
import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.uiautomator.UiDevice;
import androidx.test.uiautomator.UiObject;
import androidx.test.uiautomator.UiObjectNotFoundException;
import androidx.test.uiautomator.UiSelector;

import com.zevrant.services.zevrantandroidapp.R;
import com.zevrant.services.zevrantandroidapp.activities.ZevrantServices;
import com.zevrant.services.zevrantandroidapp.secrets.Secrets;
import com.zevrant.services.zevrantandroidapp.secrets.SecretsInitializer;

import org.apache.commons.lang3.StringUtils;

import io.cucumber.java.Before;
import io.cucumber.java.en.And;
import io.cucumber.java.en.Given;

public class BasicSteps {
    private static final String activityPackage = "com.zevrant.services.zevrantandroidapp.activities.";

    private ActivityScenario<ZevrantServices> scenario;
    private Activity zevrantActivity;

    public BasicSteps() {
    }

    public Activity getZevrantActivity() {
        return zevrantActivity;
    }

    @Before
    public void setup() {
        scenario = ActivityScenario.launch(ZevrantServices.class);
        scenario.onActivity((activity) -> {
            zevrantActivity = activity;
        });
    }

    @Given("^I start the application$")
    public void iStartTheApplication() {
        assertNotNull(zevrantActivity);
        SecretsInitializer.init();
    }

    @And("I verify the page transition to the login page")
    public void iVerifyThePageTransitionToHomeActivity() {
        onView(withId(R.id.LoginFormSubmitButton))
                .check(ViewAssertions.matches(ViewMatchers.isDisplayed())); //Check if the login submit button exists
    }

    @And("I verify the page transition to the home page")
    public void iVerifyThePageTransitionToLoginFormActivity() {
        onView(withId(R.id.loginButton))
                .check(ViewAssertions.matches(ViewMatchers.isDisplayed())); //Check if the login submit button exists
    }

    @And("^I click the home login button$")
    public void pressLoginButton() {
        onView(withId(R.id.loginButton))
                .perform(click());
    }

    @And("^I grant permission to access storage")
    public void grantStoragePermission() throws UiObjectNotFoundException {
        Instrumentation instrumentation = InstrumentationRegistry.getInstrumentation();
        UiObject allowPermission = UiDevice.getInstance(instrumentation).findObject(new UiSelector().text("Allow"));
        if (allowPermission.exists()) {
            allowPermission.click();
        } else {
            fail("unable to find permission request menu button");
        }
    }

    @And("^I grant permission to save credentials")
    public void grantCredentialsSave() throws UiObjectNotFoundException {
        Instrumentation instrumentation = InstrumentationRegistry.getInstrumentation();
        UiObject allowPermission = UiDevice.getInstance(instrumentation).findObject(new UiSelector().text("SAVE PASSWORD"));
        if (allowPermission.exists()) {
            allowPermission.click();
        } else {
            fail("unable to find permission request menu button");
        }
    }

    @And("^I close the keyboard$")
    public void closeKeyboard() {
        Espresso.closeSoftKeyboard();
    }

    public static String getPasswordByUsername(String username) {
        String password = Secrets.getPassword(username);
        if(StringUtils.isBlank(password)) {
            throw new RuntimeException("Password for requested username ".concat(username).concat(" does not exist!!!"));
        }
        return password;
    }

}
