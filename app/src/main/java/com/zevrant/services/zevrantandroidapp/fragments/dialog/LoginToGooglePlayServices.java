package com.zevrant.services.zevrantandroidapp.fragments.dialog;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.Fragment;

import com.zevrant.services.zevrantandroidapp.R;

/**
 * A simple {@link Fragment} subclass.
 * Use the {@link LoginToGooglePlayServices#newInstance} factory method to
 * create an instance of this fragment.
 */
@Deprecated
public class LoginToGooglePlayServices extends DialogFragment {

    public LoginToGooglePlayServices() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @return A new instance of fragment LoginToGooglePlayServices.
     */
    @Deprecated
    public static LoginToGooglePlayServices newInstance() {
        return new LoginToGooglePlayServices();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_login_to_google_play_services, container, false);
    }
}