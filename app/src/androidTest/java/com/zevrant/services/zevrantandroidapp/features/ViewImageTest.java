package com.zevrant.services.zevrantandroidapp.features;

import static androidx.test.espresso.Espresso.onData;
import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.withEffectiveVisibility;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;

import androidx.test.espresso.action.ViewActions;
import androidx.test.espresso.matcher.ViewMatchers;
import androidx.test.ext.junit.rules.ActivityScenarioRule;
import androidx.test.platform.app.InstrumentationRegistry;

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
import com.zevrant.services.zevrantandroidapp.utilities.TestConstants;
import com.zevrant.services.zevrantuniversalcommon.services.ChecksumService;
import com.zevrant.services.zevrantuniversalcommon.services.HexConversionService;

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
        photoBackupService = new TestPhotoBackupService();
        photoBackupService.deleteBackups(getTargetContext(), getTestContext(), cleanupService,
                credentialsService, jsonParser, backupService, fileHash);
        photoBackupService.addPhoto(getTargetContext(), getTestContext(), false);
        photoBackupService.verifyPhotoAdded(getTargetContext());
        photoBackupService.backupFile(getTargetContext(), backupService, encryptionService,
                credentialsService, jsonParser, oAuthService);
        assertThat("Photo was reported as backed up but no hashes found", photoBackupService.hasHashes(backupService), is(true));
        activityRule.getScenario().recreate();

    }

    @Test
    public void displayImagesTest() throws Exception {

        Thread.sleep(4000);
        onData(is(not(nullValue())))
                .inAdapterView(withId(R.id.imageList))
                .atPosition(0)
                .check(matches(withEffectiveVisibility(ViewMatchers.Visibility.VISIBLE)));

//        onView(withId(-2 - 1))
//                .check(matches(withEffectiveVisibility(ViewMatchers.Visibility.VISIBLE)))
//                .perform(click());
//        onView(withId(R.id.imageItemLeft))
//                .check(matches(withEffectiveVisibility(ViewMatchers.Visibility.VISIBLE)));

    }

    @Test
    public void displayImagesLoadAdditional() throws Exception {
        photoBackupService.deleteMediaStore(getTargetContext());
        cleanupService.eraseBackups(credentialsService.getAuthorization(), getTargetContext().getString(R.string.backup_base_url), getTargetContext()).get();
        TestConstants.additionalTestImages.forEach(image -> {
            try {
                photoBackupService.addPhoto(getTargetContext(), getTestContext(), false, image);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });
        assertThat("Backups should be empty!", photoBackupService.hasHashes(backupService), is(false));
        photoBackupService.backupFile(getTargetContext(), backupService, encryptionService,
                credentialsService, jsonParser, oAuthService);
        activityRule.getScenario().recreate();
        Thread.sleep(16000);
        onData(is(not(nullValue())))
                .inAdapterView(withId(R.id.imageList))
                .atPosition(5)
                .check(matches(withEffectiveVisibility(ViewMatchers.Visibility.VISIBLE)))
                .perform(ViewActions.swipeUp());
        Thread.sleep(4000);
        onData(is(not(nullValue())))
                .inAdapterView(withId(R.id.imageList))
                .atPosition(6)
                .check(matches(withEffectiveVisibility(ViewMatchers.Visibility.VISIBLE)));
    }
}
