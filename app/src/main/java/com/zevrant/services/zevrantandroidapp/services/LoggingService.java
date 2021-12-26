package com.zevrant.services.zevrantandroidapp.services;

import android.util.Log;

public class LoggingService {

    public static void error(String message, Class<?> clazz) {
        Log.e(clazz.getName(), message);
    }

    public static void debug(String message, Class<?> clazz) {
        Log.d(clazz.getName(), message);
    }

    public static void info(String message, Class<?> clazz) {
        Log.i(clazz.getName(), message);
    }
}
