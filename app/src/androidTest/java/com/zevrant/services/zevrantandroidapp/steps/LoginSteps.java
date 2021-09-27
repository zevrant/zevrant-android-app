package com.zevrant.services.zevrantandroidapp.steps;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.action.ViewActions.typeText;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.isEnabled;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNot.not;
import static org.hamcrest.core.IsNull.nullValue;

import androidx.test.espresso.Espresso;
import androidx.test.espresso.action.ViewActions;
import androidx.test.espresso.assertion.ViewAssertions;
import androidx.test.espresso.matcher.ViewMatchers;
import androidx.test.uiautomator.UiObjectNotFoundException;

import com.zevrant.services.zevrantandroidapp.R;
import com.zevrant.services.zevrantandroidapp.exceptions.CredentialsNotFoundException;
import com.zevrant.services.zevrantandroidapp.secrets.Secrets;
import com.zevrant.services.zevrantandroidapp.services.CredentialsService;
import com.zevrant.services.zevrantandroidapp.services.EncryptionService;
import com.zevrant.services.zevrantandroidapp.utilities.Constants;

import io.cucumber.java.en.And;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.When;

public class LoginSteps {

    private BasicSteps basicSteps;

    public LoginSteps() {
        this.basicSteps = new BasicSteps();
    }

    @When("^I click the login button$")
    public void clickLoginButton() throws UiObjectNotFoundException {
        onView(withId(R.id.LoginFormSubmitButton))
                .check(ViewAssertions.matches(ViewMatchers.isDisplayed()))
                .check(ViewAssertions.matches(ViewMatchers.isEnabled()))
                .check(ViewAssertions.matches(ViewMatchers.isClickable()))
                .perform(click());
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
    public void loginAs(String username) throws InterruptedException, UiObjectNotFoundException, CredentialsNotFoundException {
        onView(withId(R.id.loginButton))
                .check(ViewAssertions.matches(ViewMatchers.isDisplayed()))
                .perform(ViewActions.click());
        typeUsername(username);
        typePassword(username);
        clickLoginButton();
        Thread.sleep(3000);

        assertThat(EncryptionService.hasSecret(Constants.SecretNames.LOGIN_USER_NAME), is(true));
        assertThat(CredentialsService.getAuthorization(), is(not(nullValue())));
    }
}
