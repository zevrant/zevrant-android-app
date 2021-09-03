package com.zevrant.services.zevrantandroidapp.services;

import android.content.Context;

import com.google.android.gms.auth.api.credentials.Credential;
import com.google.android.gms.auth.api.credentials.CredentialRequest;
import com.google.android.gms.auth.api.credentials.Credentials;
import com.google.android.gms.auth.api.credentials.CredentialsClient;
import com.zevrant.services.zevrantandroidapp.R;
import com.zevrant.services.zevrantandroidapp.exceptions.CredentialsNotFoundException;
import com.zevrant.services.zevrantandroidapp.pojo.CredentialWrapper;
import com.zevrant.services.zevrantandroidapp.pojo.OAuthToken;

import org.acra.ACRA;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.util.Observer;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

public class CredentialsService {

    private static OAuthToken oAuthToken;

    private static LocalDateTime expiresAt;

    private static CredentialWrapper credentialWrapper;

    public static void init() {
        credentialWrapper = new CredentialWrapper();
    }

    private static final Logger logger = LoggerFactory.getLogger(CredentialsService.class);

    public void addObserver(Observer observer) {
        credentialWrapper.addObserver(observer);
    }

    public static Credential getCredential() {
        return credentialWrapper.getCredential();
    }

    public static void setCredential(Credential credential) {
        credentialWrapper.setCredential(credential);
    }

    public static String getAuthorization() throws CredentialsNotFoundException {
        LocalDateTime now = LocalDateTime.now();
        if (expiresAt == null
                || expiresAt.isBefore(now)
                || expiresAt.isEqual(now)) {
            try {
                String result = getNewOAuthToken().get();
                if (!result.equals("SUCCESS")) {
                    logger.error("No other states implemented something weird is happening here");
                }
            } catch (ExecutionException | InterruptedException e) {
                logger.error(ExceptionUtils.getStackTrace(e));
                ACRA.getErrorReporter().handleSilentException(e);

            }
        }
        if (oAuthToken.getAccessToken() == null) {
            throw new CredentialsNotFoundException("Oauth Service returned an oauth token without the actual token");
        }
        return oAuthToken.getAccessToken();
    }

    private static Future<String> getNewOAuthToken() throws CredentialsNotFoundException, ExecutionException, InterruptedException {
        CompletableFuture<String> future = new CompletableFuture<>();
        Credential credential = credentialWrapper.getCredential();
        if (credential == null) {
            throw new CredentialsNotFoundException("No credentials for Zevrant Services found");
        }
        Future<String> responseFuture = OAuthService.login(credential.getId(), credential.getPassword());
        oAuthToken = JsonParser.readValueFromString(responseFuture.get(), OAuthToken.class);
        future.complete("SUCCESS");
        return future;
    }

    public static void deleteSmartLockCredentials(Context context) {
        CredentialsClient credentialsClient = Credentials.getClient(context);
        CredentialRequest credentialRequest = new CredentialRequest.Builder()
                .setPasswordLoginSupported(true)
                .setAccountTypes(context.getString(R.string.oauth_base_url))
                .build();

        credentialsClient.request(credentialRequest).addOnCompleteListener((task) -> {
            if(task.isSuccessful()) {
                Credential credential = task.getResult().getCredential();
                if(credential != null) {
                    credentialsClient.delete(credential); //just kind of best effort no error checking needed
                }
            }
        });
    }
}
