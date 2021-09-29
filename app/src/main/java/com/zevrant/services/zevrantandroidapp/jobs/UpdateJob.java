package com.zevrant.services.zevrantandroidapp.jobs;

import static org.acra.ACRA.LOG_TAG;

import android.annotation.SuppressLint;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageInstaller;
import android.content.pm.PackageManager;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.work.ListenableWorker;
import androidx.work.WorkerParameters;
import androidx.work.impl.utils.futures.SettableFuture;

import com.google.common.util.concurrent.ListenableFuture;
import com.zevrant.services.zevrantandroidapp.R;
import com.zevrant.services.zevrantandroidapp.activities.ZevrantServices;
import com.zevrant.services.zevrantandroidapp.exceptions.CredentialsNotFoundException;
import com.zevrant.services.zevrantandroidapp.pojo.UpdateCheckResponse;
import com.zevrant.services.zevrantandroidapp.services.CredentialsService;
import com.zevrant.services.zevrantandroidapp.services.JsonParser;
import com.zevrant.services.zevrantandroidapp.services.UpdateService;
import com.zevrant.services.zevrantandroidapp.utilities.JobUtilities;

import org.acra.ACRA;
import org.apache.commons.lang3.exception.ExceptionUtils;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

@SuppressLint("RestrictedApi")
public class UpdateJob extends ListenableWorker {

    private final Context context;
    private final String username;
    private final String password;

    private final SettableFuture<Result> mFuture;

    public UpdateJob(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
        this.context = context;
        username = workerParams.getInputData().getString("username");
        password = workerParams.getInputData().getString("password");
        mFuture = SettableFuture.create();
    }

    @NonNull
    @Override
    public ListenableFuture<Result> startWork() {
        Executors.newCachedThreadPool().submit(() -> {
            try {
                SharedPreferences sharedPreferences = context.getSharedPreferences("zevrant-services-preferences", Context.MODE_PRIVATE);
                String authorization = CredentialsService.getAuthorization();
                UpdateCheckResponse updateCheckResponse = isUpdateAvailable(authorization, sharedPreferences);
                if (updateCheckResponse.isNewVersionAvailable()) {
                    Log.i(LOG_TAG, "A newer version is available v".concat(updateCheckResponse.getLatestVersion()));
                    Future<InputStream> apkResponse = UpdateService.downloadVersion(updateCheckResponse.getLatestVersion(), authorization);
                    Log.i(LOG_TAG, "update downloaded");
                    installApk(apkResponse.get());
                    SharedPreferences.Editor editor = sharedPreferences.edit();
                    editor.putString(context.getString(R.string.version), updateCheckResponse.getLatestVersion()).apply();
                }
                mFuture.set(Result.success());
            } catch (IOException | InterruptedException | ExecutionException | CredentialsNotFoundException e) {
                Log.e(LOG_TAG,  Objects.requireNonNull(e.getMessage()).concat(" \n ").concat(ExceptionUtils.getStackTrace(e)));
                ACRA.getErrorReporter().handleSilentException(e);
                mFuture.setException(e);
            }
        });
        return mFuture;
    }

    private UpdateCheckResponse isUpdateAvailable(String authorization, SharedPreferences sharedPreferences)
            throws CredentialsNotFoundException, ExecutionException, InterruptedException {
        Log.i(LOG_TAG, "Checking for updates");
        Log.i(LOG_TAG, "default version is ".concat(getDefaultVersion()));
        String version = sharedPreferences.getString(context.getString(R.string.version), getDefaultVersion());
        Future<String> updateResponseFuture = UpdateService.isUpdateAvailable(version, authorization);
        String updateResponse = updateResponseFuture.get();
        Log.i(LOG_TAG, "Update response received ".concat(updateResponse));
        UpdateCheckResponse updateCheckResponse = JsonParser.readValueFromString(updateResponse, UpdateCheckResponse.class);
        assert updateCheckResponse != null : "null value returned for update check response";
        return updateCheckResponse;
    }

    private String getDefaultVersion() {
        PackageInfo pInfo = null;
        try {
            pInfo = context.getPackageManager().getPackageInfo(context.getPackageName(), 0);
        } catch (PackageManager.NameNotFoundException e) {
            ACRA.getErrorReporter().handleSilentException(e);
            mFuture.setException(e);
        }
        assert pInfo != null : "failed to retrieve version info from package manager";
        return pInfo.versionName;
    }

    private void installApk(InputStream is) throws IOException {

        //Target sdk version is 30 so we don't need to check if android version is Q(version 29) or higher
        PackageInstaller installer = context.getPackageManager().getPackageInstaller();
        int sessionId = installer.createSession(new PackageInstaller.SessionParams(PackageInstaller.SessionParams.MODE_FULL_INSTALL));
        PackageInstaller.Session session = installer.openSession(sessionId);
        OutputStream outputStream = session.openWrite("zevrant-services-".concat(UUID.randomUUID().toString()).concat(".apk"), 0L, 5120000);
        long byteCount = JobUtilities.copyData(is, outputStream);
        Log.d(LOG_TAG, "wrote ".concat(String.valueOf(byteCount)).concat(" bytes to session"));
        session.fsync(outputStream);
        outputStream.close();
        Intent receiver = new Intent(context, ZevrantServices.class);
        receiver.setAction("com.example.android.apis.content.SESSION_API_PACKAGE_INSTALLED");
        PendingIntent pendingIntent = PendingIntent.getActivity(context, 0, receiver, 0);

        Log.i(LOG_TAG, "commiting install");
        session.commit(pendingIntent.getIntentSender());
        session.close();
    }
}
