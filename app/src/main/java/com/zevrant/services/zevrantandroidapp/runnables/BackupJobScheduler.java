package com.zevrant.services.zevrantandroidapp.runnables;

import android.app.AppOpsManager;
import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import androidx.work.*;
import com.zevrant.services.zevrantandroidapp.jobs.PhotoBackup;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.TimeUnit;

import static android.app.AppOpsManager.MODE_ALLOWED;
import static android.app.AppOpsManager.OPSTR_GET_USAGE_STATS;

public class BackupJobScheduler implements Runnable {

    private static final Logger logger = LoggerFactory.getLogger(BackupJobScheduler.class);

    private static final Handler mainHandler = new Handler(Looper.getMainLooper());


    private final Context context;
    private String username;
    private String password;

    public BackupJobScheduler(Context context, String username, String password) {
        this.context = context;
        this.username = username;
        this.password = password;
    }

    private boolean checkForPermission(Context context) {
        AppOpsManager appOps = (AppOpsManager) context.getSystemService(Context.APP_OPS_SERVICE);
        int mode = appOps.unsafeCheckOpNoThrow(OPSTR_GET_USAGE_STATS, android.os.Process.myUid(), context.getPackageName());
        return mode == MODE_ALLOWED;//TODO move app permission request here
    }

    @Override
    public void run() {
        Data data = new Data.Builder()
                .putString("username", username)
                .putString("password", password)
                .build();
//        while (!Thread.interrupted()) {

            Constraints constraints = new Constraints.Builder()
                    .setTriggerContentMaxDelay(1, TimeUnit.SECONDS)
                    .build();

            WorkRequest request = new OneTimeWorkRequest.Builder(PhotoBackup.class)
                    .setConstraints(constraints)
                    .setInputData(data)
                    .build();

            WorkManager.getInstance(context)
                    .enqueue(request);

//            try {
//                Thread.sleep(5000);
//            } catch (InterruptedException e) {
//                logger.info("Schedule running apps job thread has been interrupted shutting down");
//            }
//        }
    }
}
