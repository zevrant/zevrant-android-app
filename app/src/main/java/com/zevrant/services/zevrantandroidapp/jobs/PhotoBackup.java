package com.zevrant.services.zevrantandroidapp.jobs;

import android.content.ContentUris;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.MediaStore;

import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.zevrant.services.zevrantandroidapp.pojo.BackupFileRequest;
import com.zevrant.services.zevrantandroidapp.pojo.CheckExistence;
import com.zevrant.services.zevrantandroidapp.pojo.FileInfo;
import com.zevrant.services.zevrantandroidapp.pojo.OAuthToken;
import com.zevrant.services.zevrantandroidapp.services.BackupService;
import com.zevrant.services.zevrantandroidapp.services.JsonParser;
import com.zevrant.services.zevrantandroidapp.services.OAuthService;
import com.zevrant.services.zevrantandroidapp.utilities.JobUtilities;

import org.acra.ACRA;
import org.apache.commons.codec.digest.MessageDigestAlgorithms;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;

public class PhotoBackup extends Worker {

    private static final Logger logger = LoggerFactory.getLogger(PhotoBackup.class);

    private final String[] projection = new String[]{
            MediaStore.Images.Media._ID,
            MediaStore.Images.Media.DISPLAY_NAME,
            MediaStore.Images.Media.SIZE
    };

    private final String username;
    private final String password;

    public PhotoBackup(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
        username = workerParams.getInputData().getString("username");
        password = workerParams.getInputData().getString("password");
    }

    @NonNull
    @Override
    public Result doWork() {
        logger.info("Job running");

        Uri uri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;

        List<FileInfo> fileInfoList = new ArrayList<>();
        try (Cursor cursor = getApplicationContext().getContentResolver().query(
                uri,
                projection,
                null,
                new String[]{},
                null
        )) {
            logger.info("found {} images", cursor.getCount());
            int idColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media._ID);
            int nameColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DISPLAY_NAME);
            int sizeColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.SIZE);
            while (cursor.moveToNext()) {
                processMediaStoreResultSet(idColumn, nameColumn, sizeColumn, cursor, uri, fileInfoList);
            }
        } catch (IOException | NoSuchAlgorithmException e) {
            ACRA.getErrorReporter().handleSilentException(e);
            e.printStackTrace();
        }

        backupFiles(fileInfoList, uri);
        return Result.success();
    }

    private void backupFiles(List<FileInfo> fileInfoList, Uri uri) {
        logger.info("{} images hashed", fileInfoList.size());
        OAuthService.login(username, password, oauthResponse -> {
            //TODO switch to stored token instead of requesting new token every time it's needed
            OAuthToken oAuthToken = JsonParser.readValueFromString(oauthResponse, OAuthToken.class);

            assert oAuthToken != null;
            assert oAuthToken.getAccessToken() != null;

            BackupService.checkExistence(new CheckExistence(fileInfoList), oAuthToken, checkExistenceResponse -> {
                CheckExistence existence = JsonParser.readValueFromString(checkExistenceResponse, CheckExistence.class);
                assert existence != null;
                assert existence.getFileInfos() != null;
                logger.info("Successfully checked existence of file hashes {} files were not found on backup server", existence.getFileInfos().size());
                long i = 1L;
                try {
                    logger.info("retrieved file info");
                    sendBackUp(existence.getFileInfos(), oAuthToken, uri, 0);
                } catch (IOException e) {
                    logger.error("Failed to read img file skipping...");
                    ACRA.getErrorReporter().handleSilentException(e);
                }

            });
        });
    }

    private void sendBackUp(List<FileInfo> fileInfos, OAuthToken oAuthToken, Uri uri, int index) throws IOException {
        if (fileInfos.size() <= index) {
            return;
        }
//        if (!BuildConfig.BUILD_TYPE.equals("release") && index > 0) {
//            return;
//        }
        FileInfo fileInfo = fileInfos.get(index);
        BackupFileRequest backupFileRequest = new BackupFileRequest(fileInfo, JobUtilities.bytesToHex(getFileBytes(uri, fileInfo)));
        logger.info(backupFileRequest.toString());
        BackupService.backupFile(backupFileRequest, oAuthToken, response -> {
            logger.info("{} was successfully backed up", fileInfo.getFileName());
            try {
                sendBackUp(fileInfos, oAuthToken, uri, index + 1);
            } catch (IOException e) {
                logger.error("backup failed in callback for file info {}", fileInfo.getId());
                ACRA.getErrorReporter().handleSilentException(e);
            }
        });
    }

    private byte[] getFileBytes(Uri uri, FileInfo fileInfo) throws IOException {
        InputStream is = getApplicationContext()
                .getContentResolver()
                .openAssetFileDescriptor(ContentUris.withAppendedId(uri, fileInfo.getId()), "r")
                .createInputStream();

        if (is.available() != fileInfo.getSize()) {
            logger.error("Not all bytes available!!!");
            throw new RuntimeException("Not all bytes available!!!");
        } else {
            byte[] bytes = new byte[(int) fileInfo.getSize()];
            int read = is.read(bytes);
            logger.info(String.valueOf(read));
            assert read == fileInfo.getSize();
            return bytes;
        }
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


    private static String getChecksum(MessageDigest digest, InputStream is) throws IOException {
        byte[] bytes = new byte[1024];
        while (is.read(bytes) > -1) {
            digest.update(bytes);
        }

        return JobUtilities.bytesToHex(digest.digest());
    }
}
