package com.zevrant.services.zevrantandroidapp.activities;

import android.Manifest;
import android.app.Activity;
import android.app.ActivityManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.view.autofill.AutofillManager;
import android.widget.Button;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.work.Constraints;
import androidx.work.Data;

import com.google.android.gms.auth.api.credentials.Credential;
import com.google.android.gms.auth.api.credentials.CredentialRequest;
import com.google.android.gms.auth.api.credentials.Credentials;
import com.google.android.gms.auth.api.credentials.CredentialsClient;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.android.material.bottomappbar.BottomAppBar;
import com.zevrant.services.zevrantandroidapp.R;
import com.zevrant.services.zevrantandroidapp.jobs.PhotoBackup;
import com.zevrant.services.zevrantandroidapp.jobs.UpdateJob;
import com.zevrant.services.zevrantandroidapp.pojo.CredentialWrapper;
import com.zevrant.services.zevrantandroidapp.services.BackupService;
import com.zevrant.services.zevrantandroidapp.services.CredentialsService;
import com.zevrant.services.zevrantandroidapp.services.OAuthService;
import com.zevrant.services.zevrantandroidapp.services.RequestQueueService;
import com.zevrant.services.zevrantandroidapp.services.UpdateService;
import com.zevrant.services.zevrantandroidapp.utilities.Constants;
import com.zevrant.services.zevrantandroidapp.utilities.JobUtilities;

import org.acra.ACRA;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Observable;
import java.util.Observer;
import java.util.concurrent.TimeUnit;

public class ZevrantServices extends Activity implements Observer {

    private static final Logger logger = LoggerFactory.getLogger(ZevrantServices.class);

    private Button loginButton;
    private BottomAppBar bottomAppBar;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        initServices(getApplicationContext());
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
        credentialsClient.request(credentialRequest).addOnCompleteListener((task) -> { //TODO replace smartlock with something else
            if(task.isSuccessful()) {
                Credential credential = task.getResult().getCredential();
                if(credential == null || credential.getId() == null) {
                    logger.error("LIES!!!! google smartlock responded success but does not have credentials");
                    ACRA.getErrorReporter().handleSilentException(new RuntimeException("Invalid Credentials State"));
                }
                CredentialsService.setCredential(credential);
                if(credential != null && !Boolean.parseBoolean(context.getString(R.string.manualServiceTesting))) {
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

    public void initServices(Context context) {
        try {
            RequestQueueService.init(getFilesDir());
            OAuthService.init(context);
            BackupService.init(context);
            CredentialsService.init();
            UpdateService.init(context);
        } catch (IOException ex) {
            logger.error(ex.getMessage() + ExceptionUtils.getStackTrace(ex));
            ACRA.getErrorReporter().handleSilentException(ex);

        }
    }

    private void startServices(String username, String password) {
        Constraints constraints = new Constraints.Builder()
                .setTriggerContentMaxDelay(1, TimeUnit.SECONDS)
                .build();
        Data data = new Data.Builder().build();
        JobUtilities.schedulePeriodicJob(getApplicationContext(), UpdateJob.class, constraints, Constants.UPDATE_TAG, data);
        JobUtilities.schedulePeriodicJob(getApplicationContext(), PhotoBackup.class, constraints, Constants.BACKUP_TAG, data);

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
        if(o instanceof CredentialWrapper
            && !Boolean.parseBoolean(getApplicationContext().getString(R.string.manualServiceTesting))) {
            Credential credential = ((CredentialWrapper) o).getCredential();
            startServices(credential.getId(), credential.getPassword());
        } else {
            logger.info("Observable not instance of Credential Wrapper");
        }
    }
}