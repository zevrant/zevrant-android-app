package com.zevrant.services.zevrantandroidapp.runnables;

import static android.app.AppOpsManager.MODE_ALLOWED;
import static android.app.AppOpsManager.OPSTR_GET_USAGE_STATS;

import android.app.AppOpsManager;
import android.content.Context;
import android.os.Handler;
import android.os.Looper;

import androidx.work.Constraints;
import androidx.work.Data;
import androidx.work.ListenableWorker;
import androidx.work.Operation;

import com.google.common.util.concurrent.ListenableFuture;
import com.zevrant.services.zevrantandroidapp.BuildConfig;
import com.zevrant.services.zevrantandroidapp.jobs.PhotoBackup;
import com.zevrant.services.zevrantandroidapp.jobs.UpdateJob;
import com.zevrant.services.zevrantandroidapp.utilities.JobUtilities;

import org.acra.ACRA;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ExecutionException;
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

    @Override
    public void run() {
        Data data = new Data.Builder()
                .putString("username", username)
                .putString("password", password)
                .build();

            Constraints constraints = new Constraints.Builder()
                    .setTriggerContentMaxDelay(1, TimeUnit.SECONDS)
                    .build();

            ListenableFuture<Operation.State.SUCCESS> results
                    = JobUtilities.scheduleJob(context, UpdateJob.class, constraints, UPDATE_TAG, data).getResult();


            results.addListener(() -> {
                    JobUtilities.scheduleJob(context, PhotoBackup.class, constraints, BACKUP_TAG, data);
            }, Runnable::run);

            logger.info("Build Type is {}, should upload all? {}", BuildConfig.BUILD_TYPE, BuildConfig.BUILD_TYPE.equals("release"));
            logger.info("Sleeping for {}", 3.6 * Math.pow(10, 6));
    }
}
