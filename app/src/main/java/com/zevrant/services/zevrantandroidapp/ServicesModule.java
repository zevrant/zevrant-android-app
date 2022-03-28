//package com.zevrant.services.zevrantandroidapp;
//
//import android.content.Context;
//
//import com.zevrant.services.zevrantandroidapp.services.EncryptionService;
//
//import dagger.Binds;
//import dagger.Module;
//import dagger.hilt.InstallIn;
//import dagger.hilt.android.components.ActivityComponent;
//
//@Module(includes = ServicesModule.Declaration.class)
//@InstallIn(ActivityComponent.class)
//public class ServicesModule {
//
//    private final Context context;
//
//    public ServicesModule(Context context) {
//        this.context = context;
//    }
//
//    @Module
//    @InstallIn(ActivityComponent.class)
//    interface Declaration {
//        @Binds
//        EncryptionService bindEncryptionService(EncryptionService encryptionService);
//    }
//
//
//
//}
