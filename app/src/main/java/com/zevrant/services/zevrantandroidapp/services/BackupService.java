package com.zevrant.services.zevrantandroidapp.services;

import static com.zevrant.services.zevrantandroidapp.utilities.Constants.LOG_TAG;

import android.content.Context;
import android.util.Log;

import com.android.volley.Request;
import com.squareup.okhttp.MediaType;
import com.squareup.okhttp.MultipartBuilder;
import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.RequestBody;
import com.squareup.okhttp.Response;
import com.zevrant.services.zevrantandroidapp.BuildConfig;
import com.zevrant.services.zevrantandroidapp.NukeSSLCerts;
import com.zevrant.services.zevrantandroidapp.R;
import com.zevrant.services.zevrantandroidapp.exceptions.CredentialsNotFoundException;
import com.zevrant.services.zevrantandroidapp.utilities.Constants;
import com.zevrant.services.zevrantandroidapp.utilities.ThreadManager;
import com.zevrant.services.zevrantandroidapp.volley.requests.StringRequest;
import com.zevrant.services.zevrantuniversalcommon.rest.backup.request.BackupFileRequest;
import com.zevrant.services.zevrantuniversalcommon.rest.backup.request.CheckExistence;

import org.acra.ACRA;
import org.apache.commons.codec.digest.DigestUtils;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;

import dagger.hilt.android.qualifiers.ApplicationContext;

public class BackupService {

    private final String backupUrl;
    private final JsonParser jsonParser;
    private final CredentialsService credentialsService;
    private final RequestQueueService requestQueueService;
    private final Context context;

    @Inject
    public BackupService(@ApplicationContext Context context, JsonParser jsonParser, CredentialsService credentialsService,
                         RequestQueueService requestQueueService) {
        backupUrl = context.getString(R.string.backup_base_url);
        this.jsonParser = jsonParser;
        this.credentialsService = credentialsService;
        this.requestQueueService = requestQueueService;
        this.context = context;
    }

    public Future<String> checkExistence(CheckExistence checkExistence, @ApplicationContext Context context) {
        CompletableFuture<String> future = new CompletableFuture<>();
        String requestBody = jsonParser.writeValueAsString(checkExistence);
        StringRequest objectRequest = new StringRequest(Request.Method.POST,
                backupUrl.concat("/file-backup/check-existence"), requestBody,
                Constants.DefaultRequestHandlers.getResponseListener(future),
                Constants.DefaultRequestHandlers.getErrorResponseListener(future, credentialsService, requestQueueService, context));
        objectRequest.setOAuthToken(credentialsService.getAuthorization());
        requestQueueService.addToQueue(objectRequest);
        return future;
    }

    public Future<String> backupFile(BackupFileRequest backupFileRequest, BufferedInputStream inputStream) {
        final String attachmentName = "bitmap";
        final String attachmentFileName = "bitmap.bmp";
        final String crlf = "\r\n";
        final String twoHyphens = "--";
        final String boundary = "*****";
        CompletableFuture<String> future = new CompletableFuture<>();
        ThreadManager.execute(() -> {
            try {

                File file = new File(context.getCacheDir().getAbsolutePath().concat(UUID.randomUUID().toString()));
                BufferedOutputStream outputStream = new BufferedOutputStream(new FileOutputStream(file));
                byte[] tempBytes = new byte[1024];
                ByteArrayOutputStream buffer = new ByteArrayOutputStream();
                int read = inputStream.read(tempBytes);
                while (read > 0) {
//            if(useInvalidPhoto) {
//                buffer.write(bytes);
//            } else {
                    buffer.write(tempBytes, 0, read);
//            }
                    read = inputStream.read(tempBytes);
                }
                byte[] bytes = buffer.toByteArray();
                Log.v(LOG_TAG, DigestUtils.sha512Hex(bytes));
                outputStream.write(bytes);
                outputStream.flush();
                outputStream.close();
                OkHttpClient client = new OkHttpClient();
                client.setConnectTimeout(5, TimeUnit.MINUTES);
                client.setReadTimeout(5, TimeUnit.MINUTES);
                client.setRetryOnConnectionFailure(true);
                if(BuildConfig.BUILD_TYPE.contains("local")) {
                    client.setSslSocketFactory(NukeSSLCerts.getSslSocketFactory());
                    client.setHostnameVerifier(((hostname, session) -> true));
                }
                Log.d(LOG_TAG, "form-data; file=\"image\"; fileName=\"".concat(backupFileRequest.getFileInfo().getFileName()).concat("\""));
                String fileName = backupFileRequest.getFileInfo().getFileName();
                RequestBody requestBody = new MultipartBuilder()
                        .type(MultipartBuilder.FORM)
                        .addFormDataPart(
                                "file",
                                fileName,
                                RequestBody.create(MediaType.parse(fileName.split("\\.")[1]), file))
                        .addFormDataPart(
                                "fileName", fileName)
                        .build();

                Log.d(LOG_TAG, "requesting from ".concat(backupUrl.concat("/file-backup")));

                com.squareup.okhttp.Request request = new com.squareup.okhttp.Request.Builder()
                        .header("Authorization", "bearer ".concat(credentialsService.getAuthorization()))
                        .url(backupUrl.concat("/file-backup"))
                        .post(requestBody)
                        .build();

                Response response = client.newCall(request).execute();

                if (!response.isSuccessful()) {
                    future.complete("ResponseMessage: ".concat(response.message()).concat(response.body().string()));
                } else {
                    future.complete("success");
                }
            } catch (IOException e) {
                Log.e(LOG_TAG, e.getLocalizedMessage());
                ACRA.getErrorReporter().handleSilentException(e);
                future.complete(e.getLocalizedMessage());
            }
        });
        return future;
    }

