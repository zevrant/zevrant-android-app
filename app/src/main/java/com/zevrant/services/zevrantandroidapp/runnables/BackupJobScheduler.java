package com.zevrant.services.zevrantandroidapp.runnables;

import static android.app.AppOpsManager.MODE_ALLOWED;
import static android.app.AppOpsManager.OPSTR_GET_USAGE_STATS;

import android.app.AppOpsManager;
import android.content.Context;
import android.os.Handler;
import android.os.Looper;

import androidx.work.Constraints;
import androidx.work.Data;
import androidx.work.OneTimeWorkRequest;
import androidx.work.Operation;
import androidx.work.WorkManager;
import androidx.work.WorkRequest;

import com.google.common.util.concurrent.ListenableFuture;
import com.zevrant.services.zevrantandroidapp.BuildConfig;
import com.zevrant.services.zevrantandroidapp.jobs.PhotoBackup;
import com.zevrant.services.zevrantandroidapp.jobs.UpdateJob;

import org.acra.ACRA;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.TimeUnit;

public class BackupJobScheduler implements Runnable {

    private static final Logger logger = LoggerFactory.getLogger(BackupJobScheduler.class);

    private static final Handler mainHandler = new Handler(Looper.getMainLooper());
    private static final String BACKUP_TAG = "BACKUP";
    private static final String UPDATE_TAG = "UPDATE";

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
        while (!Thread.interrupted()) {

            Constraints constraints = new Constraints.Builder()
                    .setTriggerContentMaxDelay(1, TimeUnit.SECONDS)
                    .build();

            WorkManager workManager = WorkManager.getInstance(context);

            workManager.cancelAllWorkByTag(BACKUP_TAG);
            workManager.cancelAllWorkByTag(UPDATE_TAG);

            WorkRequest request = new OneTimeWorkRequest.Builder(PhotoBackup.class)
                    .addTag(BACKUP_TAG)
                    .setConstraints(constraints)
                    .setInputData(data)
                    .build();

            WorkRequest updateRequest = new OneTimeWorkRequest.Builder(UpdateJob.class)
                    .addTag(UPDATE_TAG)
                    .setConstraints(constraints)
                    .setInputData(data)
                    .build();

            ListenableFuture<Operation.State.SUCCESS> results = workManager.enqueue(updateRequest).getResult();

            results.addListener(() -> {
                workManager.enqueue(request);
            }, Runnable::run);

            logger.info("Build Type is {}, should upload all? {}", BuildConfig.BUILD_TYPE, BuildConfig.BUILD_TYPE.equals("release"));
            logger.info("Sleeping for {}", 3.6 * Math.pow(10, 6));
            if (!BuildConfig.BUILD_TYPE.equals("release")) {
                break;
            }

            try {

                Thread.sleep((int) Math.floor(3.6 * Math.pow(10, 6)));
            } catch (InterruptedException e) {
                ACRA.getErrorReporter().handleSilentException(e);
                logger.info("Schedule running apps job thread has been interrupted shutting down");
            }

        }
    }
}
