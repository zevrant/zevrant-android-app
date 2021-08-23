package com.zevrant.services.zevrantandroidapp.services;

import com.google.android.gms.auth.api.credentials.Credential;
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
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public class CredentialsService {

    private static OAuthToken oAuthToken;

    private static LocalDateTime expiresAt;

    private static CredentialWrapper credentialWrapper;

    public static void init() {
        credentialWrapper = new CredentialWrapper();
    }

    private static final Logger logger = LoggerFactory.getLogger(CredentialsService.class);

    public void setCredentials(Credential credential) {
        credentialWrapper.setCredential(credential);
    }

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
                || expiresAt.isAfter(now)
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
            throw new CredentialsNotFoundException("Oauth Service reutrned an oauth token without the actual token");
        }
        return oAuthToken.getAccessToken();
    }

    private static Future<String> getNewOAuthToken() {
        CompletableFuture<String> future = new CompletableFuture<>();
        Executors.newCachedThreadPool().submit(() -> {
            Credential credential = credentialWrapper.getCredential();
            if (credential == null) {
                throw new CredentialsNotFoundException("No credentials for Zevrant Services found");
            }
            OAuthService.login(credential.getId(), credential.getPassword(), response -> {
                oAuthToken = JsonParser.readValueFromString(response, OAuthToken.class);
                future.complete("SUCCESS");
            });
            return null;
        });

        return future;
    }
}
