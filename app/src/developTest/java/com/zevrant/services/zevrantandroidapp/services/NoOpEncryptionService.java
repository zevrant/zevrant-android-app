package com.zevrant.services.zevrantandroidapp.services;

import java.util.HashMap;
import java.util.Map;

/**
 * This class is used to bypass the fact that the Android Keystore API does not work properly when
 * used via Instrumentation tests.
 */
public class NoOpEncryptionService implements EncryptionServiceContract {

    private static Map<String,String> sharedPreferences = new HashMap<>();
    private static String secretName;

    //You cannot access private key from tests only in the application
    public String getSecret(String secretName) {
            return sharedPreferences.get(secretName);
    }

    public void setSecret(String secretName, String secretValue) {
            sharedPreferences.put(secretName, secretValue);
    }

    public boolean hasSecret(String loginUserName) {
        return sharedPreferences.containsKey(loginUserName);
    }
}
