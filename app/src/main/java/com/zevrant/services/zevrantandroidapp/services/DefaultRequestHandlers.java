package com.zevrant.services.zevrantandroidapp.services;

import androidx.work.Data;

import com.android.volley.Response;
import com.zevrant.services.zevrantandroidapp.exceptions.MethodNotImplementedException;

import org.acra.ACRA;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.CompletableFuture;

public class DefaultRequestHandlers {

    private static final Logger logger = LoggerFactory.getLogger(DefaultRequestHandlers.class);

    public static Response.ErrorListener getErrorResponseListener(CompletableFuture<String> future) {
        return (volleyError) -> {
            Data.Builder failureDataBuilder = new Data.Builder();
            if (volleyError.getMessage() != null) {
                logger.error(volleyError.getMessage());
                failureDataBuilder.putString("message", volleyError.getMessage());
                ACRA.log.e("VolleyError", volleyError.getMessage());
            } else {
                logger.error(ExceptionUtils.getStackTrace(volleyError));
                ACRA.log.e("VolleyError", ExceptionUtils.getStackTrace(volleyError));

            }
            failureDataBuilder.putString("stackTrace", ExceptionUtils.getStackTrace(volleyError));
            if (volleyError.networkResponse != null) {
                logger.error("Response code returned {}", volleyError.networkResponse.statusCode);
                failureDataBuilder.putString("responseCode", String.valueOf(volleyError.networkResponse.statusCode));
                ACRA.log.e("VolleyError", "Response code returned " + volleyError.networkResponse.statusCode);
                if (volleyError.networkResponse.data != null) {
                    logger.error(new String(volleyError.networkResponse.data));
                    ACRA.log.e("VolleyError", new String(volleyError.networkResponse.data));
                }
            }

            ACRA.getErrorReporter().handleSilentException(volleyError);

            if(future != null) {
                future.complete("FAILURE");
            }
        };
    }

    public static Response.ErrorListener getErrorResponseListenerStream(CompletableFuture<InputStream> future) {
        return (volleyError) -> {
            Data.Builder failureDataBuilder = new Data.Builder();
            if (volleyError.getMessage() != null) {
                logger.error(volleyError.getMessage());
                failureDataBuilder.putString("message", volleyError.getMessage());
                ACRA.log.e("VolleyError", volleyError.getMessage());
            } else {
                logger.error(ExceptionUtils.getStackTrace(volleyError));
                ACRA.log.e("VolleyError", ExceptionUtils.getStackTrace(volleyError));

            }
            failureDataBuilder.putString("stackTrace", ExceptionUtils.getStackTrace(volleyError));
            if (volleyError.networkResponse != null) {
                logger.error("Response code returned {}", volleyError.networkResponse.statusCode);
                failureDataBuilder.putString("responseCode", String.valueOf(volleyError.networkResponse.statusCode));
                ACRA.log.e("VolleyError", "Response code returned " + volleyError.networkResponse.statusCode);
                if (volleyError.networkResponse.data != null) {
                    logger.error(new String(volleyError.networkResponse.data));
                    ACRA.log.e("VolleyError", new String(volleyError.networkResponse.data));
                }
            }

            ACRA.getErrorReporter().handleSilentException(volleyError);

            future.complete(new ByteArrayInputStream("FAILURE".getBytes(StandardCharsets.UTF_8)));
        };
    }

    public static Response.Listener<String> getResponseListener(CompletableFuture<String> future) {
        return future::complete;
    }

    private static Object getErrorData(Data data, Class<?> clazz) {
        switch (clazz.getName()) {
            case "java.lang.String":
                return data.toString();
            case "java.io.InputStream":
                return new ByteArrayInputStream(data.toString().getBytes(StandardCharsets.UTF_8));
        }
        throw new MethodNotImplementedException("Type " + clazz.getName() + " is not implemented for getErrorData()");
    }

}
