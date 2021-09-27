package com.zevrant.services.zevrantandroidapp.volley.requests;

import androidx.annotation.Nullable;

import com.android.volley.Response;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

public class StringRequest extends com.android.volley.toolbox.StringRequest {

    private final Map<String, String> headers;
    private final Map<String, String> params;
    private String body;

    public StringRequest(int method, String url, Response.Listener<String> listener, @Nullable Response.ErrorListener errorListener) {
        super(method, url, listener, errorListener);
        this.headers = new HashMap<>();
        this.params = new HashMap<>();

        this.setContentType("application/json");
    }

    public StringRequest(int method, String url, String body, Response.Listener<String> listener, @Nullable Response.ErrorListener errorListener) {
        this(method, url, listener, errorListener);
        this.body = body;
    }

    @Override
    public byte[] getBody() {
        if (this.body != null) {
            return this.body.getBytes(StandardCharsets.UTF_8);
        } else return null;
    }

    public void setHeader(String key, String value) {
        headers.put(key, value);
    }

    @Override
    public Map<String, String> getHeaders() {
        return this.headers;
    }

    @Nullable
    @Override
    protected Map<String, String> getParams() {
        return this.params;
    }

    public void setParam(String key, String value) {
        params.put(key, value);
    }

    public void setContentType(String contentType) {
        headers.put("Content-Type", contentType);
    }

    public void setOAuthToken(String token) {
        headers.put("Authorization", "bearer ".concat(token));
    }
}
