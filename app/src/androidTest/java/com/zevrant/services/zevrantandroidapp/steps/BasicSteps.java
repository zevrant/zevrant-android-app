package com.zevrant.services.zevrantandroidapp.steps;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.platform.app.InstrumentationRegistry.getInstrumentation;
import static org.acra.ACRA.LOG_TAG;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

import android.app.Instrumentation;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import androidx.test.core.app.ActivityScenario;
import androidx.test.espresso.Espresso;
import androidx.test.espresso.assertion.ViewAssertions;
import androidx.test.espresso.matcher.ViewMatchers;
import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.uiautomator.UiDevice;
import androidx.test.uiautomator.UiObject;
import androidx.test.uiautomator.UiObjectNotFoundException;
import androidx.test.uiautomator.UiSelector;
import androidx.work.Configuration;
import androidx.work.testing.SynchronousExecutor;
import androidx.work.testing.WorkManagerTestInitHelper;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.zevrant.services.zevrantandroidapp.R;
import com.zevrant.services.zevrantandroidapp.activities.ZevrantServices;
import com.zevrant.services.zevrantandroidapp.exceptions.CredentialsNotFoundException;
import com.zevrant.services.zevrantandroidapp.pojo.OAuthToken;
import com.zevrant.services.zevrantandroidapp.secrets.Secrets;
import com.zevrant.services.zevrantandroidapp.secrets.SecretsInitializer;
import com.zevrant.services.zevrantandroidapp.services.BackupService;
import com.zevrant.services.zevrantandroidapp.services.CleanupService;
import com.zevrant.services.zevrantandroidapp.services.EncryptionService;
import com.zevrant.services.zevrantandroidapp.services.NoOpEncryptionService;
import com.zevrant.services.zevrantandroidapp.services.OAuthService;
import com.zevrant.services.zevrantandroidapp.utilities.CucumberScreenCaptureProcessor;
import com.zevrant.services.zevrantandroidapp.utilities.TestConstants;

import org.apache.commons.lang3.StringUtils;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeoutException;

import io.cucumber.core.api.Scenario;
import io.cucumber.java.After;
import io.cucumber.java.AfterStep;
import io.cucumber.java.Before;
import io.cucumber.java.BeforeStep;
import io.cucumber.java.en.And;
import io.cucumber.java.en.Given;

public class BasicSteps {

    private static final Map<String, Object> context = new HashMap<>();
    private final CucumberScreenCaptureProcessor captureProcessor = new CucumberScreenCaptureProcessor();
    private ActivityScenario<ZevrantServices> scenario;
    private ZevrantServices zevrantActivity;
    private Scenario cucumberScenario;

    public BasicSteps() {

    }

    public static void addContextData(String key, Object value) {
        context.put(key, value);
    }

    public static Object getContextData(String key) {
        return context.get(key);
    }

    public static Context getTargetContext() {
        return getInstrumentation().getTargetContext();
    }

    public static Context getTestContext() {
        return getInstrumentation().getContext();
    }

    public static String getPasswordByUsername(String username) {
        String password = Secrets.getPassword(username);
        if (StringUtils.isBlank(password)) {
            throw new RuntimeException("Password for requested username ".concat(username).concat(" does not exist!!!"));
        }
        return password;
    }

    public ZevrantServices getZevrantActivity() {
        return zevrantActivity;
    }

    @Before
    public void setup() {
        EncryptionService.init(new NoOpEncryptionService());
        Log.d(LOG_TAG, "Starting Application");
        scenario = ActivityScenario.launch(ZevrantServices.class);

        scenario.onActivity((activity) -> {
            zevrantActivity = activity;
            Configuration config = new Configuration.Builder()
                    .setMinimumLoggingLevel(Log.DEBUG)
                    .setExecutor(new SynchronousExecutor())
                    .build();

            WorkManagerTestInitHelper.initializeTestWorkManager(
                    getTargetContext(), config);


//            zevrantActivity.initServices(getTargetContext());
        });
    }

    @After
    public void tearDown() throws CredentialsNotFoundException, ExecutionException, InterruptedException, TimeoutException, JsonProcessingException {
        //Cannot access runtime values after app has already stopped even if stored in static context,
        //this includes oauth token and values stored in SharedPreferences
        OAuthToken oAuthToken = new ObjectMapper().readValue(OAuthService.login("test-admin", Secrets.getPassword("test-admin")).get(), OAuthToken.class);
        Future<String> future = CleanupService.eraseBackups(oAuthToken.getAccessToken());
        assertThat(future.get(TestConstants.DEFAULT_TIMEOUT_INTERVAL, TestConstants.DEFAULT_TIMEOUT_UNIT), is(not(containsString("error"))));
        future =
                BackupService.getAlllHashes(oAuthToken.getAccessToken());
        String responseString = future.get(TestConstants.DEFAULT_TIMEOUT_INTERVAL, TestConstants.DEFAULT_TIMEOUT_UNIT);
        ObjectMapper objectMapper = new ObjectMapper();
        List<String> hashes = objectMapper.readValue(responseString, new TypeReference<List<String>>() {
        });
        assertThat("Failed to delete hashes", hashes.size(), is(0));

    }

    @AfterStep
    public void afterStep(Scenario scenario) throws IOException {
        embedPhoto();
    }

    @BeforeStep
    public void beforeStep(Scenario scenario) throws IOException {
        cucumberScenario = scenario;
        if (this.zevrantActivity != null) {
            embedPhoto();
        }
    }

    private void embedPhoto() throws IOException {
        byte[] bytes = captureProcessor.takeScreenshot(null);
        assertThat(bytes.length, is(greaterThan(0)));
        cucumberScenario.embed(bytes, "image/png");
    }

    @Given("^I start the application$")
    public void iStartTheApplication() throws InterruptedException, UiObjectNotFoundException, IOException {
        assertNotNull(zevrantActivity);
        SecretsInitializer.init();
        EncryptionService.init(new NoOpEncryptionService());
        grantStoragePermission();
        Thread.sleep(30000);
        this.embedPhoto();
    }

    @And("I verify the page transition to the login page")
    public void iVerifyThePageTransitionToHomeActivity() {
        onView(withId(R.id.LoginFormSubmitButton))
                .check(ViewAssertions.matches(ViewMatchers.isDisplayed())); //Check if the login submit button exists
    }

    @And("I verify the page transition to the home page")
    public void iVerifyThePageTransitionToLoginFormActivity() throws InterruptedException {
        Thread.sleep(3000);
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
        Instrumentation instrumentation = getInstrumentation();
        UiObject allowPermission = UiDevice.getInstance(instrumentation).findObject(new UiSelector().textMatches("Allow|ALLOW|allow"));
        allowPermission.waitForExists(10000);
        if (allowPermission.exists()) {
            allowPermission.click();
        }
    }

    @And("^I close the keyboard$")
    public void closeKeyboard() {
        Espresso.closeSoftKeyboard();
    }

}
