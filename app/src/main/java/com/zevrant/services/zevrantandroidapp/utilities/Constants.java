package com.zevrant.services.zevrantandroidapp.utilities;

import android.content.Context;
import android.util.Log;

import androidx.work.Data;

import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.zevrant.services.zevrantandroidapp.activities.ZevrantServices;
import com.zevrant.services.zevrantandroidapp.exceptions.MethodNotImplementedException;
import com.zevrant.services.zevrantandroidapp.pojo.ErrorListener;
import com.zevrant.services.zevrantandroidapp.services.CredentialsService;
import com.zevrant.services.zevrantandroidapp.services.RequestQueueService;
import com.zevrant.services.zevrantandroidapp.volley.requests.StringRequest;

import org.acra.ACRA;
import org.apache.commons.lang3.exception.ExceptionUtils;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.CompletableFuture;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Constants {

    public static final String LOG_TAG = "ZevrantServices";

    public static class JobTags {
        public static final String BACKUP_TAG = "BACKUP";
        public static final String UPDATE_TAG = "UPDATE";
    }

    public static class MediaViewerControls {
        public static final int MAX_WIDTH_DP = 175;
        public static final int MAX_HIEGHT_DP = 125;
    }

    public static class SecretNames {
        public static final String TOKEN_0 = "token0";
        public static final String REFRESH_TOKEN_1 = "refreshToken1";
        public static final String REFRESH_TOKEN_2 = "refreshToken2";
        public static final String TOKEN_EXPIRATION = "tokenExpiration";
        public static final String REFRESH_TOKEN_EXPIRATION = "refreshTokenExpiration";
    }

    public enum UserPreference {
        DEFAULT_PAGE_COUNT("6", Pattern.compile("\\d+"));

        private final String value;
        private final Pattern validatorPattern;

        UserPreference(String value, Pattern validatorPattern) {
            this.value = value;
            this.validatorPattern = validatorPattern;
        }

        public String getValue() {
            return value;
        }

        public Matcher getMatcher(String inputString) {
            return validatorPattern.matcher(inputString);
        }
    }

    public static class DefaultRequestHandlers {
        public static ErrorListener<StringRequest> getErrorResponseListener(CompletableFuture<String> future,
                                                                            CredentialsService credentialsService,
                                                                            RequestQueueService requestQueueService,
                                                                            Context context) {
            return new ErrorListener<>(credentialsService, requestQueueService, context) {
                public void onErrorResponse(VolleyError volleyError) {
                    String completeString = "FAILURE: ";
                    Data.Builder failureDataBuilder = new Data.Builder();
                    if (volleyError != null
                            && volleyError.networkResponse != null
                            && volleyError.networkResponse.statusCode == 401) {
                        Log.d(LOG_TAG, "requesting login, request returned 401");
                        context.getMainExecutor().execute(() -> ZevrantServices.switchToLogin(context));
                        return; //We do not need an error report for login/authentication errors
                    }
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
                        if (volleyError.networkResponse.statusCode == 401) {
                            reAuthAndExecuteStringRequest();
                        }
                        if(volleyError.networkResponse.statusCode == 500 && getRequest().getUrl().contains("openid-connect/token")) {
                            Log.d(LOG_TAG, "Requesting user login, keycloak returned an error");
                            context.getMainExecutor().execute(() -> ZevrantServices.switchToLogin(context));
                            future.complete("Login Needed");
                            return;
                        }
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
                }
            };
        }

        public static Response.ErrorListener getErrorResponseListenerStream
                (CompletableFuture<InputStream> future) {
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

        public static Response.Listener<String> getResponseListener
                (CompletableFuture<String> future) {
            return future::complete;
        }

        public static Response.Listener<InputStream> getResponseListenerStream
                (CompletableFuture<InputStream> future) {
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

}
