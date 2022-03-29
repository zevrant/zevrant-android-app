package com.zevrant.services.zevrantandroidapp.services;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.core.IsNot.not;

import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Environment;
import android.provider.MediaStore;

import androidx.work.ListenableWorker;
import androidx.work.testing.TestListenableWorkerBuilder;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.zevrant.services.zevrantandroidapp.R;
import com.zevrant.services.zevrantandroidapp.jobs.PhotoBackup;
import com.zevrant.services.zevrantandroidapp.utilities.TestConstants;
import com.zevrant.services.zevrantuniversalcommon.services.ChecksumService;
import com.zevrant.services.zevrantuniversalcommon.services.HexConversionService;

import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.lang3.StringUtils;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

public class TestPhotoBackupService {

    private static final String testFileHash = "2ceb517f7442b31dd1bf08fbcf93a64b5b0533bf0ab5a103670d1c5ed1de0882461d603581cba548b7f7541f427ba0fdc6e0c57058695cf92385fd5422910790";

    public void backupFile(Context targetContext, BackupService backupService, HashingService hashingService,
                           EncryptionService encryptionService, CredentialsService credentialsService,
                           JsonParser jsonParser, OAuthService oAuthService, String fileHash) throws Exception {
        PhotoBackup photoBackup = TestListenableWorkerBuilder.from(targetContext, PhotoBackup.class)
                .build();
        photoBackup.setBackupService(backupService);
        photoBackup.setCredentialsService(credentialsService);
        photoBackup.setEncryptionService(encryptionService);
        photoBackup.setJsonParser(jsonParser);
        photoBackup.setoAuthService(oAuthService);
        ListenableWorker.Result result = photoBackup.startWork().get();

        assertThat("WorkerTask did not succeed", result.toString(), is("Success {mOutputData=Data {}}"));
        Future<String> future =
                backupService.getAlllHashes();
        String responseString = future.get(TestConstants.DEFAULT_TIMEOUT_INTERVAL, TestConstants.DEFAULT_TIMEOUT_UNIT);
        ObjectMapper objectMapper = new ObjectMapper();
        List<String> hashes = objectMapper.readValue(responseString, new TypeReference<>() {
        });

        assertThat("File hash was not found on server", hashes.size(), is(greaterThan(0)));
        assertThat("File hash ".concat(fileHash).concat(" was not found"), hashes.contains(fileHash), is(true));
    }

    public boolean hasHashes(BackupService backupService) throws Exception {
        Future<String> future = backupService.getAlllHashes();
        String responseString = future.get(5, TimeUnit.MINUTES);
        ObjectMapper objectMapper = new ObjectMapper();
        List<String> hashes = objectMapper.readValue(responseString, new TypeReference<>() {
        });
        return hashes.size() > 0;
    }

    public void deleteBackups(Context targetContext, Context testContext, CleanupService cleanupService,
                              CredentialsService credentialsService, JsonParser jsonParser, BackupService backupService,
                              String fileHash) throws Exception{
        Future<String> future = cleanupService.eraseBackups(credentialsService.getAuthorization(), targetContext.getString(R.string.backup_base_url), testContext);
        String deleteResponse = future.get(5, TimeUnit.MINUTES);
        assertThat("Call to delete test hash returned empty", StringUtils.isNotBlank(deleteResponse), is(true));
        assertThat("Delete Call did not return json instead it returned '".concat(deleteResponse).concat("'"), deleteResponse.charAt(0), is('['));
        future = backupService.getAlllHashes();
        String responseString = future.get(5, TimeUnit.MINUTES);
        ObjectMapper objectMapper = new ObjectMapper();
        List<String> hashes = objectMapper.readValue(responseString, new TypeReference<>() {
        });

        assertThat("File hashes were not cleared after test run", hashes.size(), is(0));
    }

    public void verifyPhotoAdded(Context targetContext, ChecksumService checksumService) throws IOException, NoSuchAlgorithmException {
        final String[] projection = new String[]{
                MediaStore.Images.Media._ID,
                MediaStore.Images.Media.DISPLAY_NAME,
                MediaStore.Images.Media.SIZE
        };
        Uri uri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
        try (Cursor cursor = targetContext.getContentResolver().query(
                uri,
                projection,
                null,
                new String[]{},
                null
        )) {
            cursor.moveToFirst();
            assertThat("Media Query Returned No Results", cursor.getCount(), is(greaterThan(0)));
            assertThat("Column Count less than or equal to zero", cursor.getColumnCount(), is(greaterThan(0)));
            int idColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media._ID);
            assertThat("Negative column value", idColumn, is(greaterThan(-1)));
            long id = cursor.getLong(idColumn);

            InputStream is = targetContext
                    .getContentResolver()
                    .openAssetFileDescriptor(ContentUris.withAppendedId(uri, id), "r")
                    .createInputStream();

            byte[] bytes = new byte[1024];
            ByteArrayOutputStream buffer = new ByteArrayOutputStream();
            int read = is.read(bytes);
            while (read > 0) {
//            if(useInvalidPhoto) {
//                buffer.write(bytes);
//            } else {
                buffer.write(bytes, 0, read);
//            }
                read = is.read(bytes);
            }
            bytes = buffer.toByteArray();
            String fileHash = DigestUtils.sha512Hex(bytes);
            assertThat("Input hash did not match preset hash for test image, this is likely a read error second verify", fileHash, is(testFileHash));
        }
    }

    public void addPhoto(Context targetContext, Context testContext, boolean useInvalidPhoto) throws IOException {
        byte[] bytes = new byte[1024];
        BufferedInputStream is = new BufferedInputStream(testContext.getResources().getAssets().open("human.jpg"));
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        int read = is.read(bytes);
        while (read > 0) {
//            if(useInvalidPhoto) {
//                buffer.write(bytes);
//            } else {
                buffer.write(bytes, 0, read);
//            }
            read = is.read(bytes);
        }
        bytes = buffer.toByteArray();
        is.close();

        String hash = DigestUtils.sha512Hex(bytes);

        assertThat("Input hash did not match preset hash for test image, this is likely a read error", hash, is(testFileHash));
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inMutable = true;
        ContentValues values = new ContentValues();
        values.put(MediaStore.Images.Media.DISPLAY_NAME, "image_screenshot_" + LocalDateTime.now().toString() + ".jpg");
        values.put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg");
        values.put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES);
        Uri uri = targetContext.getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);

        try (BufferedOutputStream imageOutStream = new BufferedOutputStream(targetContext.getContentResolver().openOutputStream(uri));) {
            imageOutStream.write(bytes);
            imageOutStream.flush();
        }
    }
}
