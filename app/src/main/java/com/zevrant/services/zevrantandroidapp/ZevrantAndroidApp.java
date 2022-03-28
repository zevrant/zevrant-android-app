package com.zevrant.services.zevrantandroidapp;

import android.app.Application;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;

import org.acra.ACRA;
import org.acra.annotation.AcraCore;
import org.acra.config.CoreConfigurationBuilder;
import org.acra.config.HttpSenderConfigurationBuilder;
import org.acra.data.StringFormat;
import org.acra.sender.HttpSender;

import dagger.hilt.android.HiltAndroidApp;

@HiltAndroidApp
@AcraCore(buildConfigClass = BuildConfig.class)
public class ZevrantAndroidApp extends Application {

    public static final String CHANNEL_ID = "test";
    public static final String CHANNEL_ID_1 = "test1";
    public ZevrantAndroidApp() {
    }

    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(base);
        CoreConfigurationBuilder builder = new CoreConfigurationBuilder(this);
        boolean acraEnabled = BuildConfig.VERSION_CODE != 3;
        builder
                .setReportFormat(StringFormat.JSON)
                .getPluginConfigurationBuilder(HttpSenderConfigurationBuilder.class)
                .setHttpMethod(HttpSender.Method.POST)
                .setUri(base.getString(R.string.reportsUrl))
                .setEnabled(acraEnabled);
        // The following line triggers the initialization of ACRA
        ACRA.init(this, builder);
    }

    @Override
    public void onCreate() {
        super.onCreate();
        NukeSSLCerts.nuke();
        createNotificationChannel();
    }

    private void createNotificationChannel() {
        NotificationChannel serviceChannel = new NotificationChannel(CHANNEL_ID, "test", NotificationManager.IMPORTANCE_HIGH);
        NotificationManager manager = getSystemService(NotificationManager.class);
        manager.createNotificationChannel(serviceChannel);
        NotificationChannel notificationChannel = new NotificationChannel(CHANNEL_ID_1, "test1", NotificationManager.IMPORTANCE_HIGH);
        manager.createNotificationChannel(notificationChannel);
    }

}
