package com.zevrant.services.zevrantandroidapp.services;

import static com.zevrant.services.zevrantandroidapp.utilities.Constants.LOG_TAG;

import android.content.Context;
import android.util.Log;

import com.android.volley.Request;
import com.fasterxml.jackson.core.type.TypeReference;
import com.zevrant.services.zevrantandroidapp.R;
import com.zevrant.services.zevrantandroidapp.activities.ZevrantServices;
import com.zevrant.services.zevrantandroidapp.exceptions.CredentialsNotFoundException;
import com.zevrant.services.zevrantandroidapp.utilities.Constants;
import com.zevrant.services.zevrantandroidapp.volley.requests.StringRequest;
import com.zevrant.services.zevrantuniversalcommon.contants.Roles;
import com.zevrant.services.zevrantuniversalcommon.rest.oauth.request.CodeExchangeRequest;
import com.zevrant.services.zevrantuniversalcommon.rest.oauth.request.TokenRefreshRequest;
import com.zevrant.services.zevrantuniversalcommon.rest.oauth.response.OAuthToken;

import org.apache.commons.lang3.StringUtils;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

import javax.inject.Inject;

import dagger.hilt.android.qualifiers.ApplicationContext;

public class OAuthService {

    private final Context context;
    private final String keycloakUrl;
    private final String oauthUrl;
    private final String redirectUri;
    private final JsonParser jsonParser;
    private final RequestQueueService requestQueueService;
    private List<String> roles;

    @Inject
    public OAuthService(@ApplicationContext Context context, JsonParser jsonParser,
                        RequestQueueService requestQueueService) {
        keycloakUrl = context.getString(R.string.keycloak_url);
        oauthUrl = context.getString(R.string.oauth_base_url);
        redirectUri = context.getString(R.string.redirect_uri);
        this.jsonParser = jsonParser;
        this.requestQueueService = requestQueueService;
        roles = new ArrayList<>();
        this.context = context;
    }

    public boolean canI(Roles action, Context context, String authorization, CredentialsService credentialsService) {
        if (roles.isEmpty()) {
            loadRoles(context, authorization, credentialsService);
        }
        return roles.contains(action.name().toLowerCase());
    }

//    public OAuthToken exchangeCode(String code) throws ExecutionException, InterruptedException {
//        String url = keycloakUrl;
//        CodeExchangeRequest codeExchangeRequest = new CodeExchangeRequest(code, redirectUri);
//        CompletableFuture<String> future = new CompletableFuture<>();
//        StringRequest request = new StringRequest(Request.Method.POST,
//                keycloakUrl,
//                jsonParser.writeValueAsString(codeExchangeRequest),
//                Constants.DefaultRequestHandlers.getResponseListener(future),
//                Constants.DefaultRequestHandlers.getErrorResponseListener(future, credentialsService, requestQueueService, context));
//        request.setContentType("application/json");
//        requestQueueService.addToQueue(request);
//        String response = future.get();
//        assert StringUtils.isNotBlank(response);
//        OAuthToken token = jsonParser.readValueFromString(response, OAuthToken.class);
//        assert token != null : "token parsed from string was null";
//        token.setExpiresInDateTime(LocalDateTime.ofEpochSecond(LocalDateTime.now().toEpochSecond(ZoneId.of("US/Eastern").getRules().getOffset(LocalDateTime.now())) - token.getExpiresIn(), 0, ZoneId.of("US/Eastern").getRules().getOffset(LocalDateTime.now())));
//        token.setRefreshExpiresInDateTime(LocalDateTime.ofEpochSecond(LocalDateTime.now().toEpochSecond(ZoneId.of("US/Eastern").getRules().getOffset(LocalDateTime.now())) - token.getRefreshExpiresIn(), 0, ZoneId.of("US/Eastern").getRules().getOffset(LocalDateTime.now())));
//        return token;
//    }

    public OAuthToken refreshToken(OAuthToken oAuthToken) throws ExecutionException, InterruptedException, CredentialsNotFoundException {
        OAuthToken token = null;
        URL url = null;
        try {
            url = new URL("https://" + context.getString(R.string.encoded_authority) +
                    "/auth/realms/" + context.getString(R.string.keycloak_realm) + "/protocol/openid-connect/token");

            String urlParameters = "client_id=android"
                    .concat("&refresh_token=".concat(URLEncoder.encode(oAuthToken.getRefreshToken(), String.valueOf(StandardCharsets.UTF_8)))
                            .concat("&grant_type=refresh_token"));

            HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();
            urlConnection.setDoOutput(true);
            urlConnection.setDoInput(true);
            urlConnection.setRequestMethod("POST");
            urlConnection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
            urlConnection.setRequestProperty("Charset", "UTF-8");

            urlConnection.connect();

            DataOutputStream outputStream = new DataOutputStream(urlConnection.getOutputStream());
            Log.d(LOG_TAG, "Writing oauth token request to server");
            outputStream.write(urlParameters.getBytes(StandardCharsets.UTF_8));
            outputStream.flush();
            outputStream.close();

            Log.d(LOG_TAG, "Received response with code " + urlConnection.getResponseCode());
            if(urlConnection.getResponseCode() > 299) {
                String errorResponse = new BufferedReader(new InputStreamReader(urlConnection.getErrorStream())).lines().collect(Collectors.joining()).toLowerCase(Locale.ROOT);
                if(errorResponse.contains("token is not active")
                        || errorResponse.contains("session not active")) {
                    Log.d(LOG_TAG, "Stored token is invalid, user should do a fresh login");
                    return null;
                } else {
                    Log.v(LOG_TAG, urlParameters);
                    Log.v(LOG_TAG, errorResponse);
                    throw new RuntimeException(new JSONObject(errorResponse).getString("error_description"));
                }

            }
            Log.d(LOG_TAG, "Received response with  " + urlConnection.getInputStream().available() + "bytes available");
            BufferedReader responseReader = new BufferedReader(new InputStreamReader(urlConnection.getInputStream()));
            String response = responseReader.lines().collect(Collectors.joining());
            Log.d(LOG_TAG, "Received response with a string length of " + response.length());
            token = jsonParser.readValueFromString(response, OAuthToken.class);
            setExpirationDateTime(token);
            assert !token.getRefreshToken().equals(oAuthToken.getRefreshToken()): "Refresh token was not updated!";
        } catch (IOException | JSONException e) {
            e.printStackTrace();
        }
        return token;
    }

