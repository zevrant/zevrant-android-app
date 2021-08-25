package com.zevrant.services.zevrantandroidapp.secrets;

import java.util.HashMap;
import java.util.Map;

public class Secrets {

    private static final Map<String,String> usernamePasswords = new HashMap<>();

    public static String getPassword(String username) {
        return usernamePasswords.get(username);
    }

    public static void setPassword(String username, String password) {
        usernamePasswords.put(username, password);
    }
}
