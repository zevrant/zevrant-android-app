package com.zevrant.services.zevrantandroidapp.services.android;

import android.app.Notification;
import android.app.Service;
import android.content.Intent;
import android.os.IBinder;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

import com.zevrant.services.zevrantandroidapp.ZevrantAndroidApp;
import com.zevrant.services.zevrantandroidapp.runnables.BackupJobScheduler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainService extends Service {

    private static final Logger logger = LoggerFactory.getLogger(MainService.class);

    private BackupJobScheduler runner;
    private ExecutorService executorService;

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        runner = new BackupJobScheduler(this, intent.getStringExtra("username"), intent.getStringExtra("password"));

        Notification notification = new NotificationCompat.Builder(this, ZevrantAndroidApp.CHANNEL_ID)
                .setContentTitle("TEST")
                .setContentText("TEST")
                .build();
        startForeground(1, notification);

        executorService = Executors.newCachedThreadPool();
        executorService.submit(runner);
        return START_REDELIVER_INTENT;
    }

    public void onDestroy() {
        executorService.shutdown();
    }
}
