package com.zevrant.services.zevrantandroidapp.activities;

import static com.zevrant.services.zevrantandroidapp.utilities.Constants.LOG_TAG;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.view.autofill.AutofillManager;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.FragmentContainerView;
import androidx.fragment.app.FragmentManager;
import androidx.work.Constraints;
import androidx.work.Data;

import com.google.android.material.bottomnavigation.BottomNavigationItemView;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.zevrant.services.zevrantandroidapp.R;
import com.zevrant.services.zevrantandroidapp.ZevrantAndroidApp;
import com.zevrant.services.zevrantandroidapp.exceptions.CredentialsNotFoundException;
import com.zevrant.services.zevrantandroidapp.fragments.LoginFragment;
import com.zevrant.services.zevrantandroidapp.fragments.MediaViewer;
import com.zevrant.services.zevrantandroidapp.pojo.AuthBody;
import com.zevrant.services.zevrantandroidapp.services.BackupService;
import com.zevrant.services.zevrantandroidapp.services.CredentialsService;
import com.zevrant.services.zevrantandroidapp.services.EncryptionService;
import com.zevrant.services.zevrantandroidapp.services.EncryptionServiceImpl;
import com.zevrant.services.zevrantandroidapp.services.JsonParser;
import com.zevrant.services.zevrantandroidapp.services.OAuthService;
import com.zevrant.services.zevrantandroidapp.services.RequestQueueService;
import com.zevrant.services.zevrantandroidapp.services.UserSettingsService;
import com.zevrant.services.zevrantandroidapp.utilities.Constants;
import com.zevrant.services.zevrantandroidapp.utilities.ThreadManager;

import org.acra.ACRA;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

public class ZevrantServices extends AppCompatActivity {

    private BottomNavigationItemView loginButton;
    private BottomNavigationView mainNavView;
    private static FragmentManager fragmentManager;
    private FragmentContainerView mainView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        fragmentManager = getSupportFragmentManager();

        initServices(this);
        this.getSystemService(AutofillManager.class)
                .disableAutofillServices();
        setContentView(R.layout.activity_main);
        initViewGlue();
        checkPermissions();
        Future<Boolean> future = handleRedirect(getIntent());
        ThreadManager.execute(() -> {
            try {
                future.get();
            } catch (ExecutionException | InterruptedException e) {
                Log.e(LOG_TAG, "failed to wait for redirect to be processed");
                ACRA.getErrorReporter().handleSilentException(e);
            }
            if (!CredentialsService.hasAuthorization()
                    && !EncryptionService.hasSecret(Constants.SecretNames.REFRESH_TOKEN_1)
                    && (getIntent() == null || getIntent().getData() == null)) {
                getMainExecutor().execute(() -> ZevrantServices.switchToLogin(this));
            } else {
                try {
                    CredentialsService.getAuthorization(this);
                    loadRoles();
                    startServices();
                    initMediaView();
                } catch (CredentialsNotFoundException ex) {
                    CredentialsService.clearAuth();
                    getMainExecutor().execute(() -> ZevrantServices.switchToLogin(this));
                }
            }
        });
    }

    public static void switchToLogin(Context context) {
        LoginFragment loginView = new LoginFragment();
        fragmentManager.beginTransaction()
                .replace(R.id.mainView, loginView)
                .commit();
        ((Activity) context).findViewById(R.id.accountButton).setVisibility(View.INVISIBLE);
        ((Activity) context).findViewById(R.id.mainNavView).setVisibility(View.INVISIBLE);
    }

    private void initMediaView() {
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.mainView, new MediaViewer())
                .commit();
    }

    private void loadRoles() {
        ThreadManager.execute(() -> OAuthService.loadRoles(this));
    }

    public Future<Boolean> handleRedirect(Intent intent) {
        CompletableFuture<Boolean> future = new CompletableFuture<>();
        new Thread(() -> {

            if (intent != null && intent.getData() != null) {
                String body = intent.getData().getQueryParameter("body");
                assert StringUtils.isNotBlank(body) : "redirect body cannot be null";
                String authBody = new String(
                        Base64.getDecoder().decode(
                                intent.getData().getQueryParameter("body")), StandardCharsets.UTF_8);
                AuthBody auth = JsonParser.readValueFromString(authBody, AuthBody.class);
                try {
                    assert auth != null;

                    CredentialsService.manageOAuthToken(OAuthService.exchangeCode(auth.getCode()), true);
                    CredentialsService.getAuthorization(this);
                    OAuthService.loadRoles(this);
                } catch (ExecutionException | InterruptedException | CredentialsNotFoundException e) {
                    e.printStackTrace();
                    Log.i("failed to exchange authorization code for a token", LOG_TAG);
                    ACRA.getErrorReporter().handleSilentException(e);
                }
            }
            future.complete(true);
        }).start();
        return future;
    }

    private void checkPermissions() {
        if (ContextCompat.checkSelfPermission(
                this, Manifest.permission.READ_EXTERNAL_STORAGE) !=
                PackageManager.PERMISSION_GRANTED) {
            // You can use the API that requires the permission.
            // The registered ActivityResultCallback gets the result of this request.
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 0);
        }
    }

    @SuppressLint("SetJavaScriptEnabled")
    private void initViewGlue() {
        mainNavView = findViewById(R.id.mainNavView);
        loginButton = findViewById(R.id.accountButton);
        mainView = findViewById(R.id.mainView);
    }

    public void initServices(Context context) {
        try {
            EncryptionService.init(new EncryptionServiceImpl(context));
            RequestQueueService.init(getFilesDir());
            OAuthService.init(context);
            BackupService.init(context);
            UserSettingsService.init(context);
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
//        JobUtilities.schedulePeriodicJob(this, PhotoBackup.class, constraints, Constants.JobTags.BACKUP_TAG, data);

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
        if (!hasAtLeastOneVisibleChild(mainNavView)) {
            mainNavView.setVisibility(View.INVISIBLE);
        }
    }

}