    public void loadRoles(Context context, String authorization, CredentialsService credentialsService) {
        CompletableFuture<String> future = new CompletableFuture<>();
        StringRequest request = new StringRequest(Request.Method.GET,
                oauthUrl.concat("/users/me/roles"),
                null,
                Constants.DefaultRequestHandlers.getResponseListener(future),
                Constants.DefaultRequestHandlers.getErrorResponseListener(future, credentialsService, requestQueueService, context)
        );
        try {
            request.setOAuthToken(authorization);
            requestQueueService.addToQueue(request);
            String response = future.get().toLowerCase(Locale.ROOT);
            if(response.contains("login needed")) {
                loadRoles(context, credentialsService.getAuthorization(), credentialsService);
            } else {
                roles = jsonParser.readValueFromString(response, new TypeReference<List<String>>() {
                });
                roles = (roles == null) ? roles = Collections.emptyList() : roles;
            }
        } catch (InterruptedException | ExecutionException interruptedException) {
            interruptedException.printStackTrace();
        }

    }

    public OAuthToken login(String username, String password) {
        OAuthToken oAuthToken = null;
        URL url = null;
        try {
            url = new URL("https://" + context.getString(R.string.encoded_authority) +
                    "/auth/realms/" + context.getString(R.string.keycloak_realm) + "/protocol/openid-connect/token");

            String urlParameters = "client_id=android"
                    .concat("&username=".concat(URLEncoder.encode(username, String.valueOf(StandardCharsets.UTF_8)))
                    .concat("&password=").concat(URLEncoder.encode(password, String.valueOf(StandardCharsets.UTF_8)))
                    .concat("&grant_type=password"));

            HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();
            urlConnection.setDoOutput(true);
            urlConnection.setDoInput(true);
            urlConnection.setRequestMethod("POST");
            urlConnection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
            urlConnection.setRequestProperty("Charset", "UTF-8");

            urlConnection.connect();

            DataOutputStream outputStream = new DataOutputStream(urlConnection.getOutputStream());
            Log.d(LOG_TAG, "Writing oauth token request to server");
            outputStream.write(urlParameters.getBytes(StandardCharsets.UTF_8));
            outputStream.flush();
            outputStream.close();

            Log.d(LOG_TAG, "Received response with code " + urlConnection.getResponseCode());
            if(urlConnection.getResponseCode() > 299) {
                String errorResponse = new BufferedReader(new InputStreamReader(urlConnection.getErrorStream())).lines().collect(Collectors.joining()).toLowerCase(Locale.ROOT);
                throw new RuntimeException(new JSONObject(errorResponse).getString("error_description"));
            }
            Log.d(LOG_TAG, "Received response with  " + urlConnection.getInputStream().available() + "bytes available");
            BufferedReader responseReader = new BufferedReader(new InputStreamReader(urlConnection.getInputStream()));
            String response = responseReader.lines().collect(Collectors.joining());
            Log.d(LOG_TAG, "Received response with a string length of " + response.length());
            oAuthToken = jsonParser.readValueFromString(response, OAuthToken.class);
            Log.d(LOG_TAG, "Received response with access token " + oAuthToken.getAccessToken());
            setExpirationDateTime(oAuthToken);
            assert StringUtils.isNotBlank(oAuthToken.getAccessToken()): "No access token was provided!";
        } catch (IOException | JSONException e) {
            e.printStackTrace();
        }
        return oAuthToken;
    }

    private void setExpirationDateTime(OAuthToken oAuthToken) {

        try {
            String decodedAccessToken = new String(Base64.getDecoder().decode(oAuthToken.getAccessToken().split("\\.")[1].getBytes(StandardCharsets.UTF_8)));
            String decodedRefreshToken = new String(Base64.getDecoder().decode(oAuthToken.getRefreshToken().split("\\.")[1].getBytes(StandardCharsets.UTF_8)));
            long expiresInEpoch = new JSONObject(decodedAccessToken).getLong("exp");
            long refreshExpiresInEpoch = new JSONObject(decodedRefreshToken).getLong("exp");
            ZonedDateTime zonedDateTime = ZonedDateTime.now(ZoneId.of("US/Eastern"));
            oAuthToken.setExpiresInDateTime(LocalDateTime.ofEpochSecond(expiresInEpoch, 0, zonedDateTime.getOffset()));
            oAuthToken.setRefreshExpiresInDateTime(LocalDateTime.ofEpochSecond(refreshExpiresInEpoch, 0, zonedDateTime.getOffset()));
        } catch (JSONException e) {
            e.printStackTrace();
            throw new RuntimeException("Failed to decode access token");
        }

    }
}
