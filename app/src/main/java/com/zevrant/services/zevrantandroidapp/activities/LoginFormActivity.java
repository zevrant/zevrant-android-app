package com.zevrant.services.zevrantandroidapp.activities;

import android.app.Activity;
import android.content.Intent;
import android.content.IntentSender;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.google.android.gms.auth.api.credentials.Credential;
import com.google.android.gms.auth.api.credentials.CredentialRequestResult;
import com.google.android.gms.auth.api.credentials.Credentials;
import com.google.android.gms.auth.api.credentials.CredentialsClient;
import com.google.android.gms.common.api.ResolvableApiException;
import com.google.android.gms.tasks.Task;
import com.zevrant.services.zevrantandroidapp.R;
import com.zevrant.services.zevrantandroidapp.pojo.OAuthToken;
import com.zevrant.services.zevrantandroidapp.services.CredentialsService;
import com.zevrant.services.zevrantandroidapp.services.JsonParser;
import com.zevrant.services.zevrantandroidapp.services.OAuthService;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LoginFormActivity extends Activity {

    private static final Logger logger = LoggerFactory.getLogger(LoginFormActivity.class);

    private EditText usernameField;
    private EditText passwordField;
    private Button submitButton;
    private CredentialsClient credentialsClient;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.login_form);
        credentialsClient = Credentials.getClient(this);

        usernameField = findViewById(R.id.LoginFormUsername);
        passwordField = findViewById(R.id.LoginFormPassword);
        submitButton = findViewById(R.id.LoginFormSubmitButton);

        submitButton.setOnClickListener((view) -> {
            String username = usernameField.getText().toString();
            String password = passwordField.getText().toString();

            logger.info("Username is {}", username);

            OAuthService.login(username, password, response -> {
                OAuthToken oAuthToken = JsonParser.readValueFromString(response.toString(), OAuthToken.class);
                if (oAuthToken != null && StringUtils.isNotBlank(oAuthToken.getAccessToken())) {
                    try {
                        Credential credential = new Credential.Builder(username)
                                .setPassword(password)
//                                .setAccountType(getString(R.string.oauth_base_url))
                                .build();
                        CredentialsService.setCredential(credential);
                        Task<Void> task = credentialsClient.save(credential);
                        task.addOnCompleteListener(task1 -> {
                            if (task.isSuccessful()) {
                                logger.info("Credentials Successfully Saved ");
                                CredentialsService.setCredential(credential);
                                Intent intent = new Intent(this, ZevrantServices.class);
                                startActivity(intent);
                            } else {
                                Exception e = task.getException();
                                if (e instanceof ResolvableApiException) {
                                    // Try to resolve the save request. This will prompt the user if
                                    // the credential is new.
                                    ResolvableApiException rae = (ResolvableApiException) e;
                                    try {
                                        rae.startResolutionForResult(this, 0); //TODO not sure what the int here signifies, supposed to use the constant RC_SAVE idk where that comes from
                                        CredentialsService.setCredential(credential);
                                    } catch (IntentSender.SendIntentException exception) {
                                        // Could not resolve the request
                                        logger.error("Failed to send resolution.", exception);
                                        Toast.makeText(this, "Save failed", Toast.LENGTH_SHORT).show();
                                    }
                                } else if (e != null) {
                                    // Request has no resolution

                                    logger.error(ExceptionUtils.getStackTrace(e));
                                    Toast.makeText(this, "Save failed", Toast.LENGTH_SHORT).show();
                                } else {
                                    logger.error("Login Task threw an exception but no exception could be retrieved");
                                    Toast.makeText(this, "Save failed", Toast.LENGTH_SHORT).show();
                                }
                            }
                        });

                    } catch (Exception ex) {
                        logger.error(ExceptionUtils.getStackTrace(ex));
                    }
                }
            });

        });
    }
}
