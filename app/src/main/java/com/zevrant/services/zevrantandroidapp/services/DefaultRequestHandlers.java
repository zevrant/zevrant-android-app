package com.zevrant.services.zevrantandroidapp.services;

import static org.acra.ACRA.LOG_TAG;

import android.util.Log;

import androidx.work.Data;

import com.android.volley.Response;
import com.zevrant.services.zevrantandroidapp.exceptions.MethodNotImplementedException;

import org.acra.ACRA;
import org.apache.commons.lang3.exception.ExceptionUtils;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.CompletableFuture;

public class DefaultRequestHandlers {


    public static Response.ErrorListener getErrorResponseListener(CompletableFuture<String> future) {
        return (volleyError) -> {
            String completeString = "FAILURE: ";
            Data.Builder failureDataBuilder = new Data.Builder();
            if (volleyError.getMessage() != null) {
                Log.e(LOG_TAG, volleyError.getMessage());
                failureDataBuilder.putString("message", volleyError.getMessage());
                ACRA.log.e("VolleyError", volleyError.getMessage());
                completeString = completeString.concat(volleyError.getMessage());
            } else {
                Log.e(LOG_TAG, ExceptionUtils.getStackTrace(volleyError));
                ACRA.log.e("VolleyError", ExceptionUtils.getStackTrace(volleyError));
                completeString = completeString.concat(ExceptionUtils.getStackTrace(volleyError));
            }
            failureDataBuilder.putString("stackTrace", ExceptionUtils.getStackTrace(volleyError));
            if (volleyError.networkResponse != null) {
                Log.e(LOG_TAG, "Response code returned ".concat(String.valueOf(volleyError.networkResponse.statusCode)));
                failureDataBuilder.putString("responseCode", String.valueOf(volleyError.networkResponse.statusCode));
                ACRA.log.e("VolleyError", "Response code returned " + volleyError.networkResponse.statusCode);
                if (volleyError.networkResponse.data != null) {
                    Log.e(LOG_TAG, new String(volleyError.networkResponse.data));
                    ACRA.log.e("VolleyError", new String(volleyError.networkResponse.data));
                    completeString = completeString.concat(" ").concat(new String(volleyError.networkResponse.data));
                }
            }

            ACRA.getErrorReporter().handleSilentException(volleyError);

            if (future != null) {
                future.complete(completeString);
            }
        };
    }

    public static Response.ErrorListener getErrorResponseListenerStream(CompletableFuture<InputStream> future) {
        return (volleyError) -> {
            Data.Builder failureDataBuilder = new Data.Builder();
            if (volleyError.getMessage() != null) {
                Log.e(LOG_TAG, volleyError.getMessage());
                failureDataBuilder.putString("message", volleyError.getMessage());
                ACRA.log.e("VolleyError", volleyError.getMessage());
            } else {
                Log.e(LOG_TAG, ExceptionUtils.getStackTrace(volleyError));
                ACRA.log.e("VolleyError", ExceptionUtils.getStackTrace(volleyError));

            }
            failureDataBuilder.putString("stackTrace", ExceptionUtils.getStackTrace(volleyError));
            if (volleyError.networkResponse != null) {
                Log.e(LOG_TAG, "Response code returned ".concat(String.valueOf(volleyError.networkResponse.statusCode)));
                failureDataBuilder.putString("responseCode", String.valueOf(volleyError.networkResponse.statusCode));
                ACRA.log.e("VolleyError", "Response code returned " + volleyError.networkResponse.statusCode);
                if (volleyError.networkResponse.data != null) {
                    Log.e(LOG_TAG, new String(volleyError.networkResponse.data));
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
