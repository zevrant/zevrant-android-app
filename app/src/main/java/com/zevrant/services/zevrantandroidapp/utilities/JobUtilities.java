package com.zevrant.services.zevrantandroidapp.utilities;

import android.content.Context;

import androidx.work.Constraints;
import androidx.work.Data;
import androidx.work.ListenableWorker;
import androidx.work.OneTimeWorkRequest;
import androidx.work.Operation;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;
import androidx.work.WorkRequest;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.concurrent.TimeUnit;

public class JobUtilities {


    // schedule the start of the service immediately
    public static Operation scheduleJob(Context context, Class<? extends ListenableWorker> jobClass,
                                        Constraints constraints, String tag, Data data) {
        WorkManager workManager = WorkManager.getInstance(context);

        workManager.cancelAllWorkByTag(tag);

        WorkRequest workRequest = new OneTimeWorkRequest.Builder(jobClass)
                .addTag(tag)
                .setConstraints(constraints)
                .setInputData(data)
                .build();

        return workManager.enqueue(workRequest);
    }

    public static void cancelWorkByTag(Context context, String tag) {
        WorkManager workManager = WorkManager.getInstance(context);
        workManager.cancelAllWorkByTag(tag);
    }

    public static void schedulePeriodicJob(Context context, Class<? extends ListenableWorker> jobClass,
                                           Constraints constraints, String tag, Data data) {
        schedulePeriodicJob(context, jobClass, constraints, tag, data, 1L, TimeUnit.HOURS);
    }

    public static void schedulePeriodicJob(Context context, Class<? extends ListenableWorker> jobClass,
                                           Constraints constraints, String tag, Data data, long timeInterval,
                                           TimeUnit timeUnit) {
        WorkManager workManager = WorkManager.getInstance(context);

        workManager.cancelAllWorkByTag(tag);

        WorkRequest workRequest = new PeriodicWorkRequest.Builder(jobClass, timeInterval, timeUnit)
                .addTag(tag)
                .setConstraints(constraints)
                .setInputData(data)
                .build();

        workManager.enqueue(workRequest);
    }

    public static long copyData(InputStream is, OutputStream os) throws IOException {
        int byteCount = 0;
        byte[] bytes = new byte[1024];
        int bytesRead = is.read(bytes);
        while (bytesRead >= 0) {
            byteCount += bytesRead;
            os.write(bytes);
            bytes = new byte[1024];
            bytesRead = is.read(bytes);
        }
        return byteCount;
    }
}
