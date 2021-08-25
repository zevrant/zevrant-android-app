package com.zevrant.services.zevrantandroidapp.services;

import android.content.Context;

import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.RetryPolicy;
import com.android.volley.VolleyError;
import com.zevrant.services.zevrantandroidapp.R;
import com.zevrant.services.zevrantandroidapp.pojo.CheckExistence;
import com.zevrant.services.zevrantandroidapp.pojo.OAuthToken;
import com.zevrant.services.zevrantandroidapp.pojo.BackupFileRequest;
import com.zevrant.services.zevrantandroidapp.volley.requests.StringRequest;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;

public class BackupService {

    private static String backupUrl;
    private static final Logger logger = LoggerFactory.getLogger(BackupService.class);

    public static void init(Context context) {
        backupUrl = context.getString(R.string.backup_base_url);
    }

    public static Future<String> checkExistence(CheckExistence checkExistence, String authorization) {
        CompletableFuture<String> future = new CompletableFuture<>();
        String requestBody = JsonParser.writeValueAsString(checkExistence);
        StringRequest objectRequest = new StringRequest(Request.Method.POST,
                backupUrl.concat("/file-backup/check-existence"), requestBody,
                DefaultRequestHandlers.getResponseListener(future),
                DefaultRequestHandlers.getErrorResponseListener(future));
        objectRequest.setOAuthToken(authorization);
        RequestQueueService.addToQueue(objectRequest);
        return future;
    }

    public static Future<String> backupFile(BackupFileRequest backupFileRequest, String authorization) {
        CompletableFuture<String> future = new CompletableFuture<>();
        logger.info(JsonParser.writeValueAsString(backupFileRequest));
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
            public void retry(VolleyError error) throws VolleyError {

            }
        });
        request.setOAuthToken(authorization);
        RequestQueueService.addToQueue(request);
        return future;
    }
}
