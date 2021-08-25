package com.zevrant.services.zevrantandroidapp.activities;

import android.Manifest;
import android.app.Activity;
import android.app.ActivityManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.view.autofill.AutofillManager;
import android.widget.Button;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.android.gms.auth.api.credentials.Credential;
import com.google.android.gms.auth.api.credentials.CredentialRequest;
import com.google.android.gms.auth.api.credentials.Credentials;
import com.google.android.gms.auth.api.credentials.CredentialsClient;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.android.material.bottomappbar.BottomAppBar;
import com.zevrant.services.zevrantandroidapp.R;
import com.zevrant.services.zevrantandroidapp.pojo.CredentialWrapper;
import com.zevrant.services.zevrantandroidapp.services.BackupService;
import com.zevrant.services.zevrantandroidapp.services.CredentialsService;
import com.zevrant.services.zevrantandroidapp.services.OAuthService;
import com.zevrant.services.zevrantandroidapp.services.RequestQueueService;
import com.zevrant.services.zevrantandroidapp.services.UpdateService;
import com.zevrant.services.zevrantandroidapp.services.android.MainService;

import org.acra.ACRA;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Observable;
import java.util.Observer;

public class ZevrantServices extends Activity implements Observer {

    private static final Logger logger = LoggerFactory.getLogger(ZevrantServices.class);

    private Button loginButton;
    private BottomAppBar bottomAppBar;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        initServices();
        getApplicationContext().getSystemService(AutofillManager.class)
                .disableAutofillServices();
        checkIfGooglePlayInstalled();

        setContentView(R.layout.activity_main);

        initViewGlue();
        checkPermissions();
        getCredentials();
    }

    private void checkPermissions() {
        logger.info("{}", ContextCompat.checkSelfPermission(
                getApplicationContext(), Manifest.permission.READ_EXTERNAL_STORAGE));
        if (ContextCompat.checkSelfPermission(
                getApplicationContext(), Manifest.permission.READ_EXTERNAL_STORAGE) !=
                PackageManager.PERMISSION_GRANTED) {
            // You can use the API that requires the permission.
            // The registered ActivityResultCallback gets the result of this request.
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, 0);
        }
    }

    private void initViewGlue() {
        bottomAppBar = findViewById(R.id.mainActivityBottomAppBar);
        loginButton = findViewById(R.id.loginButton);

        loginButton.setOnClickListener((view) -> {
            Intent intent = new Intent(this, LoginFormActivity.class);
            startActivity(intent);
        });
    }

    private void getCredentials() {
        CredentialsClient credentialsClient = Credentials.getClient(this);
        CredentialRequest credentialRequest = new CredentialRequest.Builder()
                .setPasswordLoginSupported(true)
                .setAccountTypes(getString(R.string.oauth_base_url))
                .build();
        Context context = getApplicationContext();
        credentialsClient.request(credentialRequest).addOnCompleteListener((task) -> {
            if(task.isSuccessful()) {
                Credential credential = task.getResult().getCredential();
                if(credential.getId() == null) {
                    logger.error("LIES!!!! google smartlock responded success but does not have credentials");
                    ACRA.getErrorReporter().handleSilentException(new RuntimeException("Invalid Credentials State"));
                }
                CredentialsService.setCredential(credential);
                if(!Boolean.parseBoolean(context.getString(R.string.manualServiceTesting))) {
                    startServices(credential.getId(), credential.getPassword());
                }
            } else {
                logger.info("failed to retrieve login credentials");

            }
        });
    }

    private void checkIfGooglePlayInstalled() {
        int result = GoogleApiAvailability.getInstance().isGooglePlayServicesAvailable(getApplicationContext());
        switch (result) {
            case ConnectionResult.SUCCESS:
                logger.info("Connection Success no action needed");
                break;
            case ConnectionResult.SERVICE_MISSING:
                logger.error("Google Play Services Missing");
                break;
            case ConnectionResult.SERVICE_UPDATING:
                logger.info("Google Play Services updating retring in 1 minute");
                break;
            case ConnectionResult.SERVICE_VERSION_UPDATE_REQUIRED:
                logger.info("Google Play Services update required");
                break;
            case ConnectionResult.SERVICE_DISABLED:
                logger.error("Google Play Services disabled");
                break;
            case ConnectionResult.SERVICE_INVALID:
                logger.error("Google Play Services invalid?");
                break;
        }
    }

    private void initServices() {
        try {
            RequestQueueService.init(getFilesDir());
            OAuthService.init(getApplicationContext());
            BackupService.init(getApplicationContext());
            CredentialsService.init(getApplicationContext());
            UpdateService.init(getApplicationContext());
        } catch (IOException ex) {
            logger.error(ex.getMessage() + ExceptionUtils.getStackTrace(ex));
            ACRA.getErrorReporter().handleSilentException(ex);

        }
    }

    private void startServices(String username, String password) {
        Intent serviceIntent = new Intent(this, MainService.class);
        serviceIntent.putExtra("username", username);
        serviceIntent.putExtra("password", password);
        if(isMyServiceRunning(MainService.class)) {
            logger.info("Service has been found running, stopping...");
            if(!stopService(serviceIntent)) {
                throw new RuntimeException("Failed to stop Main Service aborting");
            }
        }
        startService(serviceIntent);
    }

    private boolean isMyServiceRunning(Class<?> serviceClass) {
        boolean isRunning = false;
        ActivityManager manager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
        for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
            isRunning = isRunning || serviceClass.getName().equals(service.service.getClassName());
        }
        return isRunning;
    }

    public boolean hasAtLeastOneVisibleChild(ViewGroup parent) {
        int childCount = parent.getChildCount();
        boolean hasVisibleChild = false;
        for (int i = 0; i < childCount; i++) {
            View childView = parent.getChildAt(i);
            if (childView instanceof ViewGroup) {
                hasVisibleChild = hasVisibleChild || hasAtLeastOneVisibleChild((ViewGroup) childView);
            }
            hasVisibleChild = hasVisibleChild || childView.getVisibility() == View.VISIBLE;
        }

        return hasVisibleChild;
    }

    private void setLoginVisibility(int visibility) {
        loginButton.setVisibility(visibility);
    }

    private void updateBottomAppBar() {
        if (!hasAtLeastOneVisibleChild(bottomAppBar)) {
            bottomAppBar.setVisibility(View.INVISIBLE);
        }
    }

    @Override
    public void update(Observable o, Object arg) {
        if(o instanceof CredentialWrapper) {
            Credential credential = ((CredentialWrapper) o).getCredential();
            startServices(credential.getId(), credential.getPassword());
        } else {
            logger.info("Observable not instance of Credential Wrapper");
        }
    }
}