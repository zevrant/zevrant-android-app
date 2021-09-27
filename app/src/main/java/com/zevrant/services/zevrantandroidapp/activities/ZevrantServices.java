package com.zevrant.services.zevrantandroidapp.activities;

import static org.acra.ACRA.LOG_TAG;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.view.autofill.AutofillManager;
import android.widget.Button;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.work.Constraints;
import androidx.work.Data;

import com.google.android.material.bottomappbar.BottomAppBar;
import com.zevrant.services.zevrantandroidapp.R;
import com.zevrant.services.zevrantandroidapp.jobs.PhotoBackup;
import com.zevrant.services.zevrantandroidapp.jobs.UpdateJob;
import com.zevrant.services.zevrantandroidapp.services.BackupService;
import com.zevrant.services.zevrantandroidapp.services.EncryptionService;
import com.zevrant.services.zevrantandroidapp.services.EncryptionServiceImpl;
import com.zevrant.services.zevrantandroidapp.services.OAuthService;
import com.zevrant.services.zevrantandroidapp.services.RequestQueueService;
import com.zevrant.services.zevrantandroidapp.services.UpdateService;
import com.zevrant.services.zevrantandroidapp.utilities.Constants;
import com.zevrant.services.zevrantandroidapp.utilities.JobUtilities;

import org.acra.ACRA;
import org.apache.commons.lang3.exception.ExceptionUtils;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

public class ZevrantServices extends Activity {

    private Button loginButton;
    private BottomAppBar bottomAppBar;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        initServices(getApplicationContext());
        getApplicationContext().getSystemService(AutofillManager.class)
                .disableAutofillServices();

        setContentView(R.layout.activity_main);
        initViewGlue();
        checkPermissions();
    }

    private void checkPermissions() {
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

    public void initServices(Context context) {
        try {
            EncryptionService.init(new EncryptionServiceImpl(context));
            RequestQueueService.init(getFilesDir());
            OAuthService.init(context);
            BackupService.init(context);
//            CredentialsService.init(); //no init needed
            UpdateService.init(context);

        } catch (IOException ex) {
            Log.e(LOG_TAG, ex.getMessage() + ExceptionUtils.getStackTrace(ex));
            ACRA.getErrorReporter().handleSilentException(ex);

        }
    }

    private void startServices() {
        Constraints constraints = new Constraints.Builder()
                .setTriggerContentMaxDelay(1, TimeUnit.SECONDS)
                .build();
        Data data = new Data.Builder().build();
        JobUtilities.schedulePeriodicJob(getApplicationContext(), UpdateJob.class, constraints, Constants.JobTags.UPDATE_TAG, data);
        JobUtilities.schedulePeriodicJob(getApplicationContext(), PhotoBackup.class, constraints, Constants.JobTags.BACKUP_TAG, data);

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

}