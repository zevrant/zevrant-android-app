package com.zevrant.services.zevrantandroidapp.fragments.dialogs;

import android.graphics.Bitmap;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.Fragment;

import com.zevrant.services.zevrantandroidapp.R;
import com.zevrant.services.zevrantandroidapp.services.BackupService;
import com.zevrant.services.zevrantandroidapp.utilities.Constants;
import com.zevrant.services.zevrantandroidapp.utilities.ImageUtilities;
import com.zevrant.services.zevrantandroidapp.utilities.ThreadManager;

import org.acra.ACRA;
import org.apache.commons.lang3.StringUtils;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.concurrent.ExecutionException;

import javax.inject.Inject;

/**
 * A simple {@link Fragment} subclass.
 * Use the {@link ImageViewDialog#newInstance} factory method to
 * create an instance of this fragment.
 */
public class ImageViewDialog extends DialogFragment {

    // TODO: Rename parameter arguments, choose names that match
    // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
    private static final String ARG_PARAM1 = "image";

    // TODO: Rename and change types of parameters
    private byte[] mParam1;
    private BackupService backupService;
    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param param1 Image to Display.
     * @return A new instance of fragment ImageView.
     */
    // TODO: Rename and change types and number of parameters
    public static ImageViewDialog newInstance(@NonNull byte[] param1) {
        ImageViewDialog fragment = new ImageViewDialog();
        Bundle args = new Bundle();
        args.putByteArray(ARG_PARAM1, param1);
        fragment.setArguments(args);
        return fragment;
    }

    public ImageViewDialog() {
        // Required empty public constructor
    }

    @Inject
    public void setBackupService(BackupService backupService) {
        this.backupService = backupService;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            mParam1 = getArguments().getByteArray(ARG_PARAM1);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_image_view, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        assert mParam1 != null && mParam1.length > 0 : "Image view dialog was created without arguments!!!!";
        ThreadManager.execute(() -> {
            BufferedInputStream inputStream = null;

            inputStream = new BufferedInputStream(new ByteArrayInputStream(mParam1));
            Bitmap bmp = ImageUtilities.createBitMap(inputStream);
            if (getContext() != null) {
                requireContext().getMainExecutor().execute(() -> {
                    ImageView imageView = view.findViewById(R.id.imageViewDialogFocus);
                    imageView.setImageBitmap(bmp);
                    imageView.setVisibility(View.VISIBLE);
                });
            } else {
                dismiss(); //TODO add additional error handling
            }
        });
    }
}