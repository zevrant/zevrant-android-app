package com.zevrant.services.zevrantandroidapp.steps;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.core.Is.is;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.MediaStore;

import androidx.work.Constraints;
import androidx.work.Data;
import androidx.work.Operation;

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

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import io.cucumber.java.en.And;
import io.cucumber.java.en.Then;

public class PhotoBackupSteps {

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
    public void verifyPhotoAdded() {
        Context context = BasicSteps.getTargetContext();
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
            assertThat(cursor.getCount(), is(greaterThan(0)));
        }
    }

    @Then("^I run the photo backup service$")
    public void startBackupService() throws ExecutionException, InterruptedException, TimeoutException {
        Context context = BasicSteps.getTargetContext();
        Constraints constraints = new Constraints.Builder()
                .setTriggerContentMaxDelay(1, TimeUnit.SECONDS)
                .build();

        Data data = new Data.Builder()
                .build();

        Operation operation =
                JobUtilities.scheduleJob(context, PhotoBackup.class, constraints, Constants.BACKUP_TAG, data);
        Operation.State state = operation.getResult().get(TestConstants.DEFAULT_TIMEOUT_INTERVAL, TestConstants.DEFAULT_TIMEOUT_UNIT);

        assertThat(state.toString(), is(equalTo("SUCCESS")));
    }

    @And("^I verify the photo was backed up")
    public void verifyPhotoBackup() throws NoSuchAlgorithmException, IOException, CredentialsNotFoundException, InterruptedException, ExecutionException, TimeoutException {
        MessageDigest digest = MessageDigest.getInstance(MessageDigestAlgorithms.SHA_512);
        Context testContext = BasicSteps.getTestContext();
        String fileHash = PhotoBackup.getChecksum(digest,
                new BufferedInputStream(testContext.getResources().getAssets().open("human.jpg")));
        CheckExistence checkExistence = new CheckExistence();
        checkExistence.setFileInfos(Collections.singletonList(new FileInfo("human.jpg", fileHash, 0L, 0L)));
        Future<String> future =
                BackupService.checkExistence(checkExistence, CredentialsService.getAuthorization());
        String responseString = future.get(TestConstants.DEFAULT_TIMEOUT_INTERVAL, TestConstants.DEFAULT_TIMEOUT_UNIT);
        ObjectMapper objectMapper = new ObjectMapper();
        CheckExistence checkExistenceResponse = objectMapper.readValue(responseString, CheckExistence.class);
        assertThat(checkExistenceResponse.getFileInfos().size(), is(equalTo(0)));
    }
}
