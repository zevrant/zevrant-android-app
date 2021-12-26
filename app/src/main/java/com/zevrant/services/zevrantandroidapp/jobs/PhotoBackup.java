package com.zevrant.services.zevrantandroidapp.jobs;

import static com.zevrant.services.zevrantandroidapp.utilities.Constants.LOG_TAG;

import android.annotation.SuppressLint;
import android.content.ContentUris;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.MediaStore;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.work.Data;
import androidx.work.ListenableWorker;
import androidx.work.WorkerParameters;
import androidx.work.impl.utils.futures.SettableFuture;

import com.google.common.util.concurrent.ListenableFuture;
import com.zevrant.services.zevrantandroidapp.exceptions.CredentialsNotFoundException;
import com.zevrant.services.zevrantandroidapp.services.BackupService;
import com.zevrant.services.zevrantandroidapp.services.EncryptionService;
import com.zevrant.services.zevrantandroidapp.services.JsonParser;
import com.zevrant.services.zevrantandroidapp.services.OAuthService;
import com.zevrant.services.zevrantandroidapp.utilities.Constants;
import com.zevrant.services.zevrantandroidapp.utilities.JobUtilities;
import com.zevrant.services.zevrantuniversalcommon.contants.Roles;
import com.zevrant.services.zevrantuniversalcommon.rest.CheckExistence;
import com.zevrant.services.zevrantuniversalcommon.rest.FileInfo;
import com.zevrant.services.zevrantuniversalcommon.rest.backup.request.BackupFileRequest;

import org.acra.ACRA;
import org.apache.commons.codec.digest.MessageDigestAlgorithms;

import java.io.IOException;
import java.io.InputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

@SuppressLint("RestrictedApi")
public class PhotoBackup extends ListenableWorker {

    private final String[] projection = new String[]{
            MediaStore.Images.Media._ID,
            MediaStore.Images.Media.DISPLAY_NAME,
            MediaStore.Images.Media.SIZE
    };
    private final Context context;
    private final Data.Builder dataBuilder;
    private SettableFuture<Result> mFuture = null;

    public PhotoBackup(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
        this.context = context;
        this.dataBuilder = new Data.Builder();
    }

    public static String getChecksum(MessageDigest digest, InputStream is) throws IOException {
        byte[] bytes = new byte[1024];
        while (is.read(bytes) > -1) {
            digest.update(bytes);
        }

        return JobUtilities.bytesToHex(digest.digest());
    }

    @NonNull
    @Override
    public ListenableFuture<Result> startWork() {
        mFuture = SettableFuture.create();
        if (!EncryptionService.isInitialized()
                || !EncryptionService.hasSecret(Constants.SecretNames.REFRESH_TOKEN_1)) {
            //TODO send notification prompting user login
            Log.e("User not logged in", LOG_TAG);
            mFuture.set(Result.failure());
            return mFuture;
        }
        getBackgroundExecutor().execute(() -> {
            boolean hasBackupPermission = OAuthService.canI(Roles.BACKUPS);
            if (!hasBackupPermission) {
                dataBuilder.putString("reason", "user does not have permission to access backups");
                mFuture.set(Result.failure(dataBuilder.build()));
            } else {
                Uri uri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
                try (Cursor cursor = context.getContentResolver().query(
                        uri,
                        projection,
                        null,
                        new String[]{},
                        null
                )) {
                    Log.i(LOG_TAG, "Job running");
                    List<FileInfo> fileInfoList = new ArrayList<>();
                    Log.i(LOG_TAG, "found ".concat(String.valueOf(cursor.getCount()).concat(" images")));
                    int idColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media._ID);
                    int nameColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DISPLAY_NAME);
                    int sizeColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.SIZE);
                    while (cursor.moveToNext()) {
                        processMediaStoreResultSet(idColumn, nameColumn, sizeColumn, cursor, uri, fileInfoList);
                    }
                    backupFiles(fileInfoList, uri);
                } catch (IOException | NoSuchAlgorithmException | CredentialsNotFoundException | ExecutionException | InterruptedException e) {
                    ACRA.getErrorReporter().handleSilentException(e);
                    e.printStackTrace();
                    mFuture.set(Result.failure());
                }
            }
        });
        return mFuture;
    }

    private void backupFiles(List<FileInfo> fileInfoList, Uri uri) throws CredentialsNotFoundException, ExecutionException, InterruptedException {
        Log.i(LOG_TAG, String.valueOf(fileInfoList.size()).concat(" images hashed"));
        Future<String> checkExistenceResponse = BackupService.checkExistence(new CheckExistence(fileInfoList));
        CheckExistence existence = JsonParser.readValueFromString(checkExistenceResponse.get(), CheckExistence.class);
        assert existence != null : "null value returned from existence check for photo backup";
        assert existence.getFileInfos() != null : "null was returned for list of fileinfos in photobackup";
        Log.i(LOG_TAG, "Successfully checked existence of file hashes ".concat(String.valueOf(existence.getFileInfos().size()).concat(" files were not found on backup server")));
        List<FileInfo> fileInfos = existence.getFileInfos();
        for (int i = 0; i < fileInfos.size(); i++) {
            try {
                sendBackUp(fileInfos.get(i), uri);
            } catch (IOException e) {
                Log.e(LOG_TAG, "Failed to read img file skipping...");
                ACRA.getErrorReporter().handleSilentException(e);
            }
        }
        Data data = dataBuilder.build();
        mFuture.set(Result.success(data));

    }

    private void sendBackUp(FileInfo fileInfo, Uri uri) throws IOException, CredentialsNotFoundException, ExecutionException, InterruptedException {
        BackupFileRequest backupFileRequest = new BackupFileRequest(fileInfo, JobUtilities.bytesToHex(getFileBytes(uri, fileInfo)));
        Log.i(LOG_TAG, backupFileRequest.toString());
        Log.i(LOG_TAG, "backing up file ".concat(fileInfo.getFileName()));
        Future<String> future = BackupService.backupFile(backupFileRequest);
        future.get();//don't really care about successfull responses just need to block until done otherwise we
        // will enqueue too many requests and throw an OOM exception
        Log.i(LOG_TAG, fileInfo.getFileName().concat(" was successfully backed up"));
    }

    private byte[] getFileBytes(Uri uri, FileInfo fileInfo) throws IOException {
        InputStream is = this.context
                .getContentResolver()
                .openAssetFileDescriptor(ContentUris.withAppendedId(uri, fileInfo.getId()), "r")
                .createInputStream();

        byte[] bytes = new byte[(int) fileInfo.getSize()];
        int read = is.read(bytes);
        Log.i(LOG_TAG, String.valueOf(read));
        assert read == fileInfo.getSize() : "failed to read entire contents of file in photo backup";
        return bytes;
    }

    private void processMediaStoreResultSet(int idColumn, int nameColumn, int sizeColumn, Cursor cursor, Uri uri, List<FileInfo> fileInfoList) throws NoSuchAlgorithmException, IOException {
        long id = cursor.getLong(idColumn);
        String name = cursor.getString(nameColumn);
        long size = cursor.getLong(sizeColumn);
        MessageDigest digest = MessageDigest.getInstance(MessageDigestAlgorithms.SHA_512);

        InputStream is = getApplicationContext()
                .getContentResolver()
                .openAssetFileDescriptor(ContentUris.withAppendedId(uri, id), "r")
                .createInputStream();
        String hash = getChecksum(digest, is);
        fileInfoList.add(new FileInfo(name, hash, id, size));
        // Stores column values and the contentUri in a local object
        // that represents the media file.
        is.close();
    }
}
