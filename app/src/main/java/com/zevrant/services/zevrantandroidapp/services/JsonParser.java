package com.zevrant.services.zevrantandroidapp.services;

import static com.zevrant.services.zevrantandroidapp.utilities.Constants.LOG_TAG;

import android.util.Log;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.acra.ACRA;
import org.apache.commons.lang3.exception.ExceptionUtils;

public class JsonParser {

    private static final ObjectMapper objectMapper = new ObjectMapper();

    static {
        objectMapper.setVisibility(PropertyAccessor.FIELD, JsonAutoDetect.Visibility.ANY);
    }

    public synchronized static <T> T readValueFromString(String jsonString, Class<T> clazz) {
        try {
            return objectMapper.readValue(jsonString, clazz);
        } catch (JsonProcessingException ex) {
            Log.e(LOG_TAG, "Failed Processing json string into class ".concat(String.valueOf(clazz)).concat(", ").concat(ExceptionUtils.getStackTrace(ex)).concat(" \n ".concat(jsonString)));
            ACRA.getErrorReporter().handleSilentException(ex);
        }
        return null;
    }

    public synchronized static <T> T readValueFromString(String jsonString, TypeReference<T> typeReference) {
        try {
            return objectMapper.readValue(jsonString, typeReference);
        } catch (JsonProcessingException ex) {
            Log.e(LOG_TAG, "Failed Processing json string into class ".concat(String.valueOf(typeReference)).concat(", ").concat(ExceptionUtils.getStackTrace(ex)).concat(" \n ".concat(jsonString)));
            ACRA.getErrorReporter().handleSilentException(ex);
        }
        return null;
    }

    public static String writeValueAsString(Object object) {
        try {
            return objectMapper.writeValueAsString(object);
        } catch (JsonProcessingException ex) {
            Log.e(LOG_TAG, "Failed Processing json class".concat(String.valueOf(object.getClass())).concat(" into string, ").concat(ExceptionUtils.getStackTrace(ex)));
            ACRA.getErrorReporter().handleSilentException(ex);
        }
        return null;
    }
}
