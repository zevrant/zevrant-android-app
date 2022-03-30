package com.zevrant.services.zevrantandroidapp.test;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.withEffectiveVisibility;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.platform.app.InstrumentationRegistry.getInstrumentation;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNot.not;
import static org.hamcrest.core.IsNull.nullValue;
import static org.hamcrest.core.StringContains.containsString;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.test.espresso.Espresso;
import androidx.test.espresso.action.ViewActions;
import androidx.test.espresso.matcher.ViewMatchers;
import androidx.test.ext.junit.rules.ActivityScenarioRule;
import androidx.test.platform.app.InstrumentationRegistry;
import androidx.work.Configuration;
import androidx.work.WorkManager;

import com.zevrant.services.zevrantandroidapp.NukeSSLCerts;
import com.zevrant.services.zevrantandroidapp.R;
import com.zevrant.services.zevrantandroidapp.activities.ZevrantServices;
import com.zevrant.services.zevrantandroidapp.secrets.Secrets;
import com.zevrant.services.zevrantandroidapp.secrets.SecretsInitializer;
import com.zevrant.services.zevrantandroidapp.services.CredentialsService;
import com.zevrant.services.zevrantandroidapp.services.EncryptionService;
import com.zevrant.services.zevrantandroidapp.services.JsonParser;
import com.zevrant.services.zevrantandroidapp.utilities.Constants;
import com.zevrant.services.zevrantuniversalcommon.rest.oauth.response.OAuthToken;

import org.apache.commons.lang3.StringUtils;
import org.junit.Before;
import org.junit.Rule;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.stream.Collectors;

import javax.inject.Inject;

import dagger.hilt.android.testing.HiltAndroidTest;

@HiltAndroidTest
public class BaseTest {

    public Context getTargetContext() {
        return getInstrumentation().getTargetContext();
    }

    public Context getTestContext() {
        return getInstrumentation().getContext();
    }

    @Inject
    protected JsonParser jsonParser;

    @Inject
    protected CredentialsService credentialsService;

    @Inject
    protected EncryptionService encryptionService;

    public void setup(ActivityScenarioRule activityRule) throws Exception {
        SecretsInitializer.init();
        NukeSSLCerts.nuke();
        InstrumentationRegistry.getInstrumentation().getUiAutomation().executeShellCommand("pm clear ".concat(this.getClass().getPackageName()));
        WorkManager.initialize(getTargetContext(), new Configuration.Builder().build());
        SharedPreferences sharedPreferences = getTargetContext().getSharedPreferences("zevrant-services-preferences", Context.MODE_PRIVATE);
        sharedPreferences.edit().clear().commit();
        ZevrantServices.switchToLogin(getTargetContext());
        Thread.sleep(2000);
        onView(withId(com.zevrant.services.zevrantandroidapp.R.id.username))
                .perform(ViewActions.typeText("test-admin"));
        assertThat("Password for user test-admin not found", Secrets.getPassword("test-admin"), is(not(nullValue())));
        onView(withId(com.zevrant.services.zevrantandroidapp.R.id.password))
                .perform(ViewActions.typeText(Secrets.getPassword("test-admin")));
        onView(withId(com.zevrant.services.zevrantandroidapp.R.id.loginButton))
                .perform(ViewActions.click());
        Thread.sleep(2000);
        onView(withId(com.zevrant.services.zevrantandroidapp.R.id.mediaScrollView))
                .check(matches(withEffectiveVisibility(ViewMatchers.Visibility.VISIBLE)));
        assertThat("Encryption Service did not have expected token", encryptionService.hasSecret(Constants.SecretNames.REFRESH_TOKEN_1), is(true));
        assertThat("Credentials Service did not have a valid token", credentialsService.getAuthorization(), is(not(nullValue())));
    }

}
