package com.zevrant.services.zevrantandroidapp.services;

import com.zevrant.services.zevrantandroidapp.exceptions.CredentialsNotFoundException;
import com.zevrant.services.zevrantandroidapp.pojo.Credential;
import com.zevrant.services.zevrantandroidapp.pojo.OAuthToken;
import com.zevrant.services.zevrantandroidapp.utilities.Constants;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

public class CredentialsService {

    private static OAuthToken oAuthToken;
    private static LocalDateTime expiresAt;

    private static final Logger logger = LoggerFactory.getLogger(CredentialsService.class);

    public static boolean hasAuthorization() {
        return oAuthToken != null && StringUtils.isNotBlank(oAuthToken.getAccessToken());
    }

    private static Credential getCredential() {
        String username = EncryptionService.getSecret(Constants.SecretNames.LOGIN_USER_NAME);
        String password = EncryptionService.getSecret(Constants.SecretNames.LOGIN_PASSWORD);
        return new Credential(username, password);
    }

    @Deprecated
    public synchronized static String getAuthorization() throws CredentialsNotFoundException {
        if(oAuthToken == null) {
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
        try {
            CompletableFuture<String> future = new CompletableFuture<>();
            Credential credential = getCredential();
            Future<String> responseFuture = OAuthService.login(credential.getUsername(), credential.getPassword());
            oAuthToken = JsonParser.readValueFromString(responseFuture.get(), OAuthToken.class);
            expiresAt = LocalDateTime.now().plusSeconds(oAuthToken.getExpiresIn() - 2);
        } catch (ExecutionException | InterruptedException e) {
            logger.error(ExceptionUtils.getStackTrace(e));
            RuntimeException runtimeException = new RuntimeException(e.getMessage());
            runtimeException.setStackTrace(e.getStackTrace());
            throw runtimeException;
        }
    }

}
