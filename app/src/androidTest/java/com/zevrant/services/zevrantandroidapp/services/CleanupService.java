package com.zevrant.services.zevrantandroidapp.services;

import com.android.volley.Request;
import com.zevrant.services.zevrantandroidapp.R;
import com.zevrant.services.zevrantandroidapp.steps.BasicSteps;
import com.zevrant.services.zevrantandroidapp.volley.requests.StringRequest;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;

public class CleanupService {

    public static Future<String> eraseBackups(String authorization) {
        CompletableFuture<String> future = new CompletableFuture<>();
        StringRequest objectRequest = new StringRequest(Request.Method.DELETE,
                BasicSteps.getTargetContext().getString(R.string.backup_base_url).concat("/file-backup"),
                DefaultRequestHandlers.getResponseListener(future),
                DefaultRequestHandlers.getErrorResponseListener(future));
        objectRequest.setOAuthToken(authorization);
        RequestQueueService.addToQueue(objectRequest);
        return future;
    }

}
