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

import com.google.android.gms.auth.api.credentials.Credential;
import com.zevrant.services.zevrantandroidapp.R;
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
    public void clickLoginButton() {
        onView(withId(R.id.LoginFormSubmitButton))
                .perform(click());
    }
    
    @And("^I enter the login username for user (\\S+)$")
    public void typeUsername(String username) {
        onView(withId(R.id.LoginFormUsername))
                .check(matches(isDisplayed()))
                .perform(typeText(username))
                .check(matches(withText(username)));
    }

    @And("^I enter the login password for user (\\S+)$")
    public void typePassword(String username){
        String password = BasicSteps.getPasswordByUsername(username);
        onView(withId(R.id.LoginFormPassword))
                .check(matches(isDisplayed()))
                .perform(typeText(password))
                .check(matches(withText(password)));
    }

    @Given("I have logged in as (\\S+)$")
    public void login(String username) {
        basicSteps.pressLoginButton();
    }

    @And("^I am logged in as (\\S+)$")
    public void verifyLogin(String username) {
        Credential credential = CredentialsService.getCredential();
        assertThat(credential, is(not(nullValue())));
        assertThat(credential.getId(), is(username));
    }
}
