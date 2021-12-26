package com.zevrant.services.zevrantandroidapp.services;

import static com.zevrant.services.zevrantandroidapp.utilities.Constants.LOG_TAG;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import junit.framework.TestCase;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.io.IOException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;

@RunWith(JUnit4.class)
public class EncryptionServiceImplTest extends TestCase {
    private AutoCloseable openMocks;
    private EncryptionServiceImpl encryptionService;

    @Mock
    private Context context;

    @Mock
    private SharedPreferences sharedPreferences;

    @Before
    public void setup() {
        openMocks = MockitoAnnotations.openMocks(this);

        encryptionService = new EncryptionServiceImpl(context);
    }

    @After
    public void tearDown() throws Exception {
        openMocks.close();
    }

    @Test
    public void createEncryptionServiceKeys() throws KeyStoreException, CertificateException, NoSuchAlgorithmException, IOException, UnrecoverableKeyException {
        given(context.getSharedPreferences(anyString(), anyInt())).willReturn(sharedPreferences);

        encryptionService.createKeys();

        KeyStore keyStore = KeyStore.getInstance("AndroidKeyStore");
        keyStore.load(null);

        assertThat(keyStore.containsAlias("rsaKey"), equalTo(true));
        Log.e(LOG_TAG, keyStore.getKey("rsaKey", "".toCharArray()).getAlgorithm());
        assertThat(keyStore.getKey("rsaKey", "".toCharArray()).getAlgorithm(), equalTo("TEST"));
    }
}