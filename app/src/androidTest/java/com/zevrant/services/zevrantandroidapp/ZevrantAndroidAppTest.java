//package com.zevrant.services.zevrantandroidapp;
//
//import static org.junit.Assert.assertEquals;
//
//import android.content.Context;
//
//import androidx.test.ext.junit.runners.AndroidJUnit4;
//import androidx.test.platform.app.InstrumentationRegistry;
//
//import com.zevrant.services.zevrantandroidapp.test.BuildConfig;
//
//import org.junit.Test;
//import org.junit.runner.RunWith;
//
//@RunWith(AndroidJUnit4.class)
//public class ZevrantAndroidAppTest {
//
//    @Test
//    public void useAppContext() {
//        // Context of the app under test.
//        Context appContext = InstrumentationRegistry.getInstrumentation().getTargetContext();
//        switch(BuildConfig.BUILD_TYPE) {
//            case "local":
//                assertEquals("com.zevrant.services.zevrantandroidapp.local", appContext.getPackageName());
//                break;
//            case "develop":
//                assertEquals("com.zevrant.services.zevrantandroidapp.develop", appContext.getPackageName());
//                break;
//            default:
//                assertEquals("com.zevrant.services.zevrantandroidapp", appContext.getPackageName());
//        }
//
//    }
//}
