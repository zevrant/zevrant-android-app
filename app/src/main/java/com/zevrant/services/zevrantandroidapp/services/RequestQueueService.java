package com.zevrant.services.zevrantandroidapp.services;

import static org.acra.ACRA.LOG_TAG;

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

import java.io.File;
import java.io.IOException;

public class RequestQueueService {

    private static final Network network = new BasicNetwork(new HurlStack());
    private static RequestQueue requestQueue;
    private static boolean hasBeenInitialized = false;

    public static void init(File filesDir) throws IOException {
        if (!hasBeenInitialized) {
            Cache cache = new DiskBasedCache(filesDir, 1024 * 1024);
            requestQueue = new RequestQueue(cache, network);
            requestQueue.start();
            hasBeenInitialized = true;
        } else {
            Log.i(LOG_TAG, "RequestQueueService has already been initialized");
        }
    }

    public static void addToQueue(JsonObjectRequest request) {
        requestQueue.add(request);
    }

    public static void addToQueue(StringRequest request) {
        requestQueue.add(request);
    }

    public static void addToQueue(InputStreamRequest request) {
        requestQueue.add(request);
    }
}
