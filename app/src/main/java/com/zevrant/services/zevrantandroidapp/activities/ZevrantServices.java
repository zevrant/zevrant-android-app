package com.zevrant.services.zevrantandroidapp.activities;

import static com.zevrant.services.zevrantandroidapp.utilities.Constants.LOG_TAG;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.view.autofill.AutofillManager;
import android.webkit.WebResourceRequest;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.work.Constraints;
import androidx.work.Data;

import com.google.android.material.bottomappbar.BottomAppBar;
import com.zevrant.services.zevrantandroidapp.R;
import com.zevrant.services.zevrantandroidapp.exceptions.CredentialsNotFoundException;
import com.zevrant.services.zevrantandroidapp.pojo.AuthBody;
import com.zevrant.services.zevrantandroidapp.services.BackupService;
import com.zevrant.services.zevrantandroidapp.services.CredentialsService;
import com.zevrant.services.zevrantandroidapp.services.EncryptionService;
import com.zevrant.services.zevrantandroidapp.services.EncryptionServiceImpl;
import com.zevrant.services.zevrantandroidapp.services.JsonParser;
import com.zevrant.services.zevrantandroidapp.services.OAuthService;
import com.zevrant.services.zevrantandroidapp.services.RequestQueueService;
import com.zevrant.services.zevrantandroidapp.services.UpdateService;
import com.zevrant.services.zevrantandroidapp.utilities.Constants;

import org.acra.ACRA;
import org.apache.commons.lang3.exception.ExceptionUtils;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

public class ZevrantServices extends Activity {

    private Button loginButton;
    private BottomAppBar bottomAppBar;
    private WebView loginWebView;

    public void overrideWebClient() {
//        if(BuildConfig.BUILD_TYPE.equals("developTest")) {
        loginWebView.clearCache(true);
        loginWebView.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest url) {
                getIntent().setData(url.getUrl());
                return false;
            }

        });
//        }
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        initServices(getApplicationContext());
        getApplicationContext().getSystemService(AutofillManager.class)
                .disableAutofillServices();

        handleRedirect(getIntent());
        setContentView(R.layout.activity_main);
        initViewGlue();
        checkPermissions();
        if (EncryptionService.hasSecret(Constants.SecretNames.REFRESH_TOKEN_1)) {
            loadRoles();
            startServices();
//            Intent intent = new Intent(this, MediaViewer.class);
//            startActivity(intent);
        }
    }

    private void loadRoles() {
        new Thread(OAuthService::loadRoles);
    }

    public void handleRedirect(Intent intent) {
        new Thread(() -> {
            if (intent != null && intent.getData() != null) {
                String authBody = new String(
                        Base64.getDecoder().decode(
                                intent.getData().getQueryParameter("body")), StandardCharsets.UTF_8);
                AuthBody auth = JsonParser.readValueFromString(authBody, AuthBody.class);
                try {
                    assert auth != null;

                    CredentialsService.manageOAuthToken(OAuthService.exchangeCode(auth.getCode()), true);
                    CredentialsService.getAuthorization();
                } catch (ExecutionException | InterruptedException | CredentialsNotFoundException e) {
                    e.printStackTrace();
                    Log.i("failed to exchange authorization code for a token", LOG_TAG);
                    ACRA.getErrorReporter().handleSilentException(e);
                }
            } else {
                Log.d("No Data in Intent", LOG_TAG);
            }
        }).start();
    }

    private void checkPermissions() {
        if (ContextCompat.checkSelfPermission(
                getApplicationContext(), Manifest.permission.READ_EXTERNAL_STORAGE) !=
                PackageManager.PERMISSION_GRANTED) {
            // You can use the API that requires the permission.
            // The registered ActivityResultCallback gets the result of this request.
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 0);
        }
    }

    @SuppressLint("SetJavaScriptEnabled")
    private void initViewGlue() {
        bottomAppBar = findViewById(R.id.mainActivityBottomAppBar);
        loginButton = findViewById(R.id.loginButton);
        loginWebView = findViewById(R.id.login_web_view);

//        if(BuildConfig.BUILD_TYPE.equals("developTest")) {
        loginWebView.getSettings().setJavaScriptEnabled(true);
//        }
        loginButton.setOnClickListener((view) -> {
            loginWebView.loadUrl(new Uri.Builder()
                    .scheme("https")
                    .encodedAuthority("develop.zevrant-services.com")
                    .path("/auth/realms/zevrant-services/protocol/openid-connect/auth")
                    .appendQueryParameter("client_id", "android")
                    .appendQueryParameter("scope", "openid")
                    .appendQueryParameter("response_type", "code")
                    .appendQueryParameter("redirect_uri", "https://android.develop.zevrant-services.com")
                    .build().toString());
            loginWebView.setVisibility(View.VISIBLE);
//            startActivity(new Intent(Intent.ACTION_VIEW, new Uri.Builder()
//                    .scheme("https")
//                    .encodedAuthority("develop.zevrant-services.com")
//                    .path("/auth/realms/zevrant-services/protocol/openid-connect/auth")
//                    .appendQueryParameter("client_id", "android")
//                    .appendQueryParameter("scope", "openid")
//                    .appendQueryParameter("response_type", "code")
//                    .appendQueryParameter("redirect_uri", "https://android.develop.zevrant-services.com")
//                    .build()));
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
//        JobUtilities.schedulePeriodicJob(getApplicationContext(), PhotoBackup.class, constraints, Constants.JobTags.BACKUP_TAG, data);

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