package com.zevrant.services.zevrantandroidapp;

import android.app.Application;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;

import androidx.annotation.NonNull;
import androidx.hilt.work.HiltWorkerFactory;
import androidx.work.Configuration;

import org.acra.ACRA;
import org.acra.annotation.AcraCore;
import org.acra.config.CoreConfigurationBuilder;
import org.acra.config.HttpSenderConfigurationBuilder;
import org.acra.data.StringFormat;
import org.acra.sender.HttpSender;

import javax.inject.Inject;

import dagger.hilt.android.HiltAndroidApp;

@HiltAndroidApp
@AcraCore(buildConfigClass = BuildConfig.class)
public class ZevrantAndroidApp extends Application implements Configuration.Provider {

    public static final String CHANNEL_ID = "test";
    public static final String CHANNEL_ID_1 = "test1";

    private HiltWorkerFactory hiltWorkerFactory;

    @Inject
    public ZevrantAndroidApp() {
    }

    @Inject
    public void setHiltWorkerFactory(HiltWorkerFactory hiltWorkerFactory) {
        this.hiltWorkerFactory = hiltWorkerFactory;
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

    @NonNull
    @Override
    public Configuration getWorkManagerConfiguration() {
        return new Configuration.Builder()
                .setWorkerFactory(hiltWorkerFactory)
                .build();

    }
}
