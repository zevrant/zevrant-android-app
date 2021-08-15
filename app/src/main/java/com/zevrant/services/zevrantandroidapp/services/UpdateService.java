package com.zevrant.services.zevrantandroidapp.services;

import android.content.Context;

import com.android.volley.Request;
import com.android.volley.Response;
import com.zevrant.services.zevrantandroidapp.R;
import com.zevrant.services.zevrantandroidapp.pojo.OAuthToken;
import com.zevrant.services.zevrantandroidapp.volley.requests.InputStreamRequest;
import com.zevrant.services.zevrantandroidapp.volley.requests.StringRequest;

import java.io.InputStream;

public class UpdateService {

    private static String backupUrl;

    public static void init(Context context) {
        backupUrl = context.getResources().getString(R.string.backup_base_url);

    }

    public static void isUpdateAvailable(String version, OAuthToken oAuthToken, Response.Listener<String> responseCallback) {
        StringRequest request = new StringRequest(Request.Method.GET,
                backupUrl.concat("/updates?version=".concat(version)),
                "",
                responseCallback,
                DefaultRequestHandlers.errorListener);
        request.setOAuthToken(oAuthToken.getAccessToken());
        RequestQueueService.addToQueue(request);
    }

    public static void downloadVersion(String version, OAuthToken oAuthToken, Response.Listener<InputStream> responseCallback) {
        InputStreamRequest request = new InputStreamRequest(Request.Method.GET,
                backupUrl.concat("/updates/download?version=".concat(version)),
                responseCallback,
                DefaultRequestHandlers.errorListener);
        request.setOAuthToken(oAuthToken.getAccessToken());
        RequestQueueService.addToQueue(request);
    }
}
