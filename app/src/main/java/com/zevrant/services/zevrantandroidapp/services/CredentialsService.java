package com.zevrant.services.zevrantandroidapp.services;

import static com.zevrant.services.zevrantandroidapp.utilities.Constants.LOG_TAG;

import android.util.Log;

import com.zevrant.services.zevrantandroidapp.exceptions.CredentialsNotFoundException;
import com.zevrant.services.zevrantandroidapp.utilities.Constants;
import com.zevrant.services.zevrantuniversalcommon.rest.oauth.response.OAuthToken;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;

public class CredentialsService {

    private static OAuthToken oAuthToken;

    public static boolean hasAuthorization() {
        return oAuthToken != null && StringUtils.isNotBlank(oAuthToken.getAccessToken());
    }

    public static synchronized OAuthToken manageOAuthToken(OAuthToken oAuthToken, boolean write) {
        if (write) {
            if (oAuthToken == null) {
                throw new RuntimeException("No Credentials Found");
            }
            encryptToken(oAuthToken);
            CredentialsService.oAuthToken = oAuthToken;
        } else if (CredentialsService.oAuthToken == null) {
            CredentialsService.oAuthToken = decryptToken();
        }
        return CredentialsService.oAuthToken;
    }

    public static String getAuthorization() throws CredentialsNotFoundException {
        if (oAuthToken == null) {
            if (EncryptionService.hasSecret(Constants.SecretNames.REFRESH_TOKEN_1)) {
                OAuthToken oauthtoken = decryptToken();
                manageOAuthToken(oauthtoken, true);
            } else {
                throw new CredentialsNotFoundException("User must Log in");
            }
        }

        if (isTokenExpired()) {
            getNewOAuthToken();
        }
        if (oAuthToken.getAccessToken() == null) {
            throw new CredentialsNotFoundException("Oauth Service returned an oauth token without the actual token");
        }
        return oAuthToken.getAccessToken();
    }

    private static void getNewOAuthToken() {
        try {
            OAuthToken oAuthToken = OAuthService.refreshToken(manageOAuthToken(null, false));
            manageOAuthToken(oAuthToken, true);
            OAuthService.loadRoles();
            Log.d("Successfuly refreshed token", LOG_TAG);
        } catch (ExecutionException | InterruptedException e) {
            Log.e(LOG_TAG, ExceptionUtils.getStackTrace(e));
            RuntimeException runtimeException = new RuntimeException(e.getMessage());
            runtimeException.setStackTrace(e.getStackTrace());
            throw runtimeException;
        }
    }

    private static boolean isTokenExpired() {
        return LocalDateTime.now().isAfter(manageOAuthToken(null, false).getExpirationDateTime().minusSeconds(60));
    }

    private static void encryptToken(OAuthToken oAuthToken) {
        String[] tokenPieces = oAuthToken.getAccessToken().split("\\.");
        String[] refreshTokenPieces = oAuthToken.getRefreshToken().split("\\.");
        EncryptionService.setSecret(Constants.SecretNames.TOKEN_0, tokenPieces[0]);
        encryptTokenPart("token-sec1-", tokenPieces[1]);
        encryptTokenPart("token-sec2-", tokenPieces[2]);

        EncryptionService.setSecret(Constants.SecretNames.TOKEN_EXPIRATION, oAuthToken.getExpirationDateTime().toString());

        EncryptionService.setSecret(Constants.SecretNames.REFRESH_TOKEN_1, refreshTokenPieces[0]);

        encryptTokenPart("refresh-token-sec1-", refreshTokenPieces[1]);

        EncryptionService.setSecret(Constants.SecretNames.REFRESH_TOKEN_2, refreshTokenPieces[2]);

        EncryptionService.setSecret(Constants.SecretNames.REFRESH_TOKEN_EXPIRATION, oAuthToken.getRefreshExpiresInDateTime().toString());
    }

