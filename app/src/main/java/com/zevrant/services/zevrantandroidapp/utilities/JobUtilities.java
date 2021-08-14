package com.zevrant.services.zevrantandroidapp.utilities;

import android.app.job.JobInfo;
import android.app.job.JobScheduler;
import android.content.ComponentName;
import android.content.Context;

public class JobUtilities {

    private final static char[] hexArray = "0123456789ABCDEF".toCharArray();

    // schedule the start of the service immediately
    public static void scheduleJob(Context context, Class jobClass) { //TODO specifiy constraints
        ComponentName serviceComponent = new ComponentName(context, jobClass);
        JobInfo.Builder builder = new JobInfo.Builder(0, serviceComponent);
        builder.setMinimumLatency(1); // wait at least
        builder.setOverrideDeadline(1); // maximum delay
        JobScheduler jobScheduler = context.getSystemService(JobScheduler.class);
        jobScheduler.schedule(builder.build());
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
}
