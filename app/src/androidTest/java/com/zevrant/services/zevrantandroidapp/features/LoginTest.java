package com.zevrant.services.zevrantandroidapp.features;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNot.not;
import static org.hamcrest.core.IsNull.nullValue;

import androidx.test.espresso.intent.Intents;
import androidx.test.ext.junit.rules.ActivityScenarioRule;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.zevrant.services.zevrantandroidapp.activities.ZevrantServices;
import com.zevrant.services.zevrantandroidapp.exceptions.CredentialsNotFoundException;
import com.zevrant.services.zevrantandroidapp.services.CredentialsService;
import com.zevrant.services.zevrantandroidapp.services.EncryptionService;
import com.zevrant.services.zevrantandroidapp.test.BaseTest;
import com.zevrant.services.zevrantandroidapp.utilities.Constants;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.IOException;

@RunWith(AndroidJUnit4.class)
public class LoginTest extends BaseTest {

    private static final String testUsername = "test-admin";

    private static final String setUsernameText = "document.getElementById(\"username\").value = \"" + testUsername + "\"";
    private static final String setPasswordText = "document.getElementById(\"password\").value = '";
    private static final String clickLoginButton = "document.getElementById('kc-login').click()";
    private static final String getUrl = "console.log(window.location.href)";
    @Rule
    public ActivityScenarioRule<ZevrantServices> activityRule
            = new ActivityScenarioRule<>(ZevrantServices.class);

    @Before
    public void setup() throws IOException, NoSuchFieldException, IllegalAccessException {
        super.setup();
        Intents.init();
    }

    @After
    public void tearDown() {
        Intents.release();
    }

    @Test
    public void loginRedirectTest() throws CredentialsNotFoundException {
        assertThat("Encryption Service did not have expected token", EncryptionService.hasSecret(Constants.SecretNames.REFRESH_TOKEN_1), is(true));
        assertThat("Credentials Service did not have a valid token", CredentialsService.getAuthorization(), is(not(nullValue())));
    }


}
