package com.zevrant.services.zevrantandroidapp.activities;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.IntentSender;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.google.android.material.snackbar.BaseTransientBottomBar;
import com.google.android.material.snackbar.Snackbar;
import com.zevrant.services.zevrantandroidapp.R;
import com.zevrant.services.zevrantandroidapp.pojo.Credential;
import com.zevrant.services.zevrantandroidapp.pojo.OAuthToken;
import com.zevrant.services.zevrantandroidapp.services.CredentialsService;
import com.zevrant.services.zevrantandroidapp.services.DefaultRequestHandlers;
import com.zevrant.services.zevrantandroidapp.services.EncryptionService;
import com.zevrant.services.zevrantandroidapp.services.JsonParser;
import com.zevrant.services.zevrantandroidapp.services.OAuthService;
import com.zevrant.services.zevrantandroidapp.utilities.Constants;

import org.acra.ACRA;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

public class LoginFormActivity extends Activity {

    private static final Logger logger = LoggerFactory.getLogger(LoginFormActivity.class);

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

            logger.info("Username is {}", username);
            Future<String> future = OAuthService.login(username, password);
            startLoadingIcon();

            LoginFormActivity activity = this;

            new Thread(() -> {
                OAuthToken oAuthToken = null;
                try {
                    oAuthToken = JsonParser.readValueFromString(future.get(), OAuthToken.class);;
                    if (oAuthToken != null && StringUtils.isNotBlank(oAuthToken.getAccessToken())) {
                        try {
                            saveCredentials(username, password);
                            String decryptedUsername = EncryptionService.getSecret(Constants.SecretNames.LOGIN_USER_NAME);
                            if(!decryptedUsername.equals("zevrant")) {
                                throw new RuntimeException("Decrypted username does not match p[re-encryption username!!!");//sanity check
                            }
                        } catch (Exception ex) {
                            logger.error(ExceptionUtils.getStackTrace(ex));
                            ACRA.getErrorReporter().handleSilentException(ex);
                        }
                    }
                } catch (ExecutionException | InterruptedException e) {
                    e.printStackTrace();
                    ACRA.getErrorReporter().handleSilentException(e);
                    logger.error(ExceptionUtils.getMessage(e));
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
//            OAuthService.login(username, password, response -> {
//                OAuthToken oAuthToken = JsonParser.readValueFromString(response, OAuthToken.class);
//                if (oAuthToken != null && StringUtils.isNotBlank(oAuthToken.getAccessToken())) {
//                    try {
//                        saveCredentials(username, password);
//                    } catch (Exception ex) {
//                        logger.error(ExceptionUtils.getStackTrace(ex));
//                        ACRA.getErrorReporter().handleSilentException(ex);
//
//                    }
//                }
//            },
//                    DefaultRequestHandlers.getErrorResponseListener());

        });
    }

    private void startLoadingIcon() {

    }

    private void stopLoadingIcon() {

    }

    private void saveCredentials(String username, String password) {
        EncryptionService.setSecret(Constants.SecretNames.LOGIN_USER_NAME, username);
        EncryptionService.setSecret(Constants.SecretNames.LOGIN_PASSWORD, password);
    }

}
