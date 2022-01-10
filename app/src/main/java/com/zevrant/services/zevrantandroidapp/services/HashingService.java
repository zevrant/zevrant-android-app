package com.zevrant.services.zevrantandroidapp.services;

import static com.zevrant.services.zevrantandroidapp.utilities.Constants.LOG_TAG;

import android.util.Log;

import com.zevrant.services.zevrantuniversalcommon.services.ChecksumService;
import com.zevrant.services.zevrantuniversalcommon.services.HexConversionService;

import org.acra.ACRA;
import org.apache.commons.codec.digest.MessageDigestAlgorithms;

import java.io.IOException;
import java.io.InputStream;
import java.security.NoSuchAlgorithmException;

public class HashingService {

    private static final ChecksumService checksumService = new ChecksumService(new HexConversionService());

    public static String getSha512Checksum(InputStream is) {
        try {
            return checksumService.getSha512Checksum(is);
        } catch( IOException ex) {
            ex.printStackTrace();
            ACRA.getErrorReporter().handleSilentException(ex);
            Log.e(LOG_TAG, "Failed to read provided input stream while attempting to generate message digest");
        } catch (NoSuchAlgorithmException ex) {
            ex.printStackTrace();
            ACRA.getErrorReporter().handleSilentException(ex);
            Log.e(LOG_TAG, "Failed to find algorithm ".concat(MessageDigestAlgorithms.SHA_512).concat(" to create checksum with"));
        }
        return null;
    }
}
