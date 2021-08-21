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

public class BackupService {

    private static String backupUrl;
    private static final Logger logger = LoggerFactory.getLogger(BackupService.class);

    public static void init(Context context) {
        backupUrl = context.getResources().getString(R.string.backup_base_url);
    }

    public static void checkExistence(CheckExistence checkExistence, OAuthToken oAuthToken, Response.Listener<String> responseCallback) {
        String requestBody = JsonParser.writeValueAsString(checkExistence);


        StringRequest objectRequest = new StringRequest(Request.Method.POST, backupUrl.concat("/file-backup/check-existence"), requestBody, responseCallback, DefaultRequestHandlers.errorListener);
        objectRequest.setOAuthToken(oAuthToken.getAccessToken());
        RequestQueueService.addToQueue(objectRequest);
    }

    public static void backupFile(BackupFileRequest backupFileRequest, OAuthToken oAuthToken, Response.Listener<String> responseCallback) {
        logger.info(JsonParser.writeValueAsString(backupFileRequest));
        StringRequest request = new StringRequest(Request.Method.PUT,
                backupUrl.concat("/file-backup"),
                JsonParser.writeValueAsString(backupFileRequest),
                responseCallback,
                DefaultRequestHandlers.errorListener);
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
        request.setOAuthToken(oAuthToken.getAccessToken());
        RequestQueueService.addToQueue(request);
    }
}
