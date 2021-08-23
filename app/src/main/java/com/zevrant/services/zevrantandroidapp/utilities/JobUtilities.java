package com.zevrant.services.zevrantandroidapp.utilities;

import android.app.job.JobInfo;
import android.app.job.JobScheduler;
import android.content.ComponentName;
import android.content.Context;

import androidx.work.Constraints;
import androidx.work.Data;
import androidx.work.ListenableWorker;
import androidx.work.OneTimeWorkRequest;
import androidx.work.Operation;
import androidx.work.WorkManager;
import androidx.work.WorkRequest;

import com.zevrant.services.zevrantandroidapp.jobs.UpdateJob;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class JobUtilities {

    private final static char[] hexArray = "0123456789ABCDEF".toCharArray();

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

    public static String bytesToHex(byte[] bytes) {
        char[] hexChars = new char[bytes.length * 2];
        for ( int j = 0; j < bytes.length; j++ ) {
            int v = bytes[j] & 0xFF;
            hexChars[j * 2] = hexArray[v >>> 4];
            hexChars[j * 2 + 1] = hexArray[v & 0x0F];
        }
        return new String(hexChars);
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
