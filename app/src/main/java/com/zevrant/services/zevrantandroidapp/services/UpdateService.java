package com.zevrant.services.zevrantandroidapp.services;

import android.content.Context;

import com.android.volley.Request;
import com.android.volley.Response;
import com.zevrant.services.zevrantandroidapp.R;
import com.zevrant.services.zevrantandroidapp.volley.requests.InputStreamRequest;
import com.zevrant.services.zevrantandroidapp.volley.requests.StringRequest;

import java.io.InputStream;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;

public class UpdateService {

    private static String backupUrl;

    public static void init(Context context) {
        backupUrl = context.getString(R.string.backup_base_url);

    }

    public static Future<String> isUpdateAvailable(String version, String authorization) {
        CompletableFuture<String> future = new CompletableFuture<>();
        StringRequest request = new StringRequest(Request.Method.GET,
                backupUrl.concat("/updates?version=".concat(version)),
                "",
                DefaultRequestHandlers.getResponseListener(future),
                DefaultRequestHandlers.getErrorResponseListener(future));
        request.setOAuthToken(authorization);
        RequestQueueService.addToQueue(request);
        return future;
    }

    public static Future<InputStream> downloadVersion(String version, String authorization) {
        CompletableFuture<InputStream> future = new CompletableFuture<>();
        InputStreamRequest request = new InputStreamRequest(Request.Method.GET,
                backupUrl.concat("/updates/download?version=".concat(version)),
                future::complete,
                DefaultRequestHandlers.getErrorResponseListenerStream(future));
        request.setOAuthToken(authorization);
        RequestQueueService.addToQueue(request);
        return future;
    }
}
