package com.zevrant.services.zevrantandroidapp.fragments;

import static com.zevrant.services.zevrantandroidapp.utilities.Constants.LOG_TAG;
import static com.zevrant.services.zevrantandroidapp.utilities.ImageUtilities.convertDpToPx;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.zevrant.services.zevrantandroidapp.R;
import com.zevrant.services.zevrantandroidapp.adapters.ImageListAdapter;
import com.zevrant.services.zevrantandroidapp.pojo.BackupFilePair;
import com.zevrant.services.zevrantandroidapp.services.BackupService;
import com.zevrant.services.zevrantandroidapp.services.JsonParser;
import com.zevrant.services.zevrantandroidapp.utilities.Constants;
import com.zevrant.services.zevrantandroidapp.utilities.ThreadManager;
import com.zevrant.services.zevrantuniversalcommon.rest.backup.response.BackupFile;
import com.zevrant.services.zevrantuniversalcommon.rest.backup.response.BackupFilesRetrieval;

import org.acra.ACRA;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.regex.Pattern;

public class MediaViewer extends Fragment {

    private int pagesDisplayed = 0;
    private View scrollView;
    private ViewGroup viewGroup;
    private int maxPages;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        this.viewGroup = viewGroup;
        return inflater.inflate(R.layout.fragment_scrolling, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        scrollView = view.findViewById(R.id.mediaScrollView);
        ThreadManager.execute(() -> {
            try {
//                    String itemsPerPage = UserSettingsService.getPreference(Constants.UserPreference.DEFAULT_PAGE_COUNT);
                String itemsPerPage = "6";
                scrollView.setOnScrollChangeListener((View.OnScrollChangeListener) (view1, i, i1, i2, i3) -> {
                    if (view1.getBottom() - (view1.getHeight() + view1.getScrollY()) == 0
                            && pagesDisplayed < maxPages) {
                        Log.d(LOG_TAG, "Hit bottom of scroller");
                        try {
                            getImagesByPage(view, pagesDisplayed, Integer.parseInt(itemsPerPage));
                        } catch (ExecutionException | InterruptedException e) {
                            e.printStackTrace();
                            ACRA.getErrorReporter().handleSilentException(e);
                        }
                    }
                });
                getImagesByPage(view, pagesDisplayed, Integer.parseInt(itemsPerPage));
            } catch (ExecutionException | InterruptedException | AssertionError e) {
                e.printStackTrace();
                ACRA.getErrorReporter().handleSilentException(e);
            }
        });

    }

    private void getImagesByPage(View parentView, int page, int count) throws ExecutionException, InterruptedException {
        String response = BackupService.getPhotoPage(
                page,
                count,
                convertDpToPx(Constants.MediaViewerControls.MAX_WIDTH_DP, getResources()),
                convertDpToPx(Constants.MediaViewerControls.MAX_HIEGHT_DP, getResources()),
                getContext()
        ).get();

        assert StringUtils.isNotBlank(response) : "Failed to retrieve image page, response was empty";
        assert !Pattern.compile("FAILURE.*").matcher(response).matches() : "Failed to retrieve image page, response FAILURE";
        Log.d(LOG_TAG, response);
        BackupFilesRetrieval backupPage = JsonParser.readValueFromString(response, BackupFilesRetrieval.class);
        assert backupPage != null : "BackupFiles Retrieval was null despite non-empty response";
        assert backupPage.getBackupFiles() != null && backupPage.getBackupFiles().size() > 0 : "Retrieved no backup files despite backup service reporting backup files existing";
        maxPages = backupPage.getMaxPage();
        addImagesToScreen(backupPage.getBackupFiles(), parentView);
        pagesDisplayed++;

    }

    private void addImagesToScreen(List<BackupFile> backupFiles, View parentView) {
        final ListView listView = parentView.findViewById(R.id.imageList);
        if ((listView.getAdapter() == null || listView.getAdapter() == null)
                && backupFiles.size() > 1) {
            BackupFile first = backupFiles.get(0);
            BackupFile second = backupFiles.get(1);
            backupFiles.remove(0);
            backupFiles.remove(0);
            createFirstRow(listView, first, second);
        }
        ImageListAdapter adapter = ((ImageListAdapter) listView.getAdapter());
        assert adapter != null;
        for (int i = 0; i < backupFiles.size(); i += 2) {
            adapter.addImage(new BackupFilePair(backupFiles.get(i), backupFiles.get(i + 1)));
        }
        if (backupFiles.size() % 2 == 1) {
            adapter.addImage(new BackupFilePair(backupFiles.get(backupFiles.size() - 1), null));
        }
    }

    private void createFirstRow(ListView listView, BackupFile first, BackupFile second) {
        CompletableFuture<Boolean> future = new CompletableFuture<>();
        requireContext().getMainExecutor().execute(() -> {
            List<BackupFilePair> images = new ArrayList<>();
            BackupFilePair pair = new BackupFilePair(first, second);
            images.add(pair);
            listView.setAdapter(new ImageListAdapter(images, getContext(), viewGroup, getChildFragmentManager()));
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