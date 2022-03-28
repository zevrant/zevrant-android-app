package com.zevrant.services.zevrantandroidapp.features;

import static androidx.test.espresso.Espresso.onData;
import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.withEffectiveVisibility;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.platform.app.InstrumentationRegistry.getInstrumentation;

import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;

import androidx.lifecycle.Lifecycle;
import androidx.test.espresso.Espresso;
import androidx.test.espresso.action.ViewActions;
import androidx.test.espresso.assertion.ViewAssertions;
import androidx.test.espresso.matcher.ViewMatchers;
import androidx.test.ext.junit.rules.ActivityScenarioRule;

import com.zevrant.services.zevrantandroidapp.R;
import com.zevrant.services.zevrantandroidapp.activities.ZevrantServices;
import com.zevrant.services.zevrantandroidapp.services.BackupService;
import com.zevrant.services.zevrantandroidapp.services.CleanupService;
import com.zevrant.services.zevrantandroidapp.services.EncryptionService;
import com.zevrant.services.zevrantandroidapp.services.HashingService;
import com.zevrant.services.zevrantandroidapp.services.JsonParser;
import com.zevrant.services.zevrantandroidapp.services.OAuthService;
import com.zevrant.services.zevrantandroidapp.services.TestPhotoBackupService;
import com.zevrant.services.zevrantandroidapp.test.BaseTest;
import com.zevrant.services.zevrantuniversalcommon.services.ChecksumService;
import com.zevrant.services.zevrantuniversalcommon.services.HexConversionService;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import javax.inject.Inject;

import dagger.hilt.android.testing.HiltAndroidRule;
import dagger.hilt.android.testing.HiltAndroidTest;

@HiltAndroidTest
public class ViewImageTest extends BaseTest {

    @Rule(order = 1)
    public ActivityScenarioRule<ZevrantServices> activityRule
            = new ActivityScenarioRule<>(ZevrantServices.class);

    @Rule(order = 0)
    public HiltAndroidRule hiltRule = new HiltAndroidRule(this);

    private TestPhotoBackupService photoBackupService;

    @Inject
    OAuthService oAuthService;

    @Inject
    BackupService backupService;

    @Inject
    EncryptionService encryptionService;

    @Inject
    JsonParser jsonParser;

    @Inject
    HashingService hashingService;

    @Inject
    CleanupService cleanupService;
    public static final String fileHash = "2ceb517f7442b31dd1bf08fbcf93a64b5b0533bf0ab5a103670d1c5ed1de0882461d603581cba548b7f7541f427ba0fdc6e0c57058695cf92385fd5422910790";

    @Before
    public void setup() throws Exception {
        hiltRule.inject();
        super.setup(activityRule);
        ChecksumService checksumService = new ChecksumService(new HexConversionService());
        photoBackupService = new TestPhotoBackupService();
        if(!photoBackupService.hasHashes(backupService)) {
            photoBackupService.addPhoto(getTargetContext(), getTestContext(), false);
            photoBackupService.verifyPhotoAdded(getTargetContext(), checksumService);
            photoBackupService.backupFile(getTargetContext(), backupService, hashingService, encryptionService,
                    credentialsService, jsonParser, oAuthService, fileHash);
            assertThat("Photo was reported as backed up but no hashes found", photoBackupService.hasHashes(backupService), is(true));
            activityRule.getScenario().recreate();
        }
    }

    @Test
    public void DisplayImagesTest() throws Exception {
        Thread.sleep(4000);
        onData(is(not(nullValue())))
                .inAdapterView(withId(R.id.imageList))
                .atPosition(0)
                .check(matches(withEffectiveVisibility(ViewMatchers.Visibility.VISIBLE)));
//        onView(withId(R.id.imageItemLeft))
//                .check(matches(withEffectiveVisibility(ViewMatchers.Visibility.VISIBLE)));

    }
}
