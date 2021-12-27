package com.zevrant.services.zevrantandroidapp.services;

import static com.zevrant.services.zevrantandroidapp.utilities.Constants.LOG_TAG;

import android.content.Context;
import android.util.Log;

import com.android.volley.Request;
import com.android.volley.RetryPolicy;
import com.android.volley.VolleyError;
import com.zevrant.services.zevrantandroidapp.R;
import com.zevrant.services.zevrantandroidapp.exceptions.CredentialsNotFoundException;
import com.zevrant.services.zevrantandroidapp.volley.requests.StringRequest;
import com.zevrant.services.zevrantuniversalcommon.rest.backup.request.BackupFileRequest;
import com.zevrant.services.zevrantuniversalcommon.rest.backup.request.CheckExistence;

import org.acra.ACRA;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;

public class BackupService {

    private static String backupUrl;

    public static void init(Context context) {
        backupUrl = context.getString(R.string.backup_base_url);
    }

    public static Future<String> checkExistence(CheckExistence checkExistence) {
        CompletableFuture<String> future = new CompletableFuture<>();
        String requestBody = JsonParser.writeValueAsString(checkExistence);
        StringRequest objectRequest = new StringRequest(Request.Method.POST,
                backupUrl.concat("/file-backup/check-existence"), requestBody,
                DefaultRequestHandlers.getResponseListener(future),
                DefaultRequestHandlers.getErrorResponseListener(future));
        try {
            objectRequest.setOAuthToken(CredentialsService.getAuthorization());
        } catch (CredentialsNotFoundException e) {
            e.printStackTrace();
            ACRA.getErrorReporter().handleSilentException(e);
        }
        RequestQueueService.addToQueue(objectRequest);
        return future;
    }

    public static Future<String> backupFile(BackupFileRequest backupFileRequest) {

        CompletableFuture<String> future = new CompletableFuture<>();
        Log.i(LOG_TAG, "Backing up file to ".concat(backupUrl.concat("/file-backup")));
        Log.i(LOG_TAG, JsonParser.writeValueAsString(backupFileRequest));
        StringRequest request = new StringRequest(Request.Method.PUT,
                backupUrl.concat("/file-backup"),
                JsonParser.writeValueAsString(backupFileRequest),
                DefaultRequestHandlers.getResponseListener(future),
                DefaultRequestHandlers.getErrorResponseListener(future));
        request.setRetryPolicy(new RetryPolicy() {
            @Override
            public int getCurrentTimeout() {
                return 500000;
            }

            @Override
            public int getCurrentRetryCount() {
                return 500000;
            }

            @Override
            public void retry(VolleyError error) {

            }
        });
        try {
            request.setOAuthToken(CredentialsService.getAuthorization());
        } catch (CredentialsNotFoundException e) {
            e.printStackTrace();
            ACRA.getErrorReporter().handleSilentException(e);
        }
        RequestQueueService.addToQueue(request);
        return future;
    }

    public static Future<String> getAlllHashes() {
        CompletableFuture<String> future = new CompletableFuture<>();
        StringRequest request = new StringRequest(Request.Method.GET,
                backupUrl.concat("/file-backup"),
                null,
                DefaultRequestHandlers.getResponseListener(future),
                DefaultRequestHandlers.getErrorResponseListener(future));
        try {
            request.setOAuthToken(CredentialsService.getAuthorization());
        } catch (CredentialsNotFoundException e) {
            e.printStackTrace();
            ACRA.getErrorReporter().handleSilentException(e);
        }
        RequestQueueService.addToQueue(request);
        return future;
    }
}
