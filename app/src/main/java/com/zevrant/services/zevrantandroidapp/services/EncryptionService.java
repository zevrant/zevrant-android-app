package com.zevrant.services.zevrantandroidapp.services;

import org.apache.commons.lang3.StringUtils;

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
        encryptionService.setSecret(secretName, secretValue);
    }

    public static boolean hasSecret(String loginUserName) {
        return StringUtils.isNotBlank(loginUserName)
                && encryptionService.hasSecret(loginUserName);
    }

    public static boolean isInitialized() {
        return encryptionService != null;
    }
}
