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
            OAuthService.loadRoles();
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
        EncryptionService.setSecret(Constants.SecretNames.TOKEN_1, tokenPieces[0]);
        EncryptionService.setSecret(Constants.SecretNames.TOKEN_2, tokenPieces[1].substring(0, 125));
        EncryptionService.setSecret(Constants.SecretNames.TOKEN_3, tokenPieces[1].substring(125, 250));
        EncryptionService.setSecret(Constants.SecretNames.TOKEN_4, tokenPieces[1].substring(250, 375));
        EncryptionService.setSecret(Constants.SecretNames.TOKEN_5, tokenPieces[1].substring(375, 500));
        EncryptionService.setSecret(Constants.SecretNames.TOKEN_6, tokenPieces[1].substring(500, 625));
        EncryptionService.setSecret(Constants.SecretNames.TOKEN_7, tokenPieces[1].substring(625, 750));
        EncryptionService.setSecret(Constants.SecretNames.TOKEN_8, tokenPieces[1].substring(750));
        EncryptionService.setSecret(Constants.SecretNames.TOKEN_11, tokenPieces[2].substring(0, 125));
        EncryptionService.setSecret(Constants.SecretNames.TOKEN_12, tokenPieces[2].substring(125, 250));
        EncryptionService.setSecret(Constants.SecretNames.TOKEN_13, tokenPieces[2].substring(250));
        EncryptionService.setSecret(Constants.SecretNames.TOKEN_EXPIRATION, oAuthToken.getExpirationDateTime().toString());
        EncryptionService.setSecret(Constants.SecretNames.REFRESH_TOKEN_1, refreshTokenPieces[0]);
        EncryptionService.setSecret(Constants.SecretNames.REFRESH_TOKEN_2, refreshTokenPieces[1].substring(0, 125));
        EncryptionService.setSecret(Constants.SecretNames.REFRESH_TOKEN_3, refreshTokenPieces[1].substring(125, 250));
        EncryptionService.setSecret(Constants.SecretNames.REFRESH_TOKEN_4, refreshTokenPieces[1].substring(250, 375));
        EncryptionService.setSecret(Constants.SecretNames.REFRESH_TOKEN_5, refreshTokenPieces[1].substring(375, 500));
        EncryptionService.setSecret(Constants.SecretNames.REFRESH_TOKEN_6, refreshTokenPieces[1].substring(500));
        EncryptionService.setSecret(Constants.SecretNames.REFRESH_TOKEN_7, refreshTokenPieces[2]);
    }

    private static OAuthToken decryptToken() {
        List<String> tokenPieces = new ArrayList<>();
        List<String> refreshTokenPieces = new ArrayList<>();
        tokenPieces.add(EncryptionService.getSecret(Constants.SecretNames.TOKEN_1));
        tokenPieces.add(".");
        tokenPieces.add(EncryptionService.getSecret(Constants.SecretNames.TOKEN_2));
        tokenPieces.add(EncryptionService.getSecret(Constants.SecretNames.TOKEN_3));
        tokenPieces.add(EncryptionService.getSecret(Constants.SecretNames.TOKEN_4));
        tokenPieces.add(EncryptionService.getSecret(Constants.SecretNames.TOKEN_5));
        tokenPieces.add(EncryptionService.getSecret(Constants.SecretNames.TOKEN_6));
        tokenPieces.add(EncryptionService.getSecret(Constants.SecretNames.TOKEN_7));
        tokenPieces.add(EncryptionService.getSecret(Constants.SecretNames.TOKEN_8));
        tokenPieces.add(".");
        tokenPieces.add(EncryptionService.getSecret(Constants.SecretNames.TOKEN_11));
        tokenPieces.add(EncryptionService.getSecret(Constants.SecretNames.TOKEN_12));
        tokenPieces.add(EncryptionService.getSecret(Constants.SecretNames.TOKEN_13));
        refreshTokenPieces.add(EncryptionService.getSecret(Constants.SecretNames.REFRESH_TOKEN_1));
        refreshTokenPieces.add(".");
        refreshTokenPieces.add(EncryptionService.getSecret(Constants.SecretNames.REFRESH_TOKEN_2));
        refreshTokenPieces.add(EncryptionService.getSecret(Constants.SecretNames.REFRESH_TOKEN_3));
        refreshTokenPieces.add(EncryptionService.getSecret(Constants.SecretNames.REFRESH_TOKEN_4));
        refreshTokenPieces.add(EncryptionService.getSecret(Constants.SecretNames.REFRESH_TOKEN_5));
        refreshTokenPieces.add(EncryptionService.getSecret(Constants.SecretNames.REFRESH_TOKEN_6));
        refreshTokenPieces.add(".");
        refreshTokenPieces.add(EncryptionService.getSecret(Constants.SecretNames.REFRESH_TOKEN_7));
        OAuthToken oAuthToken = new OAuthToken();
        oAuthToken.setAccessToken(tokenPieces.stream().reduce((accumulator, combiner) -> accumulator = accumulator.concat(combiner)).orElseThrow(() -> new RuntimeException("failed to join access token")));
        oAuthToken.setRefreshToken(refreshTokenPieces.stream().reduce((accumulator, combiner) -> accumulator = accumulator.concat(combiner)).orElseThrow(() -> new RuntimeException("failed to join refresh token")));
        String time = EncryptionService.getSecret(Constants.SecretNames.TOKEN_EXPIRATION);
        oAuthToken.setExpiresInDateTime(LocalDateTime.parse(time));
        long expiresIn = LocalDateTime.now().toEpochSecond(ZoneId.of("US/Eastern").getRules().getOffset(LocalDateTime.now())) - oAuthToken.getExpirationDateTime().toEpochSecond(ZoneId.of("US/Eastern").getRules().getOffset(LocalDateTime.now()));
        oAuthToken.setExpiresIn(expiresIn);
        return oAuthToken;
    }

}
