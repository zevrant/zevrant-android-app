package com.zevrant.services.zevrantandroidapp.services;

import android.content.Context;

import com.android.volley.Request;
import com.fasterxml.jackson.core.type.TypeReference;
import com.zevrant.services.zevrantandroidapp.R;
import com.zevrant.services.zevrantandroidapp.exceptions.CredentialsNotFoundException;
import com.zevrant.services.zevrantandroidapp.volley.requests.StringRequest;
import com.zevrant.services.zevrantuniversalcommon.contants.Roles;
import com.zevrant.services.zevrantuniversalcommon.rest.oauth.request.CodeExchangeRequest;
import com.zevrant.services.zevrantuniversalcommon.rest.oauth.request.TokenRefreshRequest;
import com.zevrant.services.zevrantuniversalcommon.rest.oauth.response.OAuthToken;

import org.apache.commons.lang3.StringUtils;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

public class OAuthService {

    private static String keycloakUrl;
    private static String oauthUrl;
    private static String redirectUri;

    private static List<String> roles;

    public static void init(Context context) {
        keycloakUrl = context.getString(R.string.keycloak_url);
        oauthUrl = context.getString(R.string.oauth_base_url);
        redirectUri = context.getString(R.string.redirect_uri);
        roles = new ArrayList<>();
    }

    public static boolean canI(Roles action) {
        if (roles.isEmpty()) {
            loadRoles();
        }
        return roles.contains(action.name().toLowerCase());
    }

    public static OAuthToken exchangeCode(String code) throws ExecutionException, InterruptedException {
        CodeExchangeRequest codeExchangeRequest = new CodeExchangeRequest(code, redirectUri);
        CompletableFuture<String> future = new CompletableFuture<>();
        StringRequest request = new StringRequest(Request.Method.POST,
                keycloakUrl,
                JsonParser.writeValueAsString(codeExchangeRequest),
                DefaultRequestHandlers.getResponseListener(future),
                DefaultRequestHandlers.getErrorResponseListener(future));
        request.setContentType("application/json");
        RequestQueueService.addToQueue(request);
        String response = future.get();
        assert StringUtils.isNotBlank(response);
        OAuthToken token = JsonParser.readValueFromString(response, OAuthToken.class);
        assert token != null : "token parsed from string was null";
        token.setExpiresInDateTime(LocalDateTime.ofEpochSecond(LocalDateTime.now().toEpochSecond(ZoneId.of("US/Eastern").getRules().getOffset(LocalDateTime.now())) - token.getExpiresIn(), 0, ZoneId.of("US/Eastern").getRules().getOffset(LocalDateTime.now())));
        token.setRefreshExpiresInDateTime(LocalDateTime.ofEpochSecond(LocalDateTime.now().toEpochSecond(ZoneId.of("US/Eastern").getRules().getOffset(LocalDateTime.now())) - token.getRefreshExpiresIn(), 0, ZoneId.of("US/Eastern").getRules().getOffset(LocalDateTime.now())));
        return token;
    }

    public static OAuthToken refreshToken(OAuthToken oAuthToken) throws ExecutionException, InterruptedException {
        TokenRefreshRequest refreshRequest = new TokenRefreshRequest(oAuthToken.getRefreshToken());
        CompletableFuture<String> future = new CompletableFuture<>();
        StringRequest request = new StringRequest(Request.Method.POST,
                keycloakUrl,
                JsonParser.writeValueAsString(refreshRequest),
                DefaultRequestHandlers.getResponseListener(future),
                DefaultRequestHandlers.getErrorResponseListener(future)
        );
        RequestQueueService.addToQueue(request);
        String response = future.get();
        OAuthToken token = JsonParser.readValueFromString(response, OAuthToken.class);
        assert token != null;
        token.setExpiresInDateTime(LocalDateTime.ofEpochSecond(LocalDateTime.now().toEpochSecond(ZoneId.of("US/Eastern").getRules().getOffset(LocalDateTime.now())) + token.getExpiresIn(), 0, ZoneId.of("US/Eastern").getRules().getOffset(LocalDateTime.now())));
        token.setRefreshExpiresInDateTime(LocalDateTime.ofEpochSecond(LocalDateTime.now().toEpochSecond(ZoneId.of("US/Eastern").getRules().getOffset(LocalDateTime.now())) - token.getRefreshExpiresIn(), 0, ZoneId.of("US/Eastern").getRules().getOffset(LocalDateTime.now())));
        return token;
    }

    public static void loadRoles() {
        CompletableFuture<String> future = new CompletableFuture<>();
        StringRequest request = new StringRequest(Request.Method.GET,
                oauthUrl.concat("/users/me/roles"),
                null,
                DefaultRequestHandlers.getResponseListener(future),
                DefaultRequestHandlers.getErrorResponseListener(future)
        );
        try {
            request.setOAuthToken(CredentialsService.getAuthorization());
            RequestQueueService.addToQueue(request);
            roles = JsonParser.readValueFromString(future.get(), new TypeReference<>() {
            });
        } catch (InterruptedException | CredentialsNotFoundException | ExecutionException interruptedException) {
            interruptedException.printStackTrace();
        }

    }
}
