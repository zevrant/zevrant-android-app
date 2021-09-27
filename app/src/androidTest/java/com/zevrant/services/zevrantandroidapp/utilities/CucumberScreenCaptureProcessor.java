package com.zevrant.services.zevrantandroidapp.utilities;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;

import android.graphics.Bitmap;

import androidx.test.runner.screenshot.BasicScreenCaptureProcessor;
import androidx.test.runner.screenshot.ScreenCapture;
import androidx.test.runner.screenshot.Screenshot;

import java.io.ByteArrayOutputStream;
import java.io.File;

public class CucumberScreenCaptureProcessor extends BasicScreenCaptureProcessor {

    private final String screenshotDir = "/sdcard/Documents/cucumber-reports/screenshots";

    public CucumberScreenCaptureProcessor() {
        super();
        File screenshotDir = new File(this.screenshotDir);
        if(!screenshotDir.exists()) {
            assertThat("Failed to make screenshot dir", screenshotDir.mkdir(), is(true));
        }
        if(!screenshotDir.isDirectory()) {
            assertThat("Failed to delete non directory file", screenshotDir.delete(), is(true));
            assertThat("Failed to create screenshot directory", screenshotDir.mkdir(), is(true));
        }
        super.mDefaultScreenshotPath = screenshotDir;
    }

    public byte[] takeScreenshot(String screenshotName) {
        ScreenCapture capture = Screenshot.capture();
        if(screenshotName != null) {
            capture.setName(screenshotName);
        }
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        Bitmap.createScaledBitmap(capture.getBitmap(), 200, 400, true)
                .compress(Bitmap.CompressFormat.PNG, 100, stream);
        return stream.toByteArray();
    }

    public String getScreenshotDir() {
        return screenshotDir;
    }
}
