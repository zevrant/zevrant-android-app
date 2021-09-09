package com.zevrant.services.zevrantandroidapp.steps;

import static android.content.Context.ACCOUNT_SERVICE;
import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.app.Instrumentation;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.util.Log;
import android.widget.EditText;

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

import com.zevrant.services.zevrantandroidapp.R;
import com.zevrant.services.zevrantandroidapp.activities.ZevrantServices;
import com.zevrant.services.zevrantandroidapp.exceptions.CredentialsNotFoundException;
import com.zevrant.services.zevrantandroidapp.secrets.Secrets;
import com.zevrant.services.zevrantandroidapp.secrets.SecretsInitializer;
import com.zevrant.services.zevrantandroidapp.services.CleanupService;
import com.zevrant.services.zevrantandroidapp.services.CredentialsService;
import com.zevrant.services.zevrantandroidapp.utilities.TestConstants;

import org.apache.commons.lang3.StringUtils;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeoutException;

import io.cucumber.java.After;
import io.cucumber.java.Before;
import io.cucumber.java.en.And;
import io.cucumber.java.en.Given;

public class BasicSteps {

    private ActivityScenario<ZevrantServices> scenario;
    private ZevrantServices zevrantActivity;
    private static final Map<String, Object> context = new HashMap<>();
    ;

    public BasicSteps() {

    }

    public static void addContextData(String key, Object value) {
        context.put(key, value);
    }

    public static Object getContextData(String key) {
        return context.get(key);
    }

    public static Context getTargetContext() {
        return InstrumentationRegistry.getInstrumentation().getTargetContext();
    }

    public static Context getTestContext() {
        return InstrumentationRegistry.getInstrumentation().getContext();
    }

    public ZevrantServices getZevrantActivity() {
        return zevrantActivity;
    }

    @Before
    public void setup() {
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
    public void tearDown() throws CredentialsNotFoundException, ExecutionException, InterruptedException, TimeoutException {
//        scenario.close();
        context.clear();
        if(CredentialsService.getCredential() == null) {
            return;
        }
        if (CredentialsService.hasAuthorization()) {
            CredentialsService.getAuthorization();
        }
        Future<String> future = CleanupService.eraseBackups(CredentialsService.getAuthorization());
        assertThat(future.get(TestConstants.DEFAULT_TIMEOUT_INTERVAL, TestConstants.DEFAULT_TIMEOUT_UNIT), is(not(containsString("error"))));
    }

    @Given("^I start the application$")
    public void iStartTheApplication() throws InterruptedException, UiObjectNotFoundException {
        assertNotNull(zevrantActivity);
        SecretsInitializer.init();
        grantStoragePermission();

        if (CredentialsService.getCredential() == null) {
            Intent playStoreIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store"));
            playStoreIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            getTargetContext().startActivity(playStoreIntent);
            Thread.sleep(2000);
            Instrumentation instrumentation = InstrumentationRegistry.getInstrumentation();
            UiDevice device = UiDevice.getInstance(instrumentation);
            UiObject signIn = device.findObject(new UiSelector().textStartsWith("SIGN IN").clickable(true));
            if(signIn.exists()) {
                signIn.click();
                Thread.sleep(2000);
                UiObject email = device.findObject(new UiSelector().className(EditText.class));
                email.click();
                email.setText("zevrantservices@gmail.com");
                UiObject next = device.findObject(new UiSelector().textStartsWith("NEXT"));
                next.click();
                UiObject password = device.findObject(new UiSelector().focused(true));
                password.setText(Secrets.getPassword("zevrantservices@gmail.com"));
                next = device.findObject(new UiSelector().textStartsWith("NEXT"));
                next.click();
                UiObject uiObject = device.findObject(new UiSelector().textStartsWith("I agree"));
                uiObject.waitForExists(10000);
                uiObject.click();
                UiObject more = device.findObject(new UiSelector().textStartsWith("MORE"));
                more.waitForExists(3000);
                assertThat(more.exists(), is(true));
                while (more.exists()) {
                    more.click();
                }
                UiObject accept = device.findObject(new UiSelector().textStartsWith("ACCEPT"));
                accept.click();
            }
            Intent intent = zevrantActivity.getIntent();
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            getTargetContext().startActivity(intent);
            Thread.sleep(3000);
        }
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
        Instrumentation instrumentation = InstrumentationRegistry.getInstrumentation();
        UiObject allowPermission = UiDevice.getInstance(instrumentation).findObject(new UiSelector().text("Allow"));
        allowPermission.waitForExists(3000);
        if (allowPermission.exists()) {
            allowPermission.click();
        } else {
            fail("unable to find permission request menu button");
        }
    }

    @And("^I grant permission to save credentials")
    public void grantCredentialsSave() throws UiObjectNotFoundException {
        Instrumentation instrumentation = InstrumentationRegistry.getInstrumentation();
        UiObject allowPermission = UiDevice.getInstance(instrumentation).findObject(new UiSelector().textMatches("SAVE|save"));
        allowPermission.waitForExists(5000);
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
        if (StringUtils.isBlank(password)) {
            throw new RuntimeException("Password for requested username ".concat(username).concat(" does not exist!!!"));
        }
        return password;
    }

}
