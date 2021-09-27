package com.zevrant.services.zevrantandroidapp.services;

import static org.acra.ACRA.LOG_TAG;

import android.util.Log;

import com.zevrant.services.zevrantandroidapp.exceptions.CredentialsNotFoundException;
import com.zevrant.services.zevrantandroidapp.pojo.Credential;
import com.zevrant.services.zevrantandroidapp.pojo.OAuthToken;
import com.zevrant.services.zevrantandroidapp.utilities.Constants;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;

import java.time.LocalDateTime;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

public class CredentialsService {

    private static OAuthToken oAuthToken;
    private static LocalDateTime expiresAt;

    public static boolean hasAuthorization() {
        return oAuthToken != null && StringUtils.isNotBlank(oAuthToken.getAccessToken());
    }

    private static Credential getCredential() {
        String username = EncryptionService.getSecret(Constants.SecretNames.LOGIN_USER_NAME);
        String password = EncryptionService.getSecret(Constants.SecretNames.LOGIN_PASSWORD);
        Log.d(LOG_TAG, "Login Username: ".concat(username));
        return new Credential(username, password);
    }

    public static void setOAuthToken(OAuthToken oAuthToken) {
        CredentialsService.oAuthToken = oAuthToken;
    }

    public synchronized static String getAuthorization() throws CredentialsNotFoundException {
        if (oAuthToken == null) {
            getNewOAuthToken();
        }
        LocalDateTime now = LocalDateTime.now();
        if (expiresAt == null
                || expiresAt.isBefore(now)
                || expiresAt.isEqual(now)) {
            getNewOAuthToken();
        }
        if (oAuthToken.getAccessToken() == null) {
            throw new CredentialsNotFoundException("Oauth Service returned an oauth token without the actual token");
        }
        return oAuthToken.getAccessToken();
    }

    private static void getNewOAuthToken() {
        String response = null;
        try {
            Credential credential = getCredential();
            if(StringUtils.isBlank(credential.getUsername()) || StringUtils.isBlank(credential.getPassword())) {
                throw new CredentialsNotFoundException("No credentialss could be found please log in.");
            }
            Future<String> responseFuture = OAuthService.login(credential.getUsername(), credential.getPassword());
            response = responseFuture.get();
            oAuthToken = JsonParser.readValueFromString(response, OAuthToken.class);
            assert oAuthToken != null;
            assert oAuthToken.getExpiresIn() > 0;
            assert StringUtils.isNotBlank(oAuthToken.getAccessToken());
            expiresAt = LocalDateTime.now().plusSeconds(oAuthToken.getExpiresIn() - 2);
        } catch (ExecutionException | InterruptedException | CredentialsNotFoundException e) {
            Log.e(LOG_TAG, ExceptionUtils.getStackTrace(e));
            RuntimeException runtimeException = new RuntimeException(e.getMessage());
            runtimeException.setStackTrace(e.getStackTrace());
            throw runtimeException;
        } catch (AssertionError ex) {
            assert response != null;
            throw new RuntimeException("Request for token Failed, response body is: \n ".concat(response));
        }
    }

}
