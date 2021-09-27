package com.zevrant.services.zevrantandroidapp.activities;

import static org.acra.ACRA.LOG_TAG;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

import com.google.android.material.snackbar.Snackbar;
import com.zevrant.services.zevrantandroidapp.R;
import com.zevrant.services.zevrantandroidapp.pojo.OAuthToken;
import com.zevrant.services.zevrantandroidapp.services.CredentialsService;
import com.zevrant.services.zevrantandroidapp.services.EncryptionService;
import com.zevrant.services.zevrantandroidapp.services.EncryptionService;
import com.zevrant.services.zevrantandroidapp.services.JsonParser;
import com.zevrant.services.zevrantandroidapp.services.OAuthService;
import com.zevrant.services.zevrantandroidapp.utilities.Constants;

import org.acra.ACRA;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

public class LoginFormActivity extends Activity {

    private EditText usernameField;
    private EditText passwordField;
    private Button submitButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.login_form);

        usernameField = findViewById(R.id.LoginFormUsername);
        passwordField = findViewById(R.id.LoginFormPassword);
        submitButton = findViewById(R.id.LoginFormSubmitButton);

        submitButton.setOnClickListener((view) -> {
            String username = usernameField.getText().toString();
            String password = passwordField.getText().toString();

            Log.i(LOG_TAG, "Username is ".concat(username));
            Future<String> future = OAuthService.login(username, password);
            startLoadingIcon();

            LoginFormActivity activity = this;

            new Thread(() -> {
                OAuthToken oAuthToken = null;
                try {
                    oAuthToken = JsonParser.readValueFromString(future.get(), OAuthToken.class);

                    if (oAuthToken != null && StringUtils.isNotBlank(oAuthToken.getAccessToken())) {
                        try {
                            Log.d(LOG_TAG, "SAVE USERNAME: ".concat(username));
                            Log.d(LOG_TAG, "SAVE PASSWORD ".concat(password));
                            saveCredentials(username, password, oAuthToken);
                            String decryptedUsername = EncryptionService.getSecret(Constants.SecretNames.LOGIN_USER_NAME);
                            Log.d(LOG_TAG, "USERNAME DECRYPTION: ".concat(username).concat("==").concat(decryptedUsername));
                            if (!decryptedUsername.equals(username)) {
                                throw new RuntimeException("Decrypted username does not match pre-encryption username!!!");//sanity check
                            }
                        } catch (Exception ex) {
                            Log.e(LOG_TAG, ExceptionUtils.getStackTrace(ex));
                            ACRA.getErrorReporter().handleSilentException(ex);
                        }
                    }
                } catch (ExecutionException | InterruptedException e) {
                    e.printStackTrace();
                    ACRA.getErrorReporter().handleSilentException(e);
                    Log.e(LOG_TAG, ExceptionUtils.getMessage(e));
                } finally {
                    stopLoadingIcon();
                    View rootView = findViewById(android.R.id.content);
                    if (oAuthToken != null) {
                        Intent intent = new Intent(this, ZevrantServices.class);
                        startActivity(intent);
                    } else {
                        Snackbar snackbar = Snackbar.make(rootView, R.string.loginFailure, 5000); //
                        snackbar.show();
                    }
                }
            }).start();

        });
    }

    private void startLoadingIcon() {

    }

    private void stopLoadingIcon() {

    }

    private void saveCredentials(String username, String password, OAuthToken oAuthToken) {
        EncryptionService.setSecret(Constants.SecretNames.LOGIN_USER_NAME, username);
        EncryptionService.setSecret(Constants.SecretNames.LOGIN_PASSWORD, password);
        CredentialsService.setOAuthToken(oAuthToken);
    }

}
