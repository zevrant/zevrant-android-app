//package com.zevrant.services.zevrantandroidapp.fragments;
//
//import android.os.Bundle;
//
//import androidx.annotation.NonNull;
//import androidx.annotation.Nullable;
//import androidx.fragment.app.Fragment;
//
//import android.view.LayoutInflater;
//import android.view.View;
//import android.view.ViewGroup;
//import android.widget.TextView;
//
//import com.zevrant.services.zevrantandroidapp.R;
//import com.zevrant.services.zevrantandroidapp.activities.MediaViewer;
//
//public class ErrorFragment extends Fragment {
//
//    private TextView errorTextView;
//
//    public ErrorFragment() {
//        // Required empty public constructor
//    }
//
//    @Override
//    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
//                             @Nullable Bundle savedInstanceState) {
//        super.onCreateView(inflater, container, savedInstanceState);
//        View view = inflater.inflate(R.layout.fragment_error, container, false);
//        view.findViewById(R.id.error_ok).setOnClickListener((buttonView) -> {
//            MediaViewer parentActivity = (MediaViewer) getActivity();
//            getParentFragmentManager().beginTransaction()
//                    .remove(this)
//                    .setReorderingAllowed(true)
//                    .commit();
//        });
//        return view;
//    }
//}