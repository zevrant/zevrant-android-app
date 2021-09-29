package com.zevrant.services.zevrantandroidapp.services;

import static com.zevrant.services.zevrantandroidapp.utilities.Constants.SecretNames.LOGIN_USER_NAME;

import static org.acra.ACRA.LOG_TAG;

import android.content.Context;
import android.util.Log;

public class EncryptionService {

    private static EncryptionServiceContract encryptionService;

    public static void init(EncryptionServiceContract encryptionService) {
        if(EncryptionService.encryptionService == null) {
            EncryptionService.encryptionService = encryptionService;
            encryptionService.createKeys();
        }

    }

    public static String getSecret(String secretName) {
        return encryptionService.getSecret(secretName);
    }

    public static void setSecret(String secretName, String secretValue) {
        if(secretValue.equals(LOGIN_USER_NAME)) {
            throw new RuntimeException("Bad Credential save");
        }
        encryptionService.setSecret(secretName, secretValue);
    }

    public static boolean hasSecret(String loginUserName) {
        return encryptionService.hasSecret(loginUserName);
    }
}
