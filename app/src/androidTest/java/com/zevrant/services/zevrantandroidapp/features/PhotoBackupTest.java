package com.zevrant.services.zevrantandroidapp.features;

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

import androidx.test.ext.junit.rules.ActivityScenarioRule;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.work.ListenableWorker;
import androidx.work.testing.TestListenableWorkerBuilder;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.zevrant.services.zevrantandroidapp.R;
import com.zevrant.services.zevrantandroidapp.activities.ZevrantServices;
import com.zevrant.services.zevrantandroidapp.exceptions.CredentialsNotFoundException;
import com.zevrant.services.zevrantandroidapp.jobs.PhotoBackup;
import com.zevrant.services.zevrantandroidapp.services.BackupService;
import com.zevrant.services.zevrantandroidapp.services.CleanupService;
import com.zevrant.services.zevrantandroidapp.services.CredentialsService;
import com.zevrant.services.zevrantandroidapp.test.BaseTest;
import com.zevrant.services.zevrantandroidapp.utilities.TestConstants;
import com.zevrant.services.zevrantuniversalcommon.rest.backup.request.CheckExistence;
import com.zevrant.services.zevrantuniversalcommon.rest.backup.request.FileInfo;

import org.apache.commons.codec.digest.MessageDigestAlgorithms;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeoutException;

@RunWith(AndroidJUnit4.class)

public class PhotoBackupTest extends BaseTest {

    @Rule
    public ActivityScenarioRule<ZevrantServices> activityRule
            = new ActivityScenarioRule<>(ZevrantServices.class);
    private String fileHash;

//    @Rule
//    @JvmField
//    public GrantPermissionRule permissionRule = GrantPermissionRule.grant(Manifest.permission.WRITE_EXTERNAL_STORAGE);

    @Before
    public void setup() throws IllegalAccessException, NoSuchFieldException, IOException {
        super.setup();
//        assertThat("Permission was not granted", ContextCompat.checkSelfPermission(getTargetContext(), Manifest.permission.WRITE_EXTERNAL_STORAGE), is(PackageManager.PERMISSION_GRANTED));
        fileHash = "";
    }

    @After
    public void teardown() throws CredentialsNotFoundException {
        CleanupService.eraseBackups(CredentialsService.getAuthorization(), getTargetContext().getString(R.string.backup_base_url), getTestContext());
    }

    @Test
    public void photoBackupTest() throws InterruptedException, ExecutionException, TimeoutException, IOException, NoSuchAlgorithmException, CredentialsNotFoundException {
        Context context = getTargetContext();
        assertThat(CredentialsService.hasAuthorization(), is(true));
        addPhoto();
        verifyPhotoAdded();
        ListenableWorker.Result result = TestListenableWorkerBuilder.from(context, PhotoBackup.class)
                .build().startWork()
                .get();
        assertThat("WorkerTask did not succeed", result.toString(), is("Success {mOutputData=Data {}}"));
        CheckExistence checkExistence = new CheckExistence();
        checkExistence.setFileInfos(Collections.singletonList(new FileInfo("human.jpg", fileHash, 0L, 0L)));
        Future<String> future =
                BackupService.getAlllHashes();
        String responseString = future.get(TestConstants.DEFAULT_TIMEOUT_INTERVAL, TestConstants.DEFAULT_TIMEOUT_UNIT);
        ObjectMapper objectMapper = new ObjectMapper();
        List<String> hashes = objectMapper.readValue(responseString, new TypeReference<>() {
        });

        assertThat("File hash was not found on server", hashes.size(), is(greaterThan(0)));
        assertThat("File has was not found", hashes.contains(fileHash), is(true));
    }

    private void verifyPhotoAdded() throws IOException, NoSuchAlgorithmException {
        Context context = getTargetContext();
        Context testContext = getTestContext();
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
            assertThat("Negative column value", idColumn, is(greaterThan(-1)));
            long id = cursor.getLong(idColumn);

            MessageDigest digest = MessageDigest.getInstance(MessageDigestAlgorithms.SHA_512);

            InputStream is = context
                    .getContentResolver()
                    .openAssetFileDescriptor(ContentUris.withAppendedId(uri, id), "r")
                    .createInputStream();

            fileHash = PhotoBackup.getChecksum(digest, is);
            assertThat(fileHash, is(not(nullValue())));
        }
    }

    private void addPhoto() throws IOException {
        final Context context = getTargetContext();
        final Context testContext = getTestContext();
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

    private void addToList(byte[] bytes, List<Byte> bytesList) {
        for (byte aByte : bytes) {
            bytesList.add(aByte);
        }
    }
}
