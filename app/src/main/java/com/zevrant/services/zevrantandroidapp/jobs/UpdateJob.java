package com.zevrant.services.zevrantandroidapp.jobs;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.IntentSender;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageInstaller;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.provider.Settings;

import androidx.annotation.NonNull;
import androidx.core.content.FileProvider;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.zevrant.services.zevrantandroidapp.R;
import com.zevrant.services.zevrantandroidapp.activities.ZevrantServices;
import com.zevrant.services.zevrantandroidapp.pojo.OAuthToken;
import com.zevrant.services.zevrantandroidapp.pojo.UpdateCheckResponse;
import com.zevrant.services.zevrantandroidapp.services.JsonParser;
import com.zevrant.services.zevrantandroidapp.services.OAuthService;
import com.zevrant.services.zevrantandroidapp.services.UpdateService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.UUID;

public class UpdateJob extends Worker {

    private static final Logger logger = LoggerFactory.getLogger(UpdateJob.class);

    private final Context context;
    private final String username;
    private final String password;

    public UpdateJob(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
        this.context = context;
        username = workerParams.getInputData().getString("username");
        password = workerParams.getInputData().getString("password");
    }

    @NonNull
    @Override
    public Result doWork() {
        OAuthService.login(username, password, oauthResponse -> {
            //TODO switch to stored token instead of requesting new token every time it's needed
            OAuthToken oAuthToken = JsonParser.readValueFromString(oauthResponse, OAuthToken.class);
            assert oAuthToken != null;
            logger.info("Checking for updates");
            PackageInfo pInfo = null;
            try {
                pInfo = context.getPackageManager().getPackageInfo(context.getPackageName(), 0);
            } catch (PackageManager.NameNotFoundException e) {
                e.printStackTrace();
            }
            assert pInfo != null;
            SharedPreferences sharedPreferences = context.getSharedPreferences("zevrant-services-preferences", Context.MODE_PRIVATE);
            logger.info("default version is {}", pInfo.versionName);
            String version = sharedPreferences.getString(context.getString(R.string.version), pInfo.versionName);
            UpdateService.isUpdateAvailable(version, oAuthToken, updateResponse -> {
                logger.info("Update response received {}", updateResponse);
                UpdateCheckResponse updateCheckResponse = JsonParser.readValueFromString(updateResponse, UpdateCheckResponse.class);
                assert updateCheckResponse != null;
                if(updateCheckResponse.isNewVersionAvailable()) {
                    logger.info("A newer version is available v{}", updateCheckResponse.getLatestVersion());
                    UpdateService.downloadVersion(updateCheckResponse.getLatestVersion(), oAuthToken, newVersionResponse -> {
                        logger.info("update downloaded");
                        File filesDir = context.getFilesDir();
                        File apkFile = new File(filesDir.getAbsolutePath().concat("newApk.apk"));
                        try(OutputStream outputStream = new FileOutputStream(apkFile)) {
                            int byteCount = 0;
                            byte[] bytes = new byte[1024];
                            int bytesRead = newVersionResponse.read(bytes);
                            while(bytesRead >= 0) {
                                byteCount += bytesRead;
                                outputStream.write(bytes);
                                bytes = new byte[1024];
                                bytesRead = newVersionResponse.read(bytes);
                            }
                            outputStream.flush();
                            outputStream.close();
                            logger.info("file written, number of bytes was {}", byteCount);
                            installApk(apkFile);
                            SharedPreferences.Editor editor = sharedPreferences.edit();
                            editor.putString(context.getString(R.string.version), updateCheckResponse.getLatestVersion()).commit();
                        } catch (IOException e) {
                            e.printStackTrace();
                        } finally {
                            if(apkFile.exists()) {
                                if(!apkFile.delete()) {
                                    logger.error("Failed to delete apk file");
                                } else {
                                    logger.info("apk file deleted");
                                }
                            }
                        }
                    });
                }
            });
        });

        return Result.success();
    }

    private void installApk(File apkFile) throws IOException {

        if (apkFile.exists() && apkFile.canRead() && apkFile.getTotalSpace() > 0) {
            //Target sdk version is 30 so we don't need to check if android version is Q(version 29) or higher
            PackageInstaller installer = context.getPackageManager().getPackageInstaller();
            int sessionId = installer.createSession(new PackageInstaller.SessionParams(PackageInstaller.SessionParams.MODE_FULL_INSTALL));
            PackageInstaller.Session session = installer.openSession(sessionId);
            logger.info("writing {} bytes to session", apkFile.length());
            OutputStream outputStream = session.openWrite("zevrant-services-".concat(UUID.randomUUID().toString()).concat(".apk"), 0L, apkFile.length());
            InputStream is = new FileInputStream(apkFile);
            int byteCount = 0;
            byte[] bytes = new byte[1024];
            int bytesRead = is.read(bytes);
            while(bytesRead >= 0) {
                byteCount += bytesRead;
                outputStream.write(bytes);
                bytes = new byte[1024];
                bytesRead = is.read(bytes);
            }
            logger.debug("wrote {} bytes to session", byteCount);
            session.fsync(outputStream);
            outputStream.close();
            Intent receiver = new Intent(context, ZevrantServices.class);
            receiver.setAction("com.example.android.apis.content.SESSION_API_PACKAGE_INSTALLED");
            PendingIntent pendingIntent = PendingIntent.getActivity(context, 0, receiver, 0);

            logger.info("commiting install");
            session.commit(pendingIntent.getIntentSender());
            session.close();
        } else {
            logger.error("Failed to find or read downloaded apk file, or the file may be empty");
            throw new RuntimeException("Failed to find or read downloaded apk file, or the file may be empty");
        }
    }
}
