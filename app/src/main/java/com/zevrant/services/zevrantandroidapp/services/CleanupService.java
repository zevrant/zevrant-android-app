package com.zevrant.services.zevrantandroidapp.services;

import android.content.Context;
import android.provider.MediaStore;

import com.android.volley.Request;
import com.zevrant.services.zevrantandroidapp.utilities.Constants;
import com.zevrant.services.zevrantandroidapp.volley.requests.StringRequest;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;

import javax.inject.Inject;

public class CleanupService {
    private static final String[] projection = new String[]{
            MediaStore.Images.Media._ID
    };

    private final RequestQueueService requestQueueService;
    private final CredentialsService credentialsService;

    @Inject
    public CleanupService(RequestQueueService requestQueueService,
                           CredentialsService credentialsService) {
        this.requestQueueService = requestQueueService;
        this.credentialsService = credentialsService;
    }

    public Future<String> eraseBackups(String authorization, String backupUrl, Context context) {
        CompletableFuture<String> future = new CompletableFuture<>();
        StringRequest objectRequest = new StringRequest(Request.Method.DELETE,
                backupUrl.concat("/file-backup"),
                Constants.DefaultRequestHandlers.getResponseListener(future),
                Constants.DefaultRequestHandlers.getErrorResponseListener(future, credentialsService, requestQueueService, context));
        objectRequest.setOAuthToken(authorization);
        requestQueueService.addToQueue(objectRequest);
        return future;
    }

}
