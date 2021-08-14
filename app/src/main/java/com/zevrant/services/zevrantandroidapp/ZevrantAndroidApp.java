package com.zevrant.services.zevrantandroidapp;

import android.app.Application;
import android.app.NotificationChannel;
import android.app.NotificationManager;

import com.zevrant.service.zevrantandroidapp.NukeSSLCerts;
import com.zevrant.services.zevrantandroidapp.services.RequestQueueService;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ZevrantAndroidApp extends Application {
    private static final Logger logger = LoggerFactory.getLogger(ZevrantAndroidApp.class);
    public static final String CHANNEL_ID = "test";
    public static final String CHANNEL_ID_1 = "test1";

    private static final ExecutorService executorService = Executors.newFixedThreadPool(4);

    public ZevrantAndroidApp() throws Exception {
//        ZevrantIOC.run(ZevrantAndroidApp.class);
        logger.info("CREATED");
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
