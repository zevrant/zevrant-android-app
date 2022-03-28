package com.zevrant.services.zevrantandroidapp.jobs;

import static com.zevrant.services.zevrantandroidapp.utilities.Constants.LOG_TAG;

import static java.util.stream.Collectors.collectingAndThen;
import static java.util.stream.Collectors.toCollection;

import android.annotation.SuppressLint;
import android.content.ContentUris;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.MediaStore;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.work.Data;
import androidx.work.ListenableWorker;
import androidx.work.WorkerParameters;
import androidx.work.impl.utils.futures.SettableFuture;

import com.google.common.util.concurrent.ListenableFuture;
import com.zevrant.services.zevrantandroidapp.NukeSSLCerts;
import com.zevrant.services.zevrantandroidapp.exceptions.CredentialsNotFoundException;
import com.zevrant.services.zevrantandroidapp.services.BackupService;
import com.zevrant.services.zevrantandroidapp.services.CredentialsService;
import com.zevrant.services.zevrantandroidapp.services.EncryptionService;
import com.zevrant.services.zevrantandroidapp.services.HashingService;
import com.zevrant.services.zevrantandroidapp.services.JsonParser;
import com.zevrant.services.zevrantandroidapp.services.OAuthService;
import com.zevrant.services.zevrantandroidapp.utilities.Constants;
import com.zevrant.services.zevrantuniversalcommon.contants.Roles;
import com.zevrant.services.zevrantuniversalcommon.rest.backup.request.BackupFileRequest;
import com.zevrant.services.zevrantuniversalcommon.rest.backup.request.CheckExistence;
import com.zevrant.services.zevrantuniversalcommon.rest.backup.request.FileInfo;

import org.acra.ACRA;
import org.apache.commons.codec.digest.MessageDigestAlgorithms;
import org.apache.commons.lang3.StringUtils;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.TreeSet;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import javax.inject.Inject;

@SuppressLint("RestrictedApi")
public class PhotoBackup extends ListenableWorker {

    private final String[] projection = new String[]{
            MediaStore.Images.Media._ID,
            MediaStore.Images.Media.DISPLAY_NAME,
            MediaStore.Images.Media.SIZE
    };
    private final Context context;
    private final Data.Builder dataBuilder;
    private SettableFuture<Result> mFuture = null;
    private BackupService backupService;
    private EncryptionService encryptionService;
    private HashingService hashingService;
    private OAuthService oAuthService;
    private JsonParser jsonParser;
    private CredentialsService credentialsService;

    public PhotoBackup(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
        this.context = context;
        this.dataBuilder = new Data.Builder();
    }

    @Inject
    public void setBackupService(BackupService backupService) {
        this.backupService = backupService;
    }

    @Inject
    public void setEncryptionService(EncryptionService encryptionService) {
        this.encryptionService = encryptionService;
    }

    @Inject
    public void setHashingService(HashingService hashingService) {
        this.hashingService = hashingService;
    }

    @Inject
    public void setoAuthService(OAuthService oAuthService) {
        this.oAuthService = oAuthService;
    }

    @Inject
    public void setJsonParser(JsonParser jsonParser) {
        this.jsonParser = jsonParser;
    }

    @Inject
    public void setCredentialsService(CredentialsService credentialsService) {
        this.credentialsService = credentialsService;
    }

