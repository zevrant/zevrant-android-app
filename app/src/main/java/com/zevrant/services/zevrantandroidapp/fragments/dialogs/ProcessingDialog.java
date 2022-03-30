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

import com.zevrant.services.zevrantandroidapp.R;
import com.zevrant.services.zevrantandroidapp.services.BackupService;
import com.zevrant.services.zevrantandroidapp.utilities.ImageUtilities;
import com.zevrant.services.zevrantandroidapp.utilities.ThreadManager;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;

import javax.inject.Inject;

public class ProcessingDialog extends DialogFragment {

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @return A new instance of fragment ImageView.
     */
    public static ProcessingDialog newInstance() {
        ProcessingDialog fragment = new ProcessingDialog();
        return fragment;
    }

    public ProcessingDialog() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        this.getDialog().getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.content_spinner, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
    }


}