    public Future<String> getAlllHashes() {
        CompletableFuture<String> future = new CompletableFuture<>();
        StringRequest request = new StringRequest(Request.Method.GET,
                backupUrl.concat("/file-backup"),
                null,
                Constants.DefaultRequestHandlers.getResponseListener(future),
                Constants.DefaultRequestHandlers.getErrorResponseListener(future, credentialsService, requestQueueService, context));
        request.setOAuthToken(credentialsService.getAuthorization());
        requestQueueService.addToQueue(request);
        return future;
    }

    public Future<String> getHashCount() {
        CompletableFuture<String> future = new CompletableFuture<>();
        StringRequest request = new StringRequest(Request.Method.GET,
                backupUrl.concat("/retrieval/count"),
                null,
                Constants.DefaultRequestHandlers.getResponseListener(future),
                Constants.DefaultRequestHandlers.getErrorResponseListener(future, credentialsService, requestQueueService, context));
        request.setOAuthToken(credentialsService.getAuthorization());
        requestQueueService.addToQueue(request);
        return future;
    }

    public Future<String> getPhotoPage(int page, int count, int displayWidth, int displayHeight, Context context) {
        CompletableFuture<String> future = new CompletableFuture<>();
        String url = backupUrl.concat("/retrieval/".concat(String.valueOf(page)).concat("/").concat(String.valueOf(count)))
                .concat("?iconWidth=").concat(String.valueOf(displayWidth))
                .concat("&iconHeight=").concat(String.valueOf(displayHeight));

        StringRequest request = new StringRequest(Request.Method.GET,
                url,
                Constants.DefaultRequestHandlers.getResponseListener(future),
                Constants.DefaultRequestHandlers.getErrorResponseListener(future, credentialsService, requestQueueService, context));
        Log.d(LOG_TAG, "setting access token to ".concat(credentialsService.getAuthorization()));
        request.setOAuthToken(credentialsService.getAuthorization());
        request.setTimeout(20000);
        requestQueueService.addToQueue(request);
        return future;
    }

    public Future<InputStream> retrieveFile(String id, int imageWidth, int imageHeight) {
        CompletableFuture<InputStream> future = new CompletableFuture<>();
        String url = backupUrl.concat("/retrieval/".concat(id))
                .concat("?iconWidth=").concat(String.valueOf(imageWidth))
                .concat("&iconHeight=").concat(String.valueOf(imageHeight));
        Log.d(LOG_TAG, "custom request to ".concat(url));
        ThreadManager.execute(() -> {
            try {
                HttpURLConnection conn = (HttpURLConnection) new URL(url).openConnection();
                conn.setRequestMethod("GET");
                conn.setRequestProperty("Authorization", "bearer ".concat(credentialsService.getAuthorization()));
                conn.setConnectTimeout(120000);
                conn.connect();
                InputStream is = null;
                try {
                     is = conn.getInputStream();
                } catch (FileNotFoundException ex) {
                    Log.e(LOG_TAG, "Failed to open connection, retrying...");
                    Thread.yield();
                    is = conn.getInputStream();
                }
                if (conn.getResponseCode() > 399) {
                    future.complete(conn.getErrorStream());
                }
                future.complete(is);
            } catch (IOException e) {
                e.printStackTrace();
                ACRA.getErrorReporter().handleSilentException(e);
                future.complete(new ByteArrayInputStream("Failed to connect to backup service".getBytes(StandardCharsets.UTF_8)));
            }
        });
        return future;
    }
}
