package com.zevrant.services.zevrantandroidapp.jobs;

import android.annotation.SuppressLint;
import android.content.ContentUris;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.MediaStore;

import androidx.annotation.NonNull;
import androidx.work.Data;
import androidx.work.ListenableWorker;
import androidx.work.WorkerParameters;
import androidx.work.impl.utils.futures.SettableFuture;

import com.google.common.util.concurrent.ListenableFuture;
import com.zevrant.services.zevrantandroidapp.exceptions.CredentialsNotFoundException;
import com.zevrant.services.zevrantandroidapp.pojo.BackupFileRequest;
import com.zevrant.services.zevrantandroidapp.pojo.CheckExistence;
import com.zevrant.services.zevrantandroidapp.pojo.FileInfo;
import com.zevrant.services.zevrantandroidapp.services.BackupService;
import com.zevrant.services.zevrantandroidapp.services.CredentialsService;
import com.zevrant.services.zevrantandroidapp.services.JsonParser;
import com.zevrant.services.zevrantandroidapp.utilities.JobUtilities;

import org.acra.ACRA;
import org.apache.commons.codec.digest.MessageDigestAlgorithms;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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

    private static final Logger logger = LoggerFactory.getLogger(PhotoBackup.class);
    private SettableFuture<Result> mFuture = null;

    private final String[] projection = new String[]{
            MediaStore.Images.Media._ID,
            MediaStore.Images.Media.DISPLAY_NAME,
            MediaStore.Images.Media.SIZE
    };

    private final Context context;
    private final Data.Builder dataBuilder;

    public PhotoBackup(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
        this.context = context;
        this.dataBuilder = new Data.Builder();
    }

    @NonNull
    @Override
    public ListenableFuture<Result> startWork() {
        mFuture = SettableFuture.create();
        getBackgroundExecutor().execute(() -> {
            Uri uri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
            try (Cursor cursor = context.getContentResolver().query(
                    uri,
                    projection,
                    null,
                    new String[]{},
                    null
            )) {
                logger.info("Job running");
                List<FileInfo> fileInfoList = new ArrayList<>();
                logger.info("found {} images", cursor.getCount());
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
        });
        return mFuture;
    }

    private void backupFiles(List<FileInfo> fileInfoList, Uri uri) throws CredentialsNotFoundException, ExecutionException, InterruptedException {
        logger.info("{} images hashed", fileInfoList.size());
        String authorization = CredentialsService.getAuthorization();
        Future<String> checkExistenceResponse = BackupService.checkExistence(new CheckExistence(fileInfoList), authorization);
        CheckExistence existence = JsonParser.readValueFromString(checkExistenceResponse.get(), CheckExistence.class);
        assert existence != null;
        assert existence.getFileInfos() != null;
        logger.info("Successfully checked existence of file hashes {} files were not found on backup server", existence.getFileInfos().size());
        List<FileInfo> fileInfos = existence.getFileInfos();
        for (int i = 0; i < fileInfos.size(); i++) {
            try {
                sendBackUp(fileInfos.get(i), uri);
            } catch (IOException e) {
                logger.error("Failed to read img file skipping...");
                ACRA.getErrorReporter().handleSilentException(e);
            }
        }
        Data data = dataBuilder.build();
        mFuture.set(Result.success(data));

    }

    private void sendBackUp(FileInfo fileInfo, Uri uri) throws IOException, CredentialsNotFoundException, ExecutionException, InterruptedException {
        BackupFileRequest backupFileRequest = new BackupFileRequest(fileInfo, JobUtilities.bytesToHex(getFileBytes(uri, fileInfo)));
        logger.info(backupFileRequest.toString());
        String authorization = CredentialsService.getAuthorization();
        logger.info("backing up file {}", fileInfo.getFileName());
        Future<String> future = BackupService.backupFile(backupFileRequest, authorization);
        future.get();//don't really care about successfull responses just need to block until done otherwise we
        // will enqueue too many requests and throw an OOM exception
        logger.info("{} was successfully backed up", fileInfo.getFileName());
    }

    private byte[] getFileBytes(Uri uri, FileInfo fileInfo) throws IOException {
        InputStream is = this.context
                .getContentResolver()
                .openAssetFileDescriptor(ContentUris.withAppendedId(uri, fileInfo.getId()), "r")
                .createInputStream();

        byte[] bytes = new byte[(int) fileInfo.getSize()];
        int read = is.read(bytes);
        logger.info(String.valueOf(read));
        assert read == fileInfo.getSize();
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


    public static String getChecksum(MessageDigest digest, InputStream is) throws IOException {
        byte[] bytes = new byte[1024];
        while (is.read(bytes) > -1) {
            digest.update(bytes);
        }

        return JobUtilities.bytesToHex(digest.digest());
    }
}
