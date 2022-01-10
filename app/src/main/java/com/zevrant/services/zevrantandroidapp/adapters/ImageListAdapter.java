package com.zevrant.services.zevrantandroidapp.adapters;

import static com.zevrant.services.zevrantandroidapp.utilities.Constants.LOG_TAG;

import android.content.Context;
import android.database.DataSetObserver;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListAdapter;

import androidx.fragment.app.FragmentManager;

import com.zevrant.services.zevrantandroidapp.R;
import com.zevrant.services.zevrantandroidapp.fragments.dialogs.ImageViewDialog;
import com.zevrant.services.zevrantandroidapp.pojo.BackupFilePair;
import com.zevrant.services.zevrantandroidapp.utilities.Constants;
import com.zevrant.services.zevrantandroidapp.utilities.ImageUtilities;
import com.zevrant.services.zevrantandroidapp.utilities.ThreadManager;
import com.zevrant.services.zevrantuniversalcommon.rest.backup.response.BackupFile;

import java.util.ArrayList;
import java.util.List;

public class ImageListAdapter implements ListAdapter {

    private final List<BackupFilePair> items;
    private final Context context;
    private final List<DataSetObserver> observers;
    private final ViewGroup viewGroup;
    private final FragmentManager fragmentManager;

    public ImageListAdapter(List<BackupFilePair> items, Context context, ViewGroup viewGroup, FragmentManager fragmentManager) {
        this.items = items;
        this.context = context;
        this.observers = new ArrayList<>();
        this.viewGroup = viewGroup;
        this.fragmentManager = fragmentManager;
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
            inflatedView = layoutInflater.inflate(R.layout.image_row, viewGroup);
        } else {
            inflatedView = convertView;
        }
        ImageView imageView = inflatedView.findViewById(R.id.imageItemLeft);
        imageView.setMaxWidth(Constants.MediaViewerControls.MAX_WIDTH_DP);
        imageView.setMaxHeight(Constants.MediaViewerControls.MAX_HIEGHT_DP);
        imageView.setOnClickListener(listener -> {
            ImageViewDialog dialog = ImageViewDialog.newInstance(item.getLeft().getFileHash());
            dialog.show(fragmentManager, item.getLeft().getFileName());
        });
        Log.d(LOG_TAG, item.getLeft().getFileName());
        imageView.setImageBitmap(ImageUtilities.createBitMap(item.getLeft().getImageIcon()));
        imageView.setVisibility(View.VISIBLE);
        if(item.getRight() != null) {
            ImageView imageViewRight = inflatedView.findViewById(R.id.imageItemRight);
            imageViewRight.setMaxWidth(Constants.MediaViewerControls.MAX_WIDTH_DP);
            imageViewRight.setMaxHeight(Constants.MediaViewerControls.MAX_HIEGHT_DP);
            imageViewRight.setOnClickListener(listener -> {
                ImageViewDialog dialog = ImageViewDialog.newInstance(item.getRight().getFileHash());
                dialog.show(fragmentManager, item.getRight().getFileName());
            });
            Log.d(LOG_TAG, item.getRight().getFileName());
            imageViewRight.setImageBitmap(ImageUtilities.createBitMap(item.getRight().getImageIcon()));
            imageViewRight.setVisibility(View.VISIBLE);
        }
        return inflatedView;
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
