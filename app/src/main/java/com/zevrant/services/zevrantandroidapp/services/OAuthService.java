package com.zevrant.services.zevrantandroidapp.services;

import android.content.Context;

import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.toolbox.RequestFuture;
import com.google.android.gms.common.util.MapUtils;
import com.zevrant.services.zevrantandroidapp.R;
import com.zevrant.services.zevrantandroidapp.pojo.OAuthTokenRequest;
import com.zevrant.services.zevrantandroidapp.volley.requests.StringRequest;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;

public class OAuthService {

    private static final Logger logger = LoggerFactory.getLogger(OAuthService.class);

    private static String oauthUrl;

    public static void init(Context context) {
        oauthUrl = context.getString(R.string.oauth_base_url);
    }

    public static void login(String username, String password, Response.Listener<String> responseCallback) {
        RequestFuture<String> future = RequestFuture.newFuture();
        logger.info("ZEV: requesting token from {}", oauthUrl.concat("/oauth/token"));
        OAuthTokenRequest tokenRequest = new OAuthTokenRequest();
        tokenRequest.setClientId(username);
        tokenRequest.setClientSecret(password);
        tokenRequest.setGrantType("client_credentials");
        tokenRequest.setScope("DEFAULT");
        StringRequest request = new StringRequest(Request.Method.POST, oauthUrl.concat("/oauth/token"), JsonParser.writeValueAsString(tokenRequest), responseCallback, DefaultRequestHandlers.errorListener);

        RequestQueueService.addToQueue(request);

    }
}
