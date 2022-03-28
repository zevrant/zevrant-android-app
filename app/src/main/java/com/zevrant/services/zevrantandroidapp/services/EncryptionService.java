package com.zevrant.services.zevrantandroidapp.services;

import static com.zevrant.services.zevrantandroidapp.utilities.Constants.LOG_TAG;

import android.content.Context;
import android.content.SharedPreferences;
import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyProperties;
import android.util.Base64;
import android.util.Log;

import com.zevrant.services.zevrantandroidapp.exceptions.CredentialsNotFoundException;

import org.acra.ACRA;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.UnrecoverableEntryException;
import java.security.cert.CertificateException;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.AlgorithmParameterSpec;
import java.util.Objects;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.inject.Inject;

import dagger.hilt.android.qualifiers.ApplicationContext;

public class EncryptionService {


    private static final String RSA_ALIAS = "rsaKey";
    private static final String PADDING = "RSA/ECB/PKCS1Padding";
    private final boolean androidSupportEnabled;
//    private static final String PADDING = "RSA/ECB/OAEPWithSHA1AndMGF1Padding";
//    private static final String PADDING = "RSA/ECB/OAEPWithSHA-1andMGF1Padding";


    private final KeyStore keyStore;
    private final SharedPreferences sharedPreferences;

    @Inject
    public EncryptionService(@ApplicationContext Context context) {
        KeyStore aKeyStore = null;
        boolean enabled = false;
        try {
            sharedPreferences = context.getSharedPreferences("zevrant-services-preferences", Context.MODE_PRIVATE);
            try {
                aKeyStore = KeyStore.getInstance("AndroidKeyStore");
                enabled = true;
            } catch (KeyStoreException ex) {
                if (Objects.requireNonNull(ex.getMessage()).contains("AndroidKeyStore not found")) {
                    aKeyStore = KeyStore.getInstance(KeyStore.getDefaultType());
                }
            }
            androidSupportEnabled = enabled;
            keyStore = aKeyStore;
            keyStore.load(null);
            createKeys();
        } catch (IOException | CertificateException | NoSuchAlgorithmException | KeyStoreException e) {
            e.printStackTrace();
            Log.e(LOG_TAG, ExceptionUtils.getStackTrace(e));
            ACRA.getErrorReporter().handleSilentException(e);
            throw new RuntimeException("Failed to initialize encryption service, authentication at this time is unavailable");
        }
    }

    private String getEncryptedSecret(String secretName) throws CredentialsNotFoundException {
        String encryptedString = sharedPreferences.getString(secretName, null);
        if (StringUtils.isBlank(encryptedString)) {
            throw new CredentialsNotFoundException("Credential for secret ".concat(secretName).concat(" could not be found"));
        }
        return encryptedString;
    }

    public void createKeys() {
        try {
            if (!keyStore.containsAlias(RSA_ALIAS)) {
                KeyPairGenerator kpGenerator;
                if(androidSupportEnabled) {
                    kpGenerator = KeyPairGenerator.getInstance("RSA", "AndroidKeyStore");
                } else {
                    kpGenerator = KeyPairGenerator.getInstance("RSA");
                }
                AlgorithmParameterSpec spec = null;
                KeyGenParameterSpec.Builder builder = new KeyGenParameterSpec.Builder(RSA_ALIAS,
                        KeyProperties.PURPOSE_ENCRYPT | KeyProperties.PURPOSE_DECRYPT);
                builder.setDigests(KeyProperties.DIGEST_SHA512);
                spec = builder.setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_RSA_PKCS1)
                        .setRandomizedEncryptionRequired(false)
                        .build();
                kpGenerator.initialize(spec);
                KeyPair kp = kpGenerator.generateKeyPair();

                assert keyStore.containsAlias(RSA_ALIAS) : "Keys were created but not stored in the keystore";
                String test = new String("test".getBytes(StandardCharsets.UTF_8), StandardCharsets.UTF_8);
                setSecret("test", test);
                String decryptionTest = getSecret(test).trim();
                assert decryptionTest.equals(test) : "Decryption/Encryption Test failed";
            }
        } catch (NoSuchAlgorithmException | NoSuchProviderException | KeyStoreException | InvalidAlgorithmParameterException e) {
            e.printStackTrace();
            RuntimeException rex = new RuntimeException(e.getMessage());
            rex.setStackTrace(e.getStackTrace());
            throw rex;
        } catch (CredentialsNotFoundException ex) {
            Log.e(LOG_TAG, "Secret encryption/decryption test failed, could not find saved credential");
            throw new RuntimeException("Encryption continuity test failed");
        }
    }

    //You cannot access private key from tests only in the application
    public String getSecret(String secretName) throws CredentialsNotFoundException {

        try {
            String encryptedSecret = getEncryptedSecret(secretName);

            KeyStore.PrivateKeyEntry entry = (KeyStore.PrivateKeyEntry) keyStore.getEntry(RSA_ALIAS, null);
            Cipher cipher = null;
//            cipher = Cipher.getInstance("RSA/ECB/NoPadding");
            cipher = Cipher.getInstance(PADDING);
            cipher.init(Cipher.DECRYPT_MODE, entry.getPrivateKey());
            byte[] decryptedBytes = cipher.doFinal(Base64.decode(encryptedSecret, Base64.DEFAULT));
            return new String(decryptedBytes, StandardCharsets.UTF_8);
        } catch (NoSuchAlgorithmException | KeyStoreException | NoSuchPaddingException
                | InvalidKeyException | BadPaddingException
                | IllegalBlockSizeException | UnrecoverableEntryException e) {
            Log.e(LOG_TAG, ExceptionUtils.getStackTrace(e));
            ACRA.getErrorReporter().handleSilentException(e);
            RuntimeException runtimeException = new RuntimeException(e.getMessage());
            runtimeException.setStackTrace(e.getStackTrace());
            throw runtimeException;
        }
    }

    public void setSecret(String secretName, String secretValue) {
        try {
            KeyStore.PrivateKeyEntry entry = (KeyStore.PrivateKeyEntry) keyStore.getEntry(RSA_ALIAS, null);
            Cipher cipher = null;
            RSAPublicKey pubKey = (RSAPublicKey) entry.getCertificate().getPublicKey();
//            cipher = Cipher.getInstance("RSA/ECB/NoPadding");
            cipher = Cipher.getInstance(PADDING);
            cipher.init(Cipher.ENCRYPT_MODE, pubKey);
            String encryptedSecret = Base64.encodeToString(cipher.doFinal(secretValue.getBytes(StandardCharsets.UTF_8)), Base64.DEFAULT);
            sharedPreferences.edit()
                    .putString(secretName, encryptedSecret)
                    .apply();
        } catch (NoSuchAlgorithmException | NoSuchPaddingException | InvalidKeyException | BadPaddingException | IllegalBlockSizeException | KeyStoreException e) {
            e.printStackTrace();
            RuntimeException runtimeException = new RuntimeException(e.getMessage());
            runtimeException.setStackTrace(e.getStackTrace());
            throw runtimeException;
        } catch (UnrecoverableEntryException e) {
            e.printStackTrace();
        }
    }

    public boolean hasSecret(String loginUserName) {
        return sharedPreferences.contains(loginUserName);
    }

    public boolean deleteSecret(String secretName) {
        return sharedPreferences.edit()
                .remove(secretName)
                .commit();
    }
}
