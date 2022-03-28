package com.zevrant.services.zevrantandroidapp.services;

import static com.zevrant.services.zevrantandroidapp.utilities.Constants.LOG_TAG;

import android.content.Context;
import android.util.Log;

import com.android.volley.Cache;
import com.android.volley.Network;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.BasicNetwork;
import com.android.volley.toolbox.DiskBasedCache;
import com.android.volley.toolbox.HurlStack;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.StringRequest;
import com.zevrant.services.zevrantandroidapp.volley.requests.InputStreamRequest;

import javax.inject.Inject;

import dagger.hilt.android.qualifiers.ApplicationContext;

public class RequestQueueService {

    private final Network network = new BasicNetwork(new HurlStack());
    private RequestQueue requestQueue;

    @Inject
    public RequestQueueService(@ApplicationContext Context context) {
            Cache cache = new DiskBasedCache(context.getFilesDir(), 1024 * 1024);
            requestQueue = new RequestQueue(cache, network);
            requestQueue.start();
    }

    public void addToQueue(JsonObjectRequest request) {
        Log.d(LOG_TAG, "requesting from ".concat(request.getUrl()));
        requestQueue.add(request);
    }

    public void addToQueue(StringRequest request) {
        Log.d(LOG_TAG, "requesting from ".concat(request.getUrl()));
        requestQueue.add(request);
    }

    public void addToQueue(InputStreamRequest request) {
        Log.d(LOG_TAG, "requesting from ".concat(request.getUrl()));
        requestQueue.add(request);
    }
}