    private static void encryptTokenPart(String sectionName, String tokenPart) {
        int i = 0;
        int substringIndex;
        for(substringIndex = 0; (substringIndex + 125) <= tokenPart.length(); substringIndex += 125) {
            Log.d(LOG_TAG, "Stored".concat(sectionName.concat(Integer.toString(i))).concat(" with size ").concat(Integer.toString(tokenPart.substring(substringIndex, substringIndex + 125).length())));
            EncryptionService.setSecret(sectionName.concat(Integer.toString(i)), tokenPart.substring(substringIndex, substringIndex + 125));
            i++;
        }
        EncryptionService.setSecret(sectionName.concat(Integer.toString(i)), tokenPart.substring(substringIndex));
        Log.d(LOG_TAG, "Stored ".concat(Integer.toString(i)).concat(" parts for ").concat(sectionName).concat(" with size ".concat(Integer.toString(tokenPart.length()))));
    }

    private static String decryptTokenPart(String sectionName) {
        StringBuilder tokenBuilder = new StringBuilder();
        int i = 0;
        while(EncryptionService.hasSecret(sectionName.concat(Integer.toString(i)))) {
            String secret = EncryptionService.getSecret(sectionName.concat(Integer.toString(i)));
            tokenBuilder.append(secret);
            Log.d(LOG_TAG, "Retrieved".concat(sectionName.concat(Integer.toString(i))).concat(" with size ").concat(Integer.toString(secret.length())));
            i++;
        }
        Log.d(LOG_TAG, "Retrieved ".concat(Integer.toString(i - 1)).concat(" parts for ").concat(sectionName).concat(" with size ".concat(Integer.toString(tokenBuilder.toString().length()))));
        return tokenBuilder.toString();
    }

    private static OAuthToken decryptToken() {
        StringBuilder tokenPieces = new StringBuilder();
        StringBuilder refreshTokenPieces = new StringBuilder();
        tokenPieces.append(EncryptionService.getSecret(Constants.SecretNames.TOKEN_0));
        tokenPieces.append(".");
        tokenPieces.append(decryptTokenPart("token-sec1-"));
        tokenPieces.append(".");
        tokenPieces.append(decryptTokenPart("token-sec2-"));
        refreshTokenPieces.append(EncryptionService.getSecret(Constants.SecretNames.REFRESH_TOKEN_1));
        refreshTokenPieces.append(".");
        refreshTokenPieces.append(decryptTokenPart("refresh-token-sec1-"));
        refreshTokenPieces.append(".");
        refreshTokenPieces.append(EncryptionService.getSecret(Constants.SecretNames.REFRESH_TOKEN_2));
        OAuthToken oAuthToken = new OAuthToken();
        oAuthToken.setAccessToken(tokenPieces.toString());
        oAuthToken.setRefreshToken(refreshTokenPieces.toString());
        String time = EncryptionService.getSecret(Constants.SecretNames.TOKEN_EXPIRATION);
        String refreshTime = EncryptionService.getSecret(Constants.SecretNames.REFRESH_TOKEN_EXPIRATION);
        oAuthToken.setExpiresInDateTime(LocalDateTime.parse(time));
        oAuthToken.setRefreshExpiresInDateTime(LocalDateTime.parse(refreshTime));
        long expiresIn = LocalDateTime.now().toEpochSecond(ZoneId.of("US/Eastern").getRules().getOffset(LocalDateTime.now())) - oAuthToken.getExpirationDateTime().toEpochSecond(ZoneId.of("US/Eastern").getRules().getOffset(LocalDateTime.now()));
        long refreshExpiresIn = LocalDateTime.now().toEpochSecond(ZoneId.of("US/Eastern").getRules().getOffset(LocalDateTime.now())) - oAuthToken.getRefreshExpiresInDateTime().toEpochSecond(ZoneId.of("US/Eastern").getRules().getOffset(LocalDateTime.now()));
        oAuthToken.setExpiresIn(Math.abs(expiresIn));
        oAuthToken.setRefreshExpiresIn(Math.abs(refreshExpiresIn));
        return oAuthToken;
    }

}
