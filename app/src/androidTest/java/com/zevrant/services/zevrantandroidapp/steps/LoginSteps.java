package com.zevrant.services.zevrantandroidapp.steps;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.action.ViewActions.typeText;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.nullValue;

import android.app.Instrumentation;
import android.content.Context;

import androidx.test.espresso.Espresso;
import androidx.test.espresso.action.ViewActions;
import androidx.test.espresso.assertion.ViewAssertions;
import androidx.test.espresso.matcher.ViewMatchers;
import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.uiautomator.UiDevice;
import androidx.test.uiautomator.UiObject;
import androidx.test.uiautomator.UiObjectNotFoundException;
import androidx.test.uiautomator.UiSelector;

import com.google.android.gms.auth.api.credentials.Credential;
import com.zevrant.services.zevrantandroidapp.R;
import com.zevrant.services.zevrantandroidapp.secrets.Secrets;
import com.zevrant.services.zevrantandroidapp.secrets.SecretsInitializer;
import com.zevrant.services.zevrantandroidapp.services.CredentialsService;

import java.util.concurrent.ExecutionException;

import io.cucumber.java.en.And;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.When;

public class LoginSteps {

    private BasicSteps basicSteps;

    public LoginSteps() {
        this.basicSteps = new BasicSteps();
    }

    @And("I verify no smart lock password has been saved")
    public void verifySmartLockDoesNotExist() {
        CredentialsService.deleteSmartLockCredentials(BasicSteps.getTargetContext());
    }

    @When("^I click the login button$")
    public void clickLoginButton() throws UiObjectNotFoundException {
        onView(withId(R.id.LoginFormSubmitButton))
                .perform(click());
        Instrumentation instrumentation = InstrumentationRegistry.getInstrumentation();
        UiDevice device = UiDevice.getInstance(instrumentation);
        UiObject savePassword = device.findObject(new UiSelector().textContains("save").clickable(true));
        savePassword.waitForExists(5000);
        savePassword.click();
        device.findObject(new UiSelector().textContains("save").clickable(true));
        savePassword.waitForExists(5000);
        savePassword.click();
    }
    
    @And("^I enter the login username for user (\\S+)$")
    public void typeUsername(String username) {
        onView(withId(R.id.LoginFormUsername))
                .check(matches(isDisplayed()))
                .perform(typeText(username))
                .check(matches(withText(username)));
        Espresso.closeSoftKeyboard();
    }

    @And("^I enter the login password for user (\\S+)$")
    public void typePassword(String username){
        String password = BasicSteps.getPasswordByUsername(username);
        onView(withId(R.id.LoginFormPassword))
                .check(matches(isDisplayed()))
                .perform(typeText(password))
                .check(matches(withText(password)));
        Espresso.closeSoftKeyboard();
    }

    @Given("I have logged in as (\\S+)$")
    public void login(String username) {
        basicSteps.pressLoginButton();
    }

    @And("^I am logged in as (\\S+)$")
    public void verifyLogin(String username) throws InterruptedException, UiObjectNotFoundException {
        Credential credential = CredentialsService.getCredential();
        if(credential == null) {
            onView(withId(R.id.loginButton))
                    .check(ViewAssertions.matches(ViewMatchers.isDisplayed()))
                    .perform(ViewActions.click());
            typeUsername(username);
            typePassword(username);
            clickLoginButton();
            Thread.sleep(3000);
            onView(withId(R.id.LoginFormSubmitButton))
                    .check(ViewAssertions.matches(ViewMatchers.isDisplayed()));
        }
        assertThat(credential, is(not(nullValue())));
        assertThat(credential.getId(), is(username));
    }
}
