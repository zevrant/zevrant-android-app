package com.zevrant.services.zevrantandroidapp.services;

import static com.zevrant.services.zevrantandroidapp.utilities.Constants.LOG_TAG;

import android.content.Context;
import android.util.Log;

import com.zevrant.services.zevrantandroidapp.activities.ZevrantServices;
import com.zevrant.services.zevrantandroidapp.exceptions.CredentialTimeoutException;
import com.zevrant.services.zevrantandroidapp.exceptions.CredentialsNotFoundException;
import com.zevrant.services.zevrantandroidapp.utilities.Constants;
import com.zevrant.services.zevrantuniversalcommon.rest.oauth.response.OAuthToken;

import org.acra.ACRA;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import javax.inject.Inject;

import dagger.hilt.android.qualifiers.ApplicationContext;

public class CredentialsService {

    private OAuthToken oAuthToken;
    private final EncryptionService encryptionService;
    private final OAuthService oAuthService;
    private final Context context;

    @Inject
    public CredentialsService(EncryptionService encryptionService, OAuthService oAuthService, @ApplicationContext Context context) {
        this.encryptionService = encryptionService;
        this.oAuthService = oAuthService;
        this.context = context;
    }

    public boolean hasAuthorization() {
        return oAuthToken != null && StringUtils.isNotBlank(oAuthToken.getAccessToken());
    }

    public synchronized OAuthToken manageOAuthToken(OAuthToken newOauthToken, boolean write) throws CredentialsNotFoundException {
        if (write) {
            if (newOauthToken == null) {
                throw new RuntimeException("No Credentials Found");
            }
            encryptToken(newOauthToken);
            this.oAuthToken = newOauthToken;
        } else if (this.oAuthToken == null) {
            this.oAuthToken = decryptToken();
        }
        return this.oAuthToken;
    }

