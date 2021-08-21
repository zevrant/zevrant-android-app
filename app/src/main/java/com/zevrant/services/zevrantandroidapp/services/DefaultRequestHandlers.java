package com.zevrant.services.zevrantandroidapp.services;

import com.android.volley.Response;
import com.android.volley.VolleyError;

import org.acra.ACRA;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DefaultRequestHandlers {

    private static final Logger logger = LoggerFactory.getLogger(DefaultRequestHandlers.class);

    public static final Response.ErrorListener errorListener = (volleyError) -> {
        if (volleyError.getMessage() != null) {
            logger.error(volleyError.getMessage());
            ACRA.log.e("VolleyError", volleyError.getMessage());
        } else {
            logger.error(ExceptionUtils.getStackTrace(volleyError));
            ACRA.log.e("VolleyError", ExceptionUtils.getStackTrace(volleyError));
        }
        if (volleyError.networkResponse != null) {
            logger.error("Response code returned {}", volleyError.networkResponse.statusCode);
            ACRA.log.e("VolleyError", "Response code returned " + volleyError.networkResponse.statusCode);
            if (volleyError.networkResponse.data != null) {
                logger.error(new String(volleyError.networkResponse.data));
                ACRA.log.e("VolleyError", new String(volleyError.networkResponse.data));
            }
        }

        ACRA.getErrorReporter().handleSilentException(volleyError);

    };

}
