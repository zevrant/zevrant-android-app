package com.zevrant.services.zevrantandroidapp.services;

import com.android.volley.Response;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DefaultRequestHandlers {

    private static final Logger logger = LoggerFactory.getLogger(DefaultRequestHandlers.class);

    public static final Response.ErrorListener errorListener = (volleyError) -> {
        if (volleyError.getMessage() != null) {
            logger.error(volleyError.getMessage());
        } else {
            logger.error(ExceptionUtils.getStackTrace(volleyError));
        }
        if (volleyError.networkResponse != null) {
            logger.error("Response code returned {}", volleyError.networkResponse.statusCode);
            if (volleyError.networkResponse.data != null) {
                logger.error(new String(volleyError.networkResponse.data));
            }
        }
    };

}
