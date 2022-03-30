package com.zevrant.services.zevrantandroidapp.fragments;

import static com.zevrant.services.zevrantandroidapp.utilities.Constants.LOG_TAG;
import static com.zevrant.services.zevrantandroidapp.utilities.ImageUtilities.convertDpToPx;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.ListView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.zevrant.services.zevrantandroidapp.R;
import com.zevrant.services.zevrantandroidapp.activities.ZevrantServices;
import com.zevrant.services.zevrantandroidapp.adapters.ImageListAdapter;
import com.zevrant.services.zevrantandroidapp.fragments.dialogs.ProcessingDialog;
import com.zevrant.services.zevrantandroidapp.pojo.BackupFilePair;
import com.zevrant.services.zevrantandroidapp.services.BackupService;
import com.zevrant.services.zevrantandroidapp.services.JsonParser;
import com.zevrant.services.zevrantandroidapp.utilities.Constants;
import com.zevrant.services.zevrantandroidapp.utilities.ImageUtilities;
import com.zevrant.services.zevrantandroidapp.utilities.ThreadManager;
import com.zevrant.services.zevrantuniversalcommon.rest.backup.response.BackupFile;
import com.zevrant.services.zevrantuniversalcommon.rest.backup.response.BackupFilesRetrieval;

import org.acra.ACRA;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.regex.Pattern;

import javax.inject.Inject;

