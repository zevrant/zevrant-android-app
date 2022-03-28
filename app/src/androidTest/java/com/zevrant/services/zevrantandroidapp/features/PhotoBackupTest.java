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
import com.zevrant.services.zevrantandroidapp.services.EncryptionService;
import com.zevrant.services.zevrantandroidapp.services.HashingService;
import com.zevrant.services.zevrantandroidapp.services.OAuthService;
import com.zevrant.services.zevrantandroidapp.services.TestPhotoBackupService;
import com.zevrant.services.zevrantandroidapp.test.BaseTest;
import com.zevrant.services.zevrantandroidapp.utilities.TestConstants;
import com.zevrant.services.zevrantuniversalcommon.services.ChecksumService;
import com.zevrant.services.zevrantuniversalcommon.services.HexConversionService;

import org.apache.commons.lang3.StringUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeoutException;

import javax.inject.Inject;

import dagger.hilt.android.testing.HiltAndroidRule;
import dagger.hilt.android.testing.HiltAndroidTest;

@HiltAndroidTest
public class PhotoBackupTest extends BaseTest {

    @Rule(order = 1)
    public ActivityScenarioRule<ZevrantServices> activityRule
            = new ActivityScenarioRule<>(ZevrantServices.class);

    @Rule(order = 0)
    public HiltAndroidRule hiltRule = new HiltAndroidRule(this);

    @Inject
    BackupService backupService;

    @Inject
    HashingService hashingService;

    @Inject
    OAuthService oAuthService;

    @Inject
    CleanupService cleanupService;

    private TestPhotoBackupService photoBackupService;

    private ChecksumService checksumService;
    public static final String fileHash = "2ceb517f7442b31dd1bf08fbcf93a64b5b0533bf0ab5a103670d1c5ed1de0882461d603581cba548b7f7541f427ba0fdc6e0c57058695cf92385fd5422910790";

//    @Rule
//    @JvmField
//    public GrantPermissionRule permissionRule = GrantPermissionRule.grant(Manifest.permission.WRITE_EXTERNAL_STORAGE);

    @Before
    public void setup() throws Exception {
        hiltRule.inject();
        super.setup(activityRule);
        checksumService = new ChecksumService(new HexConversionService());
        photoBackupService = new TestPhotoBackupService();
        photoBackupService.deleteBackups(getTargetContext(), getTestContext(), cleanupService,
                credentialsService, jsonParser, backupService, fileHash);
    }

    @After
    public void teardown() throws Exception {
        if(photoBackupService != null) {
            photoBackupService.deleteBackups(getTargetContext(), getTestContext(), cleanupService,
                    credentialsService, jsonParser, backupService, fileHash);
        }
    }

    //TODO: investigate why file has calculated on android is different than spring
    @Test
    public void photoBackupTest() throws InterruptedException, ExecutionException, TimeoutException, IOException, NoSuchAlgorithmException, CredentialsNotFoundException {

        Context context = getTargetContext();
        photoBackupService.addPhoto(getTargetContext(), getTestContext(), false);
        photoBackupService.verifyPhotoAdded(getTargetContext(), checksumService);
        PhotoBackup photoBackup = TestListenableWorkerBuilder.from(context, PhotoBackup.class)
                .build();
        photoBackup.setBackupService(backupService);
        photoBackup.setCredentialsService(credentialsService);
        photoBackup.setHashingService(hashingService);
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

        assertThat("File hash was not found on server", hashes.size(), is(1));
        assertThat("File hash ".concat(fileHash).concat(" was not found"), hashes.get(0), is(fileHash));
    }


}
