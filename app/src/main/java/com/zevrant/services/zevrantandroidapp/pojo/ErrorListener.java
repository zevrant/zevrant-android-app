package com.zevrant.services.zevrantandroidapp.pojo;

import static com.zevrant.services.zevrantandroidapp.utilities.Constants.LOG_TAG;

import android.content.Context;
import android.util.Log;

import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.zevrant.services.zevrantandroidapp.exceptions.CredentialsNotFoundException;
import com.zevrant.services.zevrantandroidapp.services.CredentialsService;
import com.zevrant.services.zevrantandroidapp.services.RequestQueueService;
import com.zevrant.services.zevrantandroidapp.volley.requests.OAuthRequest;

import org.acra.ACRA;

public abstract class ErrorListener<T extends Request> implements Response.ErrorListener {

    private T request;
    private final CredentialsService credentialsService;
    private final RequestQueueService requestQueueService;

    public ErrorListener(CredentialsService credentialsService, RequestQueueService requestQueueService, Context context) {
        this.credentialsService = credentialsService;
        this.requestQueueService = requestQueueService;
    }

    public T getRequest() {
        return request;
    }

    public void setRequest(T request) {
        this.request = request;
    }

    @Override
    public abstract void onErrorResponse(VolleyError error) ;

    public void reAuthAndExecuteStringRequest() {
        if(request == null) {
            throw new RuntimeException("No request set cannot re-authenticate");
        }

        ( (OAuthRequest) request).setOAuthToken(credentialsService.getAuthorization());
        requestQueueService.addToQueue((StringRequest) request);


    }

}