import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class MediaViewer extends Fragment {

    private int pagesDisplayed = 0;
    private SwipeRefreshLayout scrollView;
    private View parentView;
    private ListView listView;
    private ViewGroup viewGroup;
    private int maxPages;
    private BackupService backupService;
    private JsonParser jsonParser;
    private String itemsPerPage = "12";
    private int displayWidithPixels;
    private int displayHeightPixels;
    private boolean ready = false;
    private ProcessingDialog processingDialog = ProcessingDialog.newInstance();

    private synchronized boolean isReady(boolean update, boolean value) {
        if (update) {
            ready = value;
        }
        return ready;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        this.viewGroup = container;
        return inflater.inflate(R.layout.fragment_scrolling, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        parentView = view;
        scrollView = view.findViewById(R.id.mediaScrollView);
        scrollView.setProgressBackgroundColorSchemeColor(getResources().getColor(R.color.purple_angular, ZevrantServices.getStaticTheme()));
        listView = scrollView.findViewById(R.id.imageList);
        displayWidithPixels = convertDpToPx(Constants.MediaViewerControls.MAX_WIDTH_DP, getResources());
        displayHeightPixels = convertDpToPx(Constants.MediaViewerControls.MAX_HIEGHT_DP, getResources());
        Log.d(LOG_TAG, "Display height pixels = ".concat(String.valueOf(displayHeightPixels)));
        Log.d(LOG_TAG, "Items per page".concat(itemsPerPage));
        listView.setOnScrollListener(new AbsListView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(AbsListView view, int scrollState) {

            }

            @Override
            public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount) {

                Log.d(LOG_TAG, "MEDIA VIEW SCROLL FIRED");
                Log.d(LOG_TAG, "Media View at Bottom? ".concat(String.valueOf(!view.canScrollList(1))));
                Log.d(LOG_TAG, "All pages display? ".concat(String.valueOf(!(pagesDisplayed <= maxPages))));
                Log.d(LOG_TAG, "Is Ready? = ".concat(String.valueOf(isReady(false, false))));
                Log.d(LOG_TAG, "Viewable Item Count = ".concat(String.valueOf(visibleItemCount)));
                ThreadManager.execute(() -> {
                    try {
                        Thread.sleep(1000);
                        if (!view.canScrollList(1) && pagesDisplayed <= maxPages && visibleItemCount > 0 && isReady(false, false)) {

                            getImagesByPage(view, pagesDisplayed, Integer.parseInt(itemsPerPage));
                        }
                    } catch (ExecutionException | InterruptedException | TimeoutException e) {
                        e.printStackTrace();
                        ACRA.getErrorReporter().handleSilentException(e);
                    }
                });
            }
        });

        scrollView.setOnRefreshListener(this::refresh);
        ThreadManager.execute(() -> {
            try {
//                    String itemsPerPage = UserSettingsService.getPreference(Constants.UserPreference.DEFAULT_PAGE_COUNT);
                getImagesByPage(view, pagesDisplayed, Integer.parseInt(itemsPerPage));
            } catch (ExecutionException | InterruptedException | AssertionError | TimeoutException e) {
                e.printStackTrace();
                ACRA.getErrorReporter().handleSilentException(e);
            }
        });

    }

    @Inject
    public void setBackupService(BackupService backupService) {
        this.backupService = backupService;
    }

    @Inject
    public void setJsonParser(JsonParser jsonParser) {
        this.jsonParser = jsonParser;
    }

    public void refresh() {

        listView.setAdapter(null);
        scrollView.setRefreshing(false);
        showSpinner(true);
        pagesDisplayed = 0;

        ThreadManager.execute(() -> {
            try {
                getImagesByPage(parentView, pagesDisplayed, Integer.parseInt(itemsPerPage));
            } catch (ExecutionException | InterruptedException | TimeoutException e) {
                e.printStackTrace();
                ACRA.getErrorReporter().handleSilentException(e);
            }
        });

    }

    private synchronized void showSpinner(boolean shouldShow) {
        if(shouldShow) {
            processingDialog = ProcessingDialog.newInstance();
            processingDialog.show(getChildFragmentManager(), "Progressing");
        } else if(processingDialog.isVisible()){
            processingDialog.dismiss();
        }
    }

    private void getImagesByPage(View parentView, int page, int count) throws ExecutionException, InterruptedException, TimeoutException {
        try {
            showSpinner(true);
            Log.d(LOG_TAG, "Getting images by page");
            isReady(true, false);
            String response = backupService.getPhotoPage(
                    page,
                    count,
                    displayWidithPixels,
                    displayHeightPixels,
                    getContext()
            ).get(30, TimeUnit.SECONDS);
            Log.d(LOG_TAG, "received response from image page request");
            assert StringUtils.isNotBlank(response) : "Failed to retrieve image page, response was empty";
            assert !Pattern.compile("FAILURE.*").matcher(response).matches() : "Failed to retrieve image page, response FAILURE";
            Log.d(LOG_TAG, response);

            BackupFilesRetrieval backupPage = jsonParser.readValueFromString(response, BackupFilesRetrieval.class);
            assert backupPage != null : "BackupFiles Retrieval was null despite non-empty response. It was ".concat(response);
            assert backupPage.getBackupFiles() != null && backupPage.getBackupFiles().size() > 0 : "Retrieved no backup files despite backup service reporting backup files existing";
            Log.d(LOG_TAG, "Images retireved!!");
            maxPages = backupPage.getMaxPage();
            addImagesToScreen(backupPage.getBackupFiles(), parentView, pagesDisplayed);
            pagesDisplayed++;
        } finally {
            isReady(true, true);
            showSpinner(false);
        }

    }

    private void addImagesToScreen(List<BackupFile> backupFiles, View parentView, int pagesDisplayed) {
        Log.d(LOG_TAG, "Adding ".concat(String.valueOf(backupFiles.size()).concat(" images")));
        if (listView.getAdapter() == null && backupFiles.isEmpty()) {
            return; //TODO show error that no photos were found
        }
        Log.d(LOG_TAG, "pages displayed = ".concat(String.valueOf(pagesDisplayed)));
        if (listView.getAdapter() != null) {
            Log.d(LOG_TAG, "items displayed = ".concat(String.valueOf(listView.getAdapter().getCount())));
        }
        if (listView.getAdapter() != null && listView.getAdapter().getCount() > 0 && pagesDisplayed == 0) {
            listView.setAdapter(null);
        }
        if (listView.getAdapter() == null && !backupFiles.isEmpty()) {
            BackupFile first = backupFiles.get(0);
            backupFiles.remove(0);
            BackupFile second = null;
            if (!backupFiles.isEmpty()) {
                second = backupFiles.get(0);
                backupFiles.remove(0);
            }
            createFirstRow(listView, first, second);
            Log.d(LOG_TAG, "First row created");
        }

        ImageListAdapter adapter = ((ImageListAdapter) listView.getAdapter());
        assert adapter != null : "Failed to get image adapter";
        Log.d(LOG_TAG, "processing ".concat(String.valueOf(backupFiles.size()).concat(" files.")));
        for (int i = 0; i < backupFiles.size() - 1; i += 2) {
            adapter.addImage(new BackupFilePair(backupFiles.get(i), backupFiles.get(i + 1)));
        }
        if (backupFiles.size() % 2 == 1) {
            adapter.addImage(new BackupFilePair(backupFiles.get(backupFiles.size() - 1), null));
        }
        Log.d(LOG_TAG, "Adapter has ".concat(String.valueOf(adapter.getCount()).concat(" images")));
    }

    private void createFirstRow(ListView listView, BackupFile first, BackupFile second) {
        CompletableFuture<Boolean> future = new CompletableFuture<>();
        requireContext().getMainExecutor().execute(() -> {
            List<BackupFilePair> images = new ArrayList<>();
            BackupFilePair pair = new BackupFilePair(first, second);
            images.add(pair);
            if (isAdded()) {
                listView.setAdapter(new ImageListAdapter(images, getContext(), viewGroup, getChildFragmentManager(), backupService, getResources()));
            }
            future.complete(true);
        });
        try {
            future.get();
        } catch (ExecutionException | InterruptedException e) {
            e.printStackTrace();
        }
    }

    private void diplayNoMediaAvailable() {
        //TODO not implemented
        Log.i(LOG_TAG, "no media to display");
    }
}