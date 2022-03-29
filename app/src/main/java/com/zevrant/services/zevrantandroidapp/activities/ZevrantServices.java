package com.zevrant.services.zevrantandroidapp.activities;

import static com.zevrant.services.zevrantandroidapp.utilities.Constants.LOG_TAG;

import android.Manifest;
import android.annotation.SuppressLint;
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
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentContainerView;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import androidx.work.Constraints;
import androidx.work.Data;

import com.google.android.material.bottomnavigation.BottomNavigationItemView;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.zevrant.services.zevrantandroidapp.BuildConfig;
import com.zevrant.services.zevrantandroidapp.R;
import com.zevrant.services.zevrantandroidapp.exceptions.CredentialsNotFoundException;
import com.zevrant.services.zevrantandroidapp.fragments.LoginFragment;
import com.zevrant.services.zevrantandroidapp.fragments.MediaViewer;
import com.zevrant.services.zevrantandroidapp.jobs.PhotoBackup;
import com.zevrant.services.zevrantandroidapp.pojo.AuthBody;
import com.zevrant.services.zevrantandroidapp.services.CredentialsService;
import com.zevrant.services.zevrantandroidapp.services.EncryptionService;
import com.zevrant.services.zevrantandroidapp.services.JsonParser;
import com.zevrant.services.zevrantandroidapp.services.OAuthService;
import com.zevrant.services.zevrantandroidapp.utilities.Constants;
import com.zevrant.services.zevrantandroidapp.utilities.JobUtilities;
import com.zevrant.services.zevrantandroidapp.utilities.ThreadManager;

import org.acra.ACRA;
import org.apache.commons.lang3.StringUtils;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;

import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class ZevrantServices extends AppCompatActivity {

    private BottomNavigationItemView loginButton;
    private BottomNavigationView mainNavView;
    private static FragmentManager fragmentManager;
    private FragmentContainerView mainView;
    private EncryptionService encryptionService;
    private CredentialsService credentialsService;
    private OAuthService oauthService;
    private JsonParser jsonParser;
    private static int currentFragment = R.id.mainView;


    public static void setCurrentFragment(int fragmentId) {
        currentFragment = fragmentId;
    }

    public static int getCurrentFragment() {
        return currentFragment;
    }

    //    private static final
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        fragmentManager = getSupportFragmentManager();
        FragmentTransaction transaction = fragmentManager.beginTransaction();
        fragmentManager.getFragments().forEach(transaction::remove);
        transaction.commit();
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
            if (!credentialsService.hasAuthorization()
                    && !encryptionService.hasSecret(Constants.SecretNames.REFRESH_TOKEN_1)) {
                getMainExecutor().execute(() -> ZevrantServices.switchToLogin(this));
            } else {
                credentialsService.getAuthorization();
                loadRoles();
                startServices();
                initMediaView();
            }
        });
        fragmentManager.addFragmentOnAttachListener((manager, fragment) -> {
            if(fragment.getId() == R.id.loginForm) {
                findViewById(R.id.accountButton).setVisibility(View.INVISIBLE);
                findViewById(R.id.mainNavView).setVisibility(View.INVISIBLE);
            }
        });
    }

    @Inject
    public void setEncryptionService(EncryptionService encryptionService) {
        this.encryptionService = encryptionService;
    }

    @Inject
    public void setCredentialsService(CredentialsService credentialsService) {
        this.credentialsService = credentialsService;
    }

    @Inject
    public void setOauthService(OAuthService oauthService) {
        this.oauthService = oauthService;
    }

    @Inject
    public void setJsonParser(JsonParser jsonParser) {
        this.jsonParser = jsonParser;
    }

    public static Future<String> switchToLogin(Context context) {
        Log.d(LOG_TAG, "Switching to login");
        if(currentFragment == R.id.loginForm || currentFragment == 0) {
            Log.d(LOG_TAG, "not switching to login, login fragment already active");
            return new CompletableFuture<>();
        }
        int fragment = currentFragment;
        currentFragment = R.id.loginForm;

        FragmentTransaction transaction = fragmentManager.beginTransaction();
        fragmentManager.getFragments().forEach(transaction::remove);
        transaction.add(R.id.mainView, new LoginFragment())
                .commit();
        return new CompletableFuture<>();
    }

    public static void navigate(Fragment previousFragment, Fragment fragment) {
        currentFragment = fragment.getId();
        fragmentManager.beginTransaction()
                .remove(previousFragment)
                .add(R.id.mainView, fragment)
        .commit();
    }


    private void initMediaView() {
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.mainView, new MediaViewer())
                .commit();
    }

    private void loadRoles() {
        ThreadManager.execute(() -> {
            oauthService.loadRoles(this, credentialsService.getAuthorization(), credentialsService);

        });
    }

    public Future<Boolean> handleRedirect(Intent intent) {
        CompletableFuture<Boolean> future = new CompletableFuture<>();
        if(credentialsService.hasAuthorization()) {
            future.complete(true);
            return future;
        }
        new Thread(() -> {

            if (intent != null && intent.getData() != null) {
                String body = intent.getData().getQueryParameter("body");
                assert StringUtils.isNotBlank(body) : "redirect body cannot be null";
                String authBody = new String(
                        Base64.getDecoder().decode(
                                intent.getData().getQueryParameter("body")), StandardCharsets.UTF_8);
                AuthBody auth = jsonParser.readValueFromString(authBody, AuthBody.class);
//                try {
//                    assert auth != null;

//                    credentialsService.manageOAuthToken(oauthService.exchangeCode(auth.getCode(), credentialsService), true);
                    oauthService.loadRoles(this, credentialsService.getAuthorization(), credentialsService);
//                } catch (ExecutionException | InterruptedException | CredentialsNotFoundException e) {
//                    e.printStackTrace();
//                    Log.i("failed to exchange authorization code for a token", LOG_TAG);
//                    ACRA.getErrorReporter().handleSilentException(e);
//                }
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

    private void startServices() {
        if(BuildConfig.BUILD_TYPE == "release"
            || BuildConfig.BUILD_TYPE == "develop") {
            Log.d(LOG_TAG, "Starting periodic job");
            Constraints constraints = new Constraints.Builder()
                    .setTriggerContentMaxDelay(1, TimeUnit.SECONDS)
                    .build();
            Data data = new Data.Builder().build();
            JobUtilities.schedulePeriodicJob(this, PhotoBackup.class, constraints, Constants.JobTags.BACKUP_TAG, data);
        } else {
            Log.d(LOG_TAG, "Not starting backup job, only starting for release && develop variants. For testing use the integration tests");
        }
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