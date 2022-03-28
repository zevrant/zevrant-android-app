package com.zevrant.services.zevrantandroidapp.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.zevrant.services.zevrantandroidapp.R;
import com.zevrant.services.zevrantandroidapp.activities.ZevrantServices;
import com.zevrant.services.zevrantandroidapp.exceptions.CredentialsNotFoundException;
import com.zevrant.services.zevrantandroidapp.services.CredentialsService;
import com.zevrant.services.zevrantandroidapp.utilities.ThreadManager;

import java.util.Objects;

import javax.inject.Inject;

import dagger.hilt.android.AndroidEntryPoint;

/**
 * A simple {@link Fragment} subclass.
 * Use the {@link LoginFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
@AndroidEntryPoint
public class LoginFragment extends Fragment {

    // TODO: Rename parameter arguments, choose names that match
    // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
    private static final String ARG_PARAM1 = "param1";
    private static final String ARG_PARAM2 = "param2";

    private CredentialsService credentialsService;

    private EditText usernameView;
    private EditText passwordView;

    @Inject
    public void setOAuthService(CredentialsService credentialsService) {
        this.credentialsService = credentialsService;
    }

    public LoginFragment() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @return A new instance of fragment BlankFragment.
     */
    public static LoginFragment newInstance() {
        LoginFragment fragment = new LoginFragment();
        Bundle args = new Bundle();
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.login_fragment, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        usernameView = (EditText) view.findViewById(R.id.username);
        passwordView = (EditText) view.findViewById(R.id.password);
        Button loginButton = (Button) view.findViewById(R.id.loginButton);
        loginButton.setOnClickListener((clickedView) -> {
            ThreadManager.execute(() -> {
                try {
                    credentialsService.freshLogin(usernameView.getText().toString(), passwordView.getText().toString());
                } catch (CredentialsNotFoundException e) {
                    //TODO add proper error handling
                    e.printStackTrace();
                }
                requireContext().getMainExecutor().execute(() -> {
                    loginButton.setVisibility(View.INVISIBLE);
                    ZevrantServices.setCurrentFragment(getId());
                    ZevrantServices.navigate(this, new MediaViewer());
                });
            });
        });


    }
}