    public String getAuthorization() {
        try {
            if (oAuthToken == null) {
                if (encryptionService.hasSecret(Constants.SecretNames.REFRESH_TOKEN_1)) {
                    OAuthToken oauthtoken = decryptToken();
                    manageOAuthToken(oauthtoken, true);
                } else {
                    throw new CredentialsNotFoundException("User must Log in");
                }
            } else {
                if (isTokenExpired() && !getNewOAuthToken(this)) {
                    Log.d(LOG_TAG, "Requesting login, token expired and unable to refresh");
                    context.getMainExecutor().execute( () -> ZevrantServices.switchToLogin(context));
                    LocalDateTime now = LocalDateTime.now();
                    while (isTokenExpired()) {
                        if(LocalDateTime.now().isAfter(now.plusMinutes(1))) {
                            throw new CredentialsNotFoundException("Timed out waiting for credentials");
                        }
                        try {
                            Thread.sleep(5000);
                            Log.d(LOG_TAG, "Waiting for oauth token to be provided");
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                    return getAuthorization();
                }
            }
            return manageOAuthToken(null, false).getAccessToken();
        } catch (CredentialsNotFoundException ex) {
            if(ex.getMessage().equals("Timed out waiting for credentials")) {
                throw new CredentialTimeoutException("Timed out waiting for credentials");
            }
            Log.d(LOG_TAG, "OAuth credentials not found, redirecting to login");
            ex.printStackTrace();
            context.getMainExecutor().execute( () -> ZevrantServices.switchToLogin(context));
            while(this.oAuthToken == null) {
                try {
                    Log.d(LOG_TAG, "Waiting for oauth token to be provided");
                    Thread.sleep(5000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

            }
            return getAuthorization();
        }

    }

    private boolean getNewOAuthToken(CredentialsService credentialsService) throws CredentialsNotFoundException {
        try {
            OAuthToken oAuthToken = oAuthService.refreshToken(manageOAuthToken(null, false));
            if(oAuthToken == null) {
                return false;
            }
            manageOAuthToken(oAuthToken, true);
            oAuthService.loadRoles(context, oAuthToken.getAccessToken(), credentialsService);
            Log.d("Successfuly refreshed token", LOG_TAG);
        } catch (ExecutionException | InterruptedException e) {
            Log.e(LOG_TAG, ExceptionUtils.getStackTrace(e));
            RuntimeException runtimeException = new RuntimeException(e.getMessage());
            runtimeException.setStackTrace(e.getStackTrace());
            throw runtimeException;
        } catch (CredentialsNotFoundException ex) {
            Log.d(LOG_TAG, "OAuth credentials not found, redirecting to login");
            context.getMainExecutor().execute( () -> ZevrantServices.switchToLogin(context));
        }
        return true;
    }

    private boolean isTokenExpired() {
        try {
            LocalDateTime now = LocalDateTime.now();
            LocalDateTime expiration = manageOAuthToken(null, false).getExpirationDateTime();
            boolean isAfter = now.isAfter(expiration);
            Log.d(LOG_TAG, "Token expires at ".concat(expiration.toString()).concat(" and it is currently ").concat(now.toString()).concat(". Is expired? ").concat(String.valueOf(isAfter)));
            return isAfter;
        } catch (CredentialsNotFoundException ex) {
            return true;
        }
    }

    private void encryptToken(OAuthToken oAuthToken) {
        String[] tokenPieces = oAuthToken.getAccessToken().split("\\.");
        String[] refreshTokenPieces = oAuthToken.getRefreshToken().split("\\.");
        encryptionService.setSecret(Constants.SecretNames.TOKEN_0, tokenPieces[0]);
        encryptTokenPart("token-sec1-", tokenPieces[1]);
        encryptTokenPart("token-sec2-", tokenPieces[2]);

        encryptionService.setSecret(Constants.SecretNames.TOKEN_EXPIRATION, oAuthToken.getExpirationDateTime().toString());

        encryptionService.setSecret(Constants.SecretNames.REFRESH_TOKEN_1, refreshTokenPieces[0]);

        encryptTokenPart("refresh-token-sec1-", refreshTokenPieces[1]);

        encryptionService.setSecret(Constants.SecretNames.REFRESH_TOKEN_2, refreshTokenPieces[2]);

        encryptionService.setSecret(Constants.SecretNames.REFRESH_TOKEN_EXPIRATION, oAuthToken.getRefreshExpiresInDateTime().toString());
    }

    private void encryptTokenPart(String sectionName, String tokenPart) {
        int i = 0;
        int substringIndex;
        for (substringIndex = 0; (substringIndex + 125) <= tokenPart.length(); substringIndex += 125) {
            Log.v(LOG_TAG, "Stored".concat(sectionName.concat(Integer.toString(i))).concat(" with size ").concat(Integer.toString(tokenPart.substring(substringIndex, substringIndex + 125).length())));
            encryptionService.setSecret(sectionName.concat(Integer.toString(i)), tokenPart.substring(substringIndex, substringIndex + 125));
            i++;
        }
        encryptionService.setSecret(sectionName.concat(Integer.toString(i)), tokenPart.substring(substringIndex));
        Log.v(LOG_TAG, "Stored ".concat(Integer.toString(i)).concat(" parts for ").concat(sectionName).concat(" with size ".concat(Integer.toString(tokenPart.length()))));
    }

    private String decryptTokenPart(String sectionName) throws CredentialsNotFoundException {
        StringBuilder tokenBuilder = new StringBuilder();
        int i = 0;
        while (encryptionService.hasSecret(sectionName.concat(Integer.toString(i)))) {
            String secret = encryptionService.getSecret(sectionName.concat(Integer.toString(i)));
            tokenBuilder.append(secret);
            Log.v(LOG_TAG, "Retrieved".concat(sectionName.concat(Integer.toString(i))).concat(" with size ").concat(Integer.toString(secret.length())));
            i++;
        }
        Log.v(LOG_TAG, "Retrieved ".concat(Integer.toString(i - 1)).concat(" parts for ").concat(sectionName).concat(" with size ".concat(Integer.toString(tokenBuilder.toString().length()))));
        return tokenBuilder.toString();
    }

    private OAuthToken decryptToken() throws CredentialsNotFoundException {
        StringBuilder tokenPieces = new StringBuilder();
        StringBuilder refreshTokenPieces = new StringBuilder();
        tokenPieces.append(encryptionService.getSecret(Constants.SecretNames.TOKEN_0))
                .append(".")
                .append(decryptTokenPart("token-sec1-"))
                .append(".")
                .append(decryptTokenPart("token-sec2-"));
        refreshTokenPieces.append(encryptionService.getSecret(Constants.SecretNames.REFRESH_TOKEN_1))
                .append(".")
                .append(decryptTokenPart("refresh-token-sec1-"))
                .append(".")
                .append(encryptionService.getSecret(Constants.SecretNames.REFRESH_TOKEN_2));
        OAuthToken oAuthToken = new OAuthToken();
        oAuthToken.setAccessToken(tokenPieces.toString());
        oAuthToken.setRefreshToken(refreshTokenPieces.toString());
        String time = encryptionService.getSecret(Constants.SecretNames.TOKEN_EXPIRATION);
        String refreshTime = encryptionService.getSecret(Constants.SecretNames.REFRESH_TOKEN_EXPIRATION);
        oAuthToken.setExpiresInDateTime(LocalDateTime.parse(time));
        oAuthToken.setRefreshExpiresInDateTime(LocalDateTime.parse(refreshTime));
        ZonedDateTime zonedDateTime = ZonedDateTime.now(ZoneId.of("US/Eastern"));
        long expiresIn = LocalDateTime.now().toEpochSecond(zonedDateTime.getOffset());
        long refreshExpiresIn = LocalDateTime.now().toEpochSecond(zonedDateTime.getOffset());
        oAuthToken.setExpiresIn(Math.abs(expiresIn));
        oAuthToken.setRefreshExpiresIn(Math.abs(refreshExpiresIn));
        return oAuthToken;
    }

    public void clearAuth() {
        encryptionService.deleteSecret(Constants.SecretNames.TOKEN_0);
        deleteTokenPart("token-sec1-");
        deleteTokenPart("token-sec2-");
        encryptionService.deleteSecret(Constants.SecretNames.TOKEN_EXPIRATION);
        encryptionService.deleteSecret(Constants.SecretNames.REFRESH_TOKEN_1);
        deleteTokenPart("refresh-token-sec1-");
        encryptionService.deleteSecret(Constants.SecretNames.REFRESH_TOKEN_2);
        encryptionService.deleteSecret(Constants.SecretNames.REFRESH_TOKEN_EXPIRATION);
        oAuthToken = null;
    }

    public void deleteCurrentToken() {
        oAuthToken = null;
    }

    private void deleteTokenPart(String sectionName) {
        int i = 0;
        while (encryptionService.hasSecret(sectionName.concat(Integer.toString(i)))) {
            encryptionService.deleteSecret(sectionName.concat(Integer.toString(i)));
        }
    }

    public void freshLogin(String username, String password) throws CredentialsNotFoundException {
        OAuthToken oAuthToken = oAuthService.login(username, password);
        manageOAuthToken(oAuthToken, true);
    }
}
