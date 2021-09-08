package com.zevrant.services.zevrantandroidapp.steps;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.core.Is.is;

import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Environment;
import android.provider.MediaStore;

import androidx.work.Constraints;
import androidx.work.Data;
import androidx.work.ListenableWorker;
import androidx.work.WorkInfo;
import androidx.work.WorkManager;
import androidx.work.testing.TestDriver;
import androidx.work.testing.TestListenableWorkerBuilder;
import androidx.work.testing.WorkManagerTestInitHelper;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.zevrant.services.zevrantandroidapp.exceptions.CredentialsNotFoundException;
import com.zevrant.services.zevrantandroidapp.jobs.PhotoBackup;
import com.zevrant.services.zevrantandroidapp.pojo.CheckExistence;
import com.zevrant.services.zevrantandroidapp.pojo.FileInfo;
import com.zevrant.services.zevrantandroidapp.services.BackupService;
import com.zevrant.services.zevrantandroidapp.services.CredentialsService;
import com.zevrant.services.zevrantandroidapp.utilities.Constants;
import com.zevrant.services.zevrantandroidapp.utilities.JobUtilities;
import com.zevrant.services.zevrantandroidapp.utilities.TestConstants;

import org.apache.commons.codec.digest.MessageDigestAlgorithms;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import io.cucumber.java.en.And;
import io.cucumber.java.en.Then;

public class PhotoBackupSteps {

    private static final Logger logger = LoggerFactory.getLogger(PhotoBackupSteps.class);

    private void addToList(byte[] bytes, List<Byte> bytesList) {
        for (byte aByte : bytes) {
            bytesList.add(aByte);
        }
    }

    @And("^I add a photo to storage")
    public void addPhotoToStorage() throws IOException {
        final Context context = BasicSteps.getTargetContext();
        final Context testContext = BasicSteps.getTestContext();
        List<Byte> bytesList = new ArrayList<>();
        byte[] bytes = new byte[1024];
        BufferedInputStream is = new BufferedInputStream(testContext.getResources().getAssets().open("human.jpg"));

        int read = is.read(bytes);
        addToList(bytes, bytesList);
        while (read > 0) {
            read = is.read(bytes);
            addToList(bytes, bytesList);
        }
        bytes = new byte[bytesList.size()];
        for (int i = 0; i < bytesList.size(); i++) {
            bytes[i] = bytesList.get(i);
        }
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inMutable = true;
        Bitmap bmp = BitmapFactory.decodeByteArray(bytes, 0, bytes.length, options);
        OutputStream imageOutStream;
        ContentValues values = new ContentValues();
        values.put(MediaStore.Images.Media.DISPLAY_NAME, "image_screenshot_" + LocalDateTime.now().toString() + ".jpg");
        values.put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg");
        values.put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES);
        Uri uri = context.getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);

        imageOutStream = context.getContentResolver().openOutputStream(uri);

        try {
            bmp.compress(Bitmap.CompressFormat.JPEG, 100, imageOutStream);
        } finally {
            imageOutStream.close();
        }

    }

    @And("^I verify the photo was added$")
    public void verifyPhotoAdded() throws NoSuchAlgorithmException, IOException {
        Context context = BasicSteps.getTargetContext();
        Context testContext = BasicSteps.getTestContext();
        final String[] projection = new String[]{
                MediaStore.Images.Media._ID,
                MediaStore.Images.Media.DISPLAY_NAME,
                MediaStore.Images.Media.SIZE
        };
        Uri uri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
        try (Cursor cursor = context.getContentResolver().query(
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
            logger.info("Id column is {}", idColumn);
            assertThat("Negative column value", idColumn, is(greaterThan(-1)));
            long id = cursor.getLong(idColumn);

            MessageDigest digest = MessageDigest.getInstance(MessageDigestAlgorithms.SHA_512);

            InputStream is = context
                    .getContentResolver()
                    .openAssetFileDescriptor(ContentUris.withAppendedId(uri, id), "r")
                    .createInputStream();

            String fileHash = PhotoBackup.getChecksum(digest,
                    is);
            BasicSteps.addContextData("fileHash", fileHash);
        }
    }

    @Then("^I run the photo backup service$")
    public void startBackupService() throws ExecutionException, InterruptedException, TimeoutException {
        Context context = BasicSteps.getTargetContext();
        Constraints constraints = new Constraints.Builder()
                .setTriggerContentMaxDelay(0, TimeUnit.SECONDS)
                .build();

        ListenableWorker.Result result = TestListenableWorkerBuilder.from(context, PhotoBackup.class)
                .build().startWork()
                .get(TestConstants.DEFAULT_TIMEOUT_INTERVAL, TestConstants.DEFAULT_TIMEOUT_UNIT);
        assertThat("WorkerTask did not succeed", result.toString(), is("Success {mOutputData=Data {}}"));
    }

    @And("^I verify the photo was backed up")
    public void verifyPhotoBackup() throws IOException, CredentialsNotFoundException, InterruptedException, ExecutionException, TimeoutException {

        String fileHash = BasicSteps.getContextData("fileHash").toString();
        CheckExistence checkExistence = new CheckExistence();
        checkExistence.setFileInfos(Collections.singletonList(new FileInfo("human.jpg", fileHash, 0L, 0L)));
        Future<String> future =
                BackupService.getAlllHashes(CredentialsService.getAuthorization());
        String responseString = future.get(TestConstants.DEFAULT_TIMEOUT_INTERVAL, TestConstants.DEFAULT_TIMEOUT_UNIT);
        ObjectMapper objectMapper = new ObjectMapper();
        List<String> hashes = objectMapper.readValue(responseString, new TypeReference<List<String>>(){});

        assertThat(hashes.size(), is(equalTo(1)));
        assertThat(hashes.get(0), is(equalTo(fileHash)));
    }
}
