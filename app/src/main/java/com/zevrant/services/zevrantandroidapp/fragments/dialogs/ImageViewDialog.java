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
import java.util.concurrent.ExecutionException;

/**
 * A simple {@link Fragment} subclass.
 * Use the {@link ImageViewDialog#newInstance} factory method to
 * create an instance of this fragment.
 */
public class ImageViewDialog extends DialogFragment {

    // TODO: Rename parameter arguments, choose names that match
    // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
    private static final String ARG_PARAM1 = "param1";

    // TODO: Rename and change types of parameters
    private String mParam1;

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param param1 Image to Display.
     * @return A new instance of fragment ImageView.
     */
    // TODO: Rename and change types and number of parameters
    public static ImageViewDialog newInstance(@NonNull String param1) {
        ImageViewDialog fragment = new ImageViewDialog();
        Bundle args = new Bundle();
        args.putString(ARG_PARAM1, param1);
        fragment.setArguments(args);
        return fragment;
    }

    public ImageViewDialog() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            mParam1 = getArguments().getString(ARG_PARAM1);
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
        assert StringUtils.isNotBlank(mParam1) : "Image view dialog was created without arguments!!!!";
        ThreadManager.execute(() -> {
            BufferedInputStream inputStream = null;
            try {
                inputStream = new BufferedInputStream(BackupService.retrieveFile(mParam1,
                        ImageUtilities.convertDpToPx(Constants.MediaViewerControls.MAX_WIDTH_DP * 2, getResources()),
                        ImageUtilities.convertDpToPx(Constants.MediaViewerControls.MAX_HIEGHT_DP * 2, getResources()),
                        getContext()).get());
                Bitmap bmp = ImageUtilities.createBitMap(inputStream);
                if (getContext() != null) {
                    requireContext().getMainExecutor().execute(() -> {
                        ImageView imageView = view.findViewById(R.id.imageViewDialogFocus);
                        imageView.setImageBitmap(bmp);
                        imageView.setVisibility(View.VISIBLE);
                    });
                } else {
                    dismiss();
                }
            } catch (ExecutionException | InterruptedException e) {
                e.printStackTrace();
                ACRA.getErrorReporter().handleSilentException(e);
            }

        });
    }
}