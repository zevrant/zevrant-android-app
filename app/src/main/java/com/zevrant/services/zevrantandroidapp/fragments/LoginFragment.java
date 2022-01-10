package com.zevrant.services.zevrantandroidapp.fragments;

import android.net.Uri;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebView;

import com.zevrant.services.zevrantandroidapp.R;

/**
 * A simple {@link Fragment} subclass.
 * Use the {@link LoginFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class LoginFragment extends Fragment {

    // TODO: Rename parameter arguments, choose names that match
    // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
    private static final String ARG_PARAM1 = "param1";
    private static final String ARG_PARAM2 = "param2";

    private WebView loginWebView;

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
        // Inflate the layout for this fragment
        loginWebView = view.findViewById(R.id.login_web_view);
//        loginWebView = getActivity().findViewById(R.id.login_web_view);
        assert loginWebView != null: "Failed to retrieve webView";
        loginWebView.getSettings().setJavaScriptEnabled(true);
        loginWebView.getSettings().setDomStorageEnabled(true);
        loginWebView.loadUrl(new Uri.Builder()
                .scheme("https")
                .encodedAuthority(getString(R.string.encoded_authority))
                .path("/auth/realms/zevrant-services/protocol/openid-connect/auth")
                .appendQueryParameter("client_id", "android")
                .appendQueryParameter("scope", "openid")
                .appendQueryParameter("response_type", "code")
                .appendQueryParameter("redirect_uri", getString(R.string.redirect_uri))
                .build().toString());
    }
}