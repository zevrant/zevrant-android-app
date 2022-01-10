package com.zevrant.services.zevrantandroidapp.utilities;

import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.TypedValue;

import java.io.File;
import java.io.InputStream;
import java.util.Base64;

public class ImageUtilities {

    public static Bitmap createBitMap(byte[] bytes) {
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inMutable = true;
        return BitmapFactory.decodeByteArray(bytes, 0, bytes.length, options);
    }

    public static Bitmap createBitMap(String base64EncodedBytes) {
        return createBitMap(Base64.getDecoder().decode(base64EncodedBytes));
    }

    public static Bitmap createBitMap(File imageFile) {
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inMutable = true;
        return BitmapFactory.decodeFile(imageFile.getAbsolutePath(), options);
    }

    public static Bitmap createBitMap(InputStream imageFile) {
        return BitmapFactory.decodeStream(imageFile);
    }

    public static int convertDpToPx(int dp, Resources r) {
        return (int) Math.round(TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP,
                (float) dp,
                r.getDisplayMetrics()
        ));
    }

    private int getDisplayHeightPixels(Resources r) {
        return r.getDisplayMetrics().heightPixels;
    }

    private int getDisplayWidthPixels(Resources r) {
        return r.getDisplayMetrics().widthPixels;
    }
}
