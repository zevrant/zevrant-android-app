package com.zevrant.services.zevrantandroidapp;

import javax.net.ssl.SSLSocketFactory;

public class NukeSSLCerts {
    protected static final String TAG = "NukeSSLCerts";

    public static void nuke() {
        //do nothing only needed for local builds
    }

    public static SSLSocketFactory getSslSocketFactory() {
        return null;
    }
}