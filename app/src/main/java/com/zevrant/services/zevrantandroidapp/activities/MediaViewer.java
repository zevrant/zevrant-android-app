//package com.zevrant.services.zevrantandroidapp.activities;
//
//import android.os.Bundle;
//
//import com.fasterxml.jackson.core.JsonProcessingException;
//import com.zevrant.services.zevrantandroidapp.R;
//import com.zevrant.services.zevrantandroidapp.exceptions.CredentialsNotFoundException;
//import com.zevrant.services.zevrantandroidapp.fragments.ErrorFragment;
//import com.zevrant.services.zevrantandroidapp.services.BackupService;
//import com.zevrant.services.zevrantandroidapp.services.CredentialsService;
//import com.zevrant.services.zevrantandroidapp.services.OAuthService;
//import com.zevrant.services.zevrantuniversalcommon.contants.Roles;
//
//import androidx.appcompat.app.AppCompatActivity;
//
//import org.acra.ACRA;
//
//import java.util.concurrent.CompletableFuture;
//import java.util.concurrent.ExecutionException;
//
//
//public class MediaViewer extends AppCompatActivity  {
//
////    private ActivityScrollingBinding binding;
//    private ErrorFragment errorFragment;
//
//    @Override
//    protected void onCreate(Bundle savedInstanceState) {
//        super.onCreate(savedInstanceState);
//    }
//
//    @Override
//    public void onStart() {
//        super.onStart();
////        binding = ActivityScrollingBinding.inflate(getLayoutInflater());
////        setContentView(binding.getRoot());
//        getSupportFragmentManager().beginTransaction()
//                .setReorderingAllowed(true)
//                .add(R.id.media_error_fragment, ErrorFragment.class, null)
//                .commit();
////        errorFragment = (ErrorFragment) getSupportFragmentManager().findFragmentById(R.id.media_error_fragment);
////        assert errorFragment != null: "failed to find media error fragment";
////        View view = errorFragment.getView();
////        assert view != null: "Error view is null";
////        TextView errorText = errorFragment.requireView().findViewById(R.id.error_fragment_text);
////        errorText.setText(R.string.media_retrieval_error);
////        showErrorText();
//
//        CompletableFuture<Boolean> completableFuture = new CompletableFuture<>();
//        new Thread(() -> {
//            boolean canBackup = OAuthService.canI(Roles.BACKUPS);
//            if (canBackup) {
//                BackupService.getAlllHashes();
//                completableFuture.complete(true);
//            }
//            completableFuture.complete(false);
//        });
//
//        completableFuture.handleAsync((acknowledge, throwable) -> {
//            hideErrorText();
//            return acknowledge;
//        });
//    }
//
//    private void showErrorText() {
//        getSupportFragmentManager().beginTransaction()
//                .setCustomAnimations(android.R.animator.fade_in, android.R.animator.fade_out)
//                .show(errorFragment)
//                .commit();
//    }
//
//    private void hideErrorText() {
//        getSupportFragmentManager().beginTransaction()
//                .setCustomAnimations(android.R.animator.fade_in, android.R.animator.fade_out)
//                .hide(errorFragment)
//                .commit();
//    }
//}