package com.zevrant.services.zevrantandroidapp.adapters;

import static com.zevrant.services.zevrantandroidapp.utilities.Constants.LOG_TAG;

import android.content.Context;
import android.content.res.Resources;
import android.database.DataSetObserver;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ListAdapter;

import androidx.fragment.app.FragmentManager;

import com.zevrant.services.zevrantandroidapp.R;
import com.zevrant.services.zevrantandroidapp.fragments.dialogs.ImageViewDialog;
import com.zevrant.services.zevrantandroidapp.pojo.BackupFilePair;
import com.zevrant.services.zevrantandroidapp.services.BackupService;
import com.zevrant.services.zevrantandroidapp.utilities.Constants;
import com.zevrant.services.zevrantandroidapp.utilities.ImageUtilities;
import com.zevrant.services.zevrantandroidapp.utilities.ThreadManager;

import org.acra.ACRA;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutionException;

public class ImageListAdapter implements ListAdapter {

    private final List<BackupFilePair> items;
    private final Context context;
    private final List<DataSetObserver> observers;
    private final ViewGroup viewGroup;
    private final FragmentManager fragmentManager;
    private final BackupService backupService;
    private final Resources resources;

    public ImageListAdapter(List<BackupFilePair> items, Context context, ViewGroup viewGroup, FragmentManager fragmentManager,
                            BackupService backupService, Resources resources) {
        this.items = items;
        this.context = context;
        this.observers = new ArrayList<>();
        this.viewGroup = viewGroup;
       this.fragmentManager = fragmentManager;
       this.backupService = backupService;
       this.resources = resources;
    }

    @Override
    public boolean areAllItemsEnabled() {
        return true;
    }

    @Override
    public boolean isEnabled(int i) {
        return true;
    }

    @Override
    public void registerDataSetObserver(DataSetObserver dataSetObserver) {
        observers.add(dataSetObserver);
    }

    @Override
    public void unregisterDataSetObserver(DataSetObserver dataSetObserver) {
        observers.remove(dataSetObserver);
    }

    @Override
    public int getCount() {
        return items.size();
    }

    @Override
    public BackupFilePair getItem(int i) {
        return items.get(i);
    }

    @Override
    public long getItemId(int i) {
        return 0;
    }

    @Override
    public boolean hasStableIds() {
        return false;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        Log.d(LOG_TAG, "position " .concat(String.valueOf(position)).concat(" list size ").concat(String.valueOf(items.size())));
        BackupFilePair item = items.get(position);
        LayoutInflater layoutInflater = LayoutInflater.from(context);
        View inflatedView;
        if(items.size() > 3) {
            Log.d(LOG_TAG, "this isn't right...");
        }
        if (convertView == null) {
            inflatedView = layoutInflater.inflate(R.layout.image_row, null);
        } else {
            inflatedView = convertView;
        }
        ImageView imageView = inflatedView.findViewById(R.id.imageItemLeft);
        imageView.setMaxWidth(Constants.MediaViewerControls.MAX_WIDTH_DP);
        imageView.setMaxHeight(Constants.MediaViewerControls.MAX_HIEGHT_DP);
        imageView.setOnClickListener(listener -> {
            ThreadManager.execute( () -> {
                ImageViewDialog dialog = ImageViewDialog.newInstance(loadImage(item.getLeft().getFileHash()));
                dialog.show(fragmentManager, item.getLeft().getFileName());
            });
        });
        Log.d(LOG_TAG, item.getLeft().getFileName());
        imageView.setImageBitmap(ImageUtilities.createBitMap(item.getLeft().getImageIcon()));
        imageView.setVisibility(View.VISIBLE);
        if(item.getRight() != null) {
            ImageView imageViewRight = inflatedView.findViewById(R.id.imageItemRight);
            imageViewRight.setMaxWidth(Constants.MediaViewerControls.MAX_WIDTH_DP);
            imageViewRight.setMaxHeight(Constants.MediaViewerControls.MAX_HIEGHT_DP);
            imageViewRight.setOnClickListener(listener -> {
                ThreadManager.execute( () -> {
                    ImageViewDialog dialog = ImageViewDialog.newInstance(loadImage(item.getRight().getFileHash()));
                    dialog.show(fragmentManager, item.getRight().getFileName());
                });
            });
            Log.d(LOG_TAG, item.getRight().getFileName());
            imageViewRight.setImageBitmap(ImageUtilities.createBitMap(item.getRight().getImageIcon()));
            imageViewRight.setVisibility(View.VISIBLE);
        }
        return inflatedView;
    }

    private byte[] loadImage(String fileHash) {
        try {
            InputStream imageInputStream = backupService.retrieveFile(fileHash,
                    ImageUtilities.convertDpToPx(Constants.MediaViewerControls.MAX_WIDTH_DP * 2, resources),
                    ImageUtilities.convertDpToPx(Constants.MediaViewerControls.MAX_HIEGHT_DP * 2, resources)
            ).get();
            if (imageInputStream != null) {
                ByteArrayOutputStream buffer = new ByteArrayOutputStream();
                byte[] bytes = new byte[2048];
                BufferedInputStream readStream = new BufferedInputStream(imageInputStream);
                int read = readStream.read(bytes);
                while (read >= 0) {
                    buffer.write(bytes, 0, read);
                    read = readStream.read(bytes);
                }
                return buffer.toByteArray();
            }
        } catch (ExecutionException | InterruptedException | IOException e) {
            e.printStackTrace();
            ACRA.getErrorReporter().handleSilentException(e);
        }
        return null; //TODO add error screen
    }

    @Override
    public int getItemViewType(int position) {
        return View.VISIBLE;
    }

    @Override
    public int getViewTypeCount() {
        return items.size();
    }

    @Override
    public boolean isEmpty() {
        return items.isEmpty();
    }

    public void addImage(BackupFilePair image) {
        context.getMainExecutor().execute(() -> {
            items.add(image);
            observers.forEach(DataSetObserver::onChanged);
        });

    }

    public void removeImage(BackupFilePair image) {
        context.getMainExecutor().execute(() -> {
            items.remove(image);
            observers.forEach(DataSetObserver::onChanged);
        });
    }
}
