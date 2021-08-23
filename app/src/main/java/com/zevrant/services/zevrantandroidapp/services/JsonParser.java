package com.zevrant.services.zevrantandroidapp.services;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.acra.ACRA;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class JsonParser {

    private static final Logger logger = LoggerFactory.getLogger(JsonParser.class);
    private static final ObjectMapper objectMapper = new ObjectMapper();

    static {
        objectMapper.setVisibility(PropertyAccessor.FIELD, JsonAutoDetect.Visibility.ANY);
    }

    public synchronized static <T> T readValueFromString(String jsonString, Class<T> clazz) {
        try {
            return objectMapper.readValue(jsonString, clazz);
        } catch(JsonProcessingException ex) {
            logger.error("Failed Processing json string into class {}, {} \n {}", clazz, ExceptionUtils.getStackTrace(ex), jsonString);
            ACRA.getErrorReporter().handleSilentException(ex);
        }
        return null;
    }

    public static String writeValueAsString(Object object) {
        try {
            return objectMapper.writeValueAsString(object);
        } catch(JsonProcessingException ex) {
            logger.error("Failed Processing json class {} into string, {}", object.getClass(), ExceptionUtils.getStackTrace(ex));
            ACRA.getErrorReporter().handleSilentException(ex);
        }
        return null;
    }
}
