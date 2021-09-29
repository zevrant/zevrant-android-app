package com.zevrant.services.zevrantandroidapp.services;

import static org.acra.ACRA.LOG_TAG;

import android.content.Context;
import android.util.Log;

import com.android.volley.Request;
import com.android.volley.Response;
import com.zevrant.services.zevrantandroidapp.R;
import com.zevrant.services.zevrantandroidapp.pojo.OAuthTokenRequest;
import com.zevrant.services.zevrantandroidapp.volley.requests.StringRequest;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;

public class OAuthService {

    private static String oauthUrl;

    public static void init(Context context) {
        oauthUrl = context.getString(R.string.oauth_base_url);
    }

    public static void login(String username, String password, Response.Listener<String> responseListener) {
        Log.i(LOG_TAG, "requesting token from ".concat(oauthUrl.concat("/oauth/token")));
        OAuthTokenRequest tokenRequest = new OAuthTokenRequest();
        tokenRequest.setClientId(username);
        tokenRequest.setClientSecret(password);
        tokenRequest.setGrantType("client_credentials");
        tokenRequest.setScope("DEFAULT");
        StringRequest request = new StringRequest(Request.Method.POST,
                oauthUrl.concat("/oauth/token"),
                JsonParser.writeValueAsString(tokenRequest),
                responseListener,
                DefaultRequestHandlers.getErrorResponseListener(null));

        RequestQueueService.addToQueue(request);
    }

    public static Future<String> login(String username, String password) {
        if(username.equals("loginUserName")) {
            throw new RuntimeException("BAD USERNAME");
        }
        CompletableFuture<String> future = new CompletableFuture<>();
        Log.i(LOG_TAG, "requesting token from ".concat(oauthUrl.concat("/oauth/token")));
        OAuthTokenRequest tokenRequest = new OAuthTokenRequest();
        tokenRequest.setClientId(username);
        tokenRequest.setClientSecret(password);
        tokenRequest.setGrantType("client_credentials");
        tokenRequest.setScope("DEFAULT");

        Log.v(LOG_TAG, JsonParser.writeValueAsString(tokenRequest));
        StringRequest request = new StringRequest(Request.Method.POST,
                oauthUrl.concat("/oauth/token"),
                JsonParser.writeValueAsString(tokenRequest),
                DefaultRequestHandlers.getResponseListener(future),
                DefaultRequestHandlers.getErrorResponseListener(future));

        RequestQueueService.addToQueue(request);
        return future;
    }

}
