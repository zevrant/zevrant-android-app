package com.zevrant.services.zevrantandroidapp.services;

import static com.zevrant.services.zevrantandroidapp.utilities.Constants.LOG_TAG;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import android.Manifest;
import android.content.ContentUris;
import android.content.Context;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.provider.MediaStore;
import android.util.Log;

import androidx.core.content.ContextCompat;

import com.android.volley.Request;
import com.zevrant.services.zevrantandroidapp.volley.requests.StringRequest;
import com.zevrant.services.zevrantuniversalcommon.rest.FileInfo;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;

public class CleanupService {
    private final String[] projection = new String[]{
            MediaStore.Images.Media._ID
    };

    public Future<String> eraseBackups(String authorization, String backupUrl, Context context) {
        CompletableFuture<String> future = new CompletableFuture<>();
        StringRequest objectRequest = new StringRequest(Request.Method.DELETE,
                backupUrl.concat("/file-backup"),
                DefaultRequestHandlers.getResponseListener(future),
                DefaultRequestHandlers.getErrorResponseListener(future));
        objectRequest.setOAuthToken(authorization);
        RequestQueueService.addToQueue(objectRequest);
        deleteAllFromMediaStore(context);
        return future;
    }

    private void deleteAllFromMediaStore(Context context) {
        assertThat("Permission was not granted", ContextCompat.checkSelfPermission(context, Manifest.permission.WRITE_EXTERNAL_STORAGE), is(PackageManager.PERMISSION_GRANTED));

        Uri mediaUri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
        try (Cursor cursor = context.getContentResolver().query(
                mediaUri,
                projection,
                null,
                new String[]{},
                null
        )) {
            Log.i(LOG_TAG, "Job running");
            List<FileInfo> fileInfoList = new ArrayList<>();
            Log.i(LOG_TAG, "found ".concat(String.valueOf(cursor.getCount()).concat(" images")));
            int idColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media._ID);
            while (cursor.moveToNext()) {
                Uri fileUri = ContentUris.withAppendedId(
                        MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, cursor.getInt(idColumn)
                );
                context.getContentResolver().delete(fileUri, null, null);
            }
        }
    }

}