    @NonNull
    @Override
    public ListenableFuture<Result> startWork() {
        NukeSSLCerts.nuke();
        mFuture = SettableFuture.create();
        if (!encryptionService.hasSecret(Constants.SecretNames.REFRESH_TOKEN_1)) {
            //TODO send notification prompting user login
            Log.e("User not logged in", LOG_TAG);
            mFuture.set(Result.failure());
            return mFuture;
        }
        getBackgroundExecutor().execute(() -> {
            boolean hasBackupPermission = false;
            hasBackupPermission = oAuthService.canI(Roles.BACKUPS, context, credentialsService.getAuthorization(), credentialsService);
            if (!hasBackupPermission) {
                dataBuilder.putString("reason", "user does not have permission to access backups");
                mFuture.set(Result.failure(dataBuilder.build()));
            } else {
                Uri uri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
                try (Cursor cursor = context.getContentResolver().query(
                        uri,
                        projection,
                        null,
                        new String[]{},
                        null
                )) {
                    Log.i(LOG_TAG, "Job running");
                    List<FileInfo> fileInfoList = new ArrayList<>();
                    Log.i(LOG_TAG, "found ".concat(String.valueOf(cursor.getCount()).concat(" images")));
                    int idColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media._ID);
                    int nameColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DISPLAY_NAME);
                    int sizeColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.SIZE);
                    while (cursor.moveToNext()) {
                        processMediaStoreResultSet(idColumn, nameColumn, sizeColumn, cursor, uri, fileInfoList);
                    }
                    fileInfoList = fileInfoList.stream()
                            .collect(collectingAndThen(toCollection(() -> new TreeSet<>(Comparator.comparing(FileInfo::getHash))),
                                    ArrayList::new));
                    backupFiles(fileInfoList, uri);
                } catch (IOException | NoSuchAlgorithmException | CredentialsNotFoundException | ExecutionException | InterruptedException e) {
                    ACRA.getErrorReporter().handleSilentException(e);
                    e.printStackTrace();
                    mFuture.set(Result.failure());
                }
            }
        });
        return mFuture;
    }

    private void backupFiles(List<FileInfo> fileInfoList, Uri uri) throws CredentialsNotFoundException, ExecutionException, InterruptedException {
        Log.i(LOG_TAG, String.valueOf(fileInfoList.size()).concat(" images hashed"));
        Future<String> checkExistenceResponse = backupService.checkExistence(new CheckExistence(fileInfoList), context);
        CheckExistence existence = jsonParser.readValueFromString(checkExistenceResponse.get(), CheckExistence.class);
        assert existence != null : "null value returned from existence check for photo backup";
        assert existence.getFileInfos() != null : "null was returned for list of fileinfos in photobackup";
        Log.i(LOG_TAG, "Successfully checked existence of file hashes ".concat(String.valueOf(existence.getFileInfos().size()).concat(" files were not found on backup server")));
        List<FileInfo> fileInfos = existence.getFileInfos();
        for (int i = 0; i < fileInfos.size(); i++) {
            try {
                try {
                    sendBackUp(fileInfos.get(i), uri);
                } catch (AssertionError assertionError) {
                    Log.d(LOG_TAG, "Backup Failed, retrying...");
                    Thread.sleep(3000);
                    sendBackUp(fileInfos.get(i), uri);
                }
                Thread.sleep( 3000);
            } catch (IOException | TimeoutException e) {
                Log.e(LOG_TAG, "Failed to read img file skipping...");
                ACRA.getErrorReporter().handleSilentException(e);
            }
        }
        Data data = dataBuilder.build();
        mFuture.set(Result.success(data));

    }

    private void sendBackUp(FileInfo fileInfo, Uri uri) throws IOException, ExecutionException, InterruptedException, TimeoutException {
        BackupFileRequest backupFileRequest = new BackupFileRequest();
        backupFileRequest.setFileInfo(fileInfo);
        Log.i(LOG_TAG, backupFileRequest.toString());
        Log.i(LOG_TAG, "backing up file ".concat(fileInfo.getFileName()));
        InputStream is = this.context
                .getContentResolver()
                .openAssetFileDescriptor(ContentUris.withAppendedId(uri, fileInfo.getId()), "r")
                .createInputStream();
        Future<String> future = backupService.backupFile(backupFileRequest, new BufferedInputStream(is));
        String results = future.get(15, TimeUnit.MINUTES);//don't really care about successful responses just need to block until done otherwise we
                                                            // will enqueue too many requests and throw an OOM exception
        Log.d(LOG_TAG, results);
        assert results.equals("success"): "File backup unsuccessful ".concat(results);

        Log.i(LOG_TAG, fileInfo.getFileName().concat(" was successfully backed up"));
    }

    private byte[] getFileBytes(Uri uri, FileInfo fileInfo) throws IOException {
        InputStream is = this.context
                .getContentResolver()
                .openAssetFileDescriptor(ContentUris.withAppendedId(uri, fileInfo.getId()), "r")
                .createInputStream();

        byte[] bytes = new byte[(int) fileInfo.getSize()];
        int read = is.read(bytes);
        Log.i(LOG_TAG, String.valueOf(read));
        assert read == fileInfo.getSize() : "failed to read entire contents of file in photo backup";
        return bytes;
    }

    private void processMediaStoreResultSet(int idColumn, int nameColumn, int sizeColumn, Cursor cursor, Uri uri, List<FileInfo> fileInfoList) throws NoSuchAlgorithmException, IOException {
        long id = cursor.getLong(idColumn);
        String name = cursor.getString(nameColumn);
        long size = cursor.getLong(sizeColumn);
        MessageDigest digest = MessageDigest.getInstance(MessageDigestAlgorithms.SHA_512);

        InputStream is = getApplicationContext().getContentResolver()
                .openAssetFileDescriptor(ContentUris.withAppendedId(uri, id), "r")
                .createInputStream();
        String hash = hashingService.getSha512Checksum(is);
        if(StringUtils.isBlank(hash)) {
            Log.i(LOG_TAG,"Not adding file info, hash was blank");
        } else {
            fileInfoList.add(new FileInfo(name, hash, id, size));
        }
        // Stores column values and the contentUri in a local object
        // that represents the media file.
        is.close();
    }
}
