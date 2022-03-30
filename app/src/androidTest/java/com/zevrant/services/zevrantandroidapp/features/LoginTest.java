package com.zevrant.services.zevrantandroidapp.features;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.withEffectiveVisibility;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.platform.app.InstrumentationRegistry.getInstrumentation;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNot.not;
import static org.hamcrest.core.IsNull.nullValue;

import androidx.test.espresso.action.ViewActions;
import androidx.test.espresso.matcher.ViewMatchers;
import androidx.test.ext.junit.rules.ActivityScenarioRule;
import androidx.test.uiautomator.By;
import androidx.test.uiautomator.UiDevice;
import androidx.test.uiautomator.UiObject2;

import com.zevrant.services.zevrantandroidapp.NukeSSLCerts;
import com.zevrant.services.zevrantandroidapp.R;
import com.zevrant.services.zevrantandroidapp.activities.ZevrantServices;
import com.zevrant.services.zevrantandroidapp.secrets.Secrets;
import com.zevrant.services.zevrantandroidapp.secrets.SecretsInitializer;
import com.zevrant.services.zevrantandroidapp.services.EncryptionService;
import com.zevrant.services.zevrantandroidapp.test.BaseTest;
import com.zevrant.services.zevrantandroidapp.utilities.Constants;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import javax.inject.Inject;

import dagger.hilt.android.testing.HiltAndroidRule;
import dagger.hilt.android.testing.HiltAndroidTest;

@HiltAndroidTest
public class LoginTest extends BaseTest {

    private static final String testUsername = "test-admin";

    private static final String setUsernameText = "document.getElementById(\"username\").value = \"" + testUsername + "\"";
    private static final String setPasswordText = "document.getElementById(\"password\").value = '";
    private static final String clickLoginButton = "document.getElementById('kc-login').click()";
    private static final String getUrl = "console.log(window.location.href)";
    @Rule(order = 1)
    public ActivityScenarioRule<ZevrantServices> activityRule
            = new ActivityScenarioRule<>(ZevrantServices.class);

    @Rule(order = 0)
    public HiltAndroidRule hiltRule = new HiltAndroidRule(this);

    @Inject
    EncryptionService encryptionService;

    @Before
    public void setup() throws Exception {
        SecretsInitializer.init();
        NukeSSLCerts.nuke();
        hiltRule.inject();
        credentialsService.clearAuth();
    }

    @Test
    public void loginTest() throws Exception {
        UiDevice device = UiDevice.getInstance(getInstrumentation());
        UiObject2 allowButton = device.findObject(By.clickable(true).text("ALLOW"));
        if(allowButton != null) {
            assertThat("Allow media access button not clickable", allowButton.isClickable(), is(true));
            allowButton.click();
        }
        onView(withId(R.id.loginForm))
                .check(matches(withEffectiveVisibility(ViewMatchers.Visibility.VISIBLE)));
        onView(withId(R.id.username))
            .check(matches(withEffectiveVisibility(ViewMatchers.Visibility.VISIBLE)));
        onView(withId(R.id.password))
                .check(matches(withEffectiveVisibility(ViewMatchers.Visibility.VISIBLE)));
        onView(withId(R.id.loginButton))
                .check(matches(withEffectiveVisibility(ViewMatchers.Visibility.VISIBLE)));

        onView(withId(R.id.username))
                .perform(ViewActions.typeText("test-admin"));
        assertThat("Password for user test-admin not found", Secrets.getPassword("test-admin"), is(not(nullValue())));
        onView(withId(R.id.password))
                .perform(ViewActions.typeText(Secrets.getPassword("test-admin")));
        onView(withId(R.id.loginButton))
                .perform(ViewActions.click());
        Thread.sleep(2000);
        onView(withId(R.id.imageList))
                .check(matches(withEffectiveVisibility(ViewMatchers.Visibility.VISIBLE)));
        assertThat("Encryption Service did not have expected token", encryptionService.hasSecret(Constants.SecretNames.REFRESH_TOKEN_1), is(true));
        assertThat("Credentials Service did not have a valid token", credentialsService.getAuthorization(), is(not(nullValue())));
    }



}
