package com.zevrant.services.zevrantandroidapp;

import org.acra.ACRA;

import java.security.SecureRandom;
import java.security.cert.X509Certificate;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

public class NukeSSLCerts {
    protected static final String TAG = "NukeSSLCerts";

    private static SSLSocketFactory sslSocketFactory;

    public static SSLSocketFactory getSslSocketFactory() {
        return sslSocketFactory;
    }

    public static TrustManager getTrustManageer() {

        return new X509TrustManager() {
            public X509Certificate[] getAcceptedIssuers() {
                X509Certificate[] myTrustedAnchors = new X509Certificate[0];
                return myTrustedAnchors;
            }

            @Override
            public void checkClientTrusted(X509Certificate[] certs, String authType) {
            }

            @Override
            public void checkServerTrusted(X509Certificate[] certs, String authType) {
            }
        };
    }

    public static void nuke() {
        try {
            TrustManager[] trustAllCerts = new TrustManager[]{
                    getTrustManageer()
            };

            SSLContext sc = SSLContext.getInstance("SSL");
            sc.init(null, trustAllCerts, new SecureRandom());
            sslSocketFactory = sc.getSocketFactory();
            HttpsURLConnection.setDefaultSSLSocketFactory(sslSocketFactory);
            HttpsURLConnection.setDefaultHostnameVerifier(new HostnameVerifier() {
                @Override
                public boolean verify(String arg0, SSLSession arg1) {
                    return true;
                }
            });

        } catch (Exception e) {
            ACRA.getErrorReporter().handleSilentException(e);
        }
    }
}