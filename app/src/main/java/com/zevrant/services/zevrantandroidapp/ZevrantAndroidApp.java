package com.zevrant.services.zevrantandroidapp;

import android.app.Application;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;

import com.zevrant.service.zevrantandroidapp.NukeSSLCerts;

import org.acra.ACRA;
import org.acra.annotation.AcraCore;
import org.acra.annotation.AcraHttpSender;
import org.acra.config.CoreConfigurationBuilder;
import org.acra.config.HttpSenderConfigurationBuilder;
import org.acra.data.StringFormat;
import org.acra.sender.HttpSender;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@AcraCore(buildConfigClass = BuildConfig.class)
public class ZevrantAndroidApp extends Application {
    private static final Logger logger = LoggerFactory.getLogger(ZevrantAndroidApp.class);
    public static final String CHANNEL_ID = "test";
    public static final String CHANNEL_ID_1 = "test1";

    private static final ExecutorService executorService = Executors.newFixedThreadPool(4);

    public ZevrantAndroidApp() {
    }

    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(base);
        CoreConfigurationBuilder builder = new CoreConfigurationBuilder(this);
        builder
                .setReportFormat(StringFormat.JSON)
                .getPluginConfigurationBuilder(HttpSenderConfigurationBuilder.class)
                .setBasicAuthLogin(base.getString(R.string.reportsUsername))
                .setBasicAuthPassword(base.getString(R.string.reportsPassword))
                .setHttpMethod(HttpSender.Method.POST)
                .setUri(base.getString(R.string.reportsUrl))
                .setEnabled(true);
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
