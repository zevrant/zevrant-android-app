package com.zevrant.services.zevrantandroidapp.utilities;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;

import androidx.test.runner.screenshot.BasicScreenCaptureProcessor;
import androidx.test.runner.screenshot.ScreenCapture;
import androidx.test.runner.screenshot.Screenshot;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.Set;

public class CucumberScreenCaptureProcessor extends BasicScreenCaptureProcessor {

    public CucumberScreenCaptureProcessor() {
        super();
        File screenshotDir = new File("/sdcard/Documents/cucumber-reports/screenshots");
        if(!screenshotDir.exists()) {
            assertThat("Failed to make screenshot dir", screenshotDir.mkdir(), is(true));
        }
        if(!screenshotDir.isDirectory()) {
            assertThat("Failed to delete non directory file", screenshotDir.delete(), is(true));
            assertThat("Failed to create screenshot directory", screenshotDir.mkdir(), is(true));
        }
        super.mDefaultScreenshotPath = screenshotDir;
    }

    public void takeScreenshot(String screenshotName) {
        ScreenCapture capture = Screenshot.capture();
        capture.setName(screenshotName);
        try {
            this.process(capture);
        } catch (IOException e) {
            e.printStackTrace();
            throw new RuntimeException("failed to take screenshot");
        }
    }
}
