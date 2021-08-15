package com.zevrant.services.zevrantandroidapp.volley.requests;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.android.volley.NetworkResponse;
import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.toolbox.HttpHeaderParser;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

public class InputStreamRequest extends Request<InputStream> {

    private final Response.Listener<InputStream> listener;
    private final Map<String, String> headers;

    public InputStreamRequest(int method, String url, @NonNull Response.Listener<InputStream> listener,
                              @Nullable Response.ErrorListener errorListener) {
        super(method, url, errorListener);
        this.listener = listener;
        this.headers = new HashMap<>();

    }

    @Override
    protected Response<InputStream> parseNetworkResponse(NetworkResponse response) {
        return Response.success(new ByteArrayInputStream(response.data), HttpHeaderParser.parseCacheHeaders(response));
    }

    @Override
    protected void deliverResponse(InputStream response) {
        listener.onResponse(response);
    }

    public void setOAuthToken(String token) {
        headers.put("Authorization", "bearer ".concat(token));
    }

    @Override
    public Map<String, String> getHeaders() {
        return this.headers;
    }
}
