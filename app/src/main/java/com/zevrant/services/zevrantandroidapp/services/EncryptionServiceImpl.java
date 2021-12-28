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

public class EncryptionServiceImpl implements EncryptionServiceContract {


    private static final String RSA_ALIAS = "rsaKey";
    private static final String PADDING = "RSA/ECB/PKCS1Padding";
//    private static final String PADDING = "RSA/ECB/OAEPWithSHA1AndMGF1Padding";
//    private static final String PADDING = "RSA/ECB/OAEPWithSHA-1andMGF1Padding";


    private static KeyStore keyStore;
    private static SharedPreferences sharedPreferences;

    public EncryptionServiceImpl(Context context) {
        try {
            sharedPreferences = context.getSharedPreferences("zevrant-services-preferences", Context.MODE_PRIVATE);
            try {
                keyStore = KeyStore.getInstance("AndroidKeyStore");
            } catch (KeyStoreException ex) {
                if (Objects.requireNonNull(ex.getMessage()).contains("AndroidKeyStore KeyStore not available")) {
                    keyStore = KeyStore.getInstance(KeyStore.getDefaultType());
                }
            }

            keyStore.load(null);
        } catch (IOException | CertificateException | NoSuchAlgorithmException | KeyStoreException e) {
            e.printStackTrace();
            Log.e(LOG_TAG, ExceptionUtils.getStackTrace(e));
            ACRA.getErrorReporter().handleSilentException(e);
            throw new RuntimeException("Failed to initialize encryption service, authentication at this time is unavailable");
        }
    }

    private static void checkInit() {
        try {
            if (!keyStore.containsAlias(RSA_ALIAS)) {
                throw new RuntimeException("EncryptionService not initialized");
            }
        } catch (KeyStoreException e) {
            e.printStackTrace();
        }
    }

    private static String getEncryptedSecret(String secretName) throws CredentialsNotFoundException {
        String encryptedString = sharedPreferences.getString(secretName, null);
        if (StringUtils.isBlank(encryptedString)) {
            throw new CredentialsNotFoundException("Credential for secret ".concat(secretName).concat(" could not be found"));
        }
        return encryptedString;
    }

    public void createKeys() {
        try {
            if (!keyStore.containsAlias(RSA_ALIAS)) {
                KeyPairGenerator kpGenerator = KeyPairGenerator.getInstance("RSA", "AndroidKeyStore");
                AlgorithmParameterSpec spec = null;
                spec = new KeyGenParameterSpec.Builder(RSA_ALIAS,
                        KeyProperties.PURPOSE_ENCRYPT | KeyProperties.PURPOSE_DECRYPT)
                        .setDigests(KeyProperties.DIGEST_SHA512)
                        .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_RSA_PKCS1)
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
        }
    }

    //You cannot access private key from tests only in the application
    public String getSecret(String secretName) {

        try {
            String encryptedSecret = getEncryptedSecret(secretName);

            checkInit();
            KeyStore keyStore = KeyStore.getInstance("AndroidKeyStore");
            keyStore.load(null);
            KeyStore.PrivateKeyEntry entry = (KeyStore.PrivateKeyEntry) keyStore.getEntry(RSA_ALIAS, null);
            Cipher cipher = null;
//            cipher = Cipher.getInstance("RSA/ECB/NoPadding");
            cipher = Cipher.getInstance(PADDING);
            cipher.init(Cipher.DECRYPT_MODE, entry.getPrivateKey());
            byte[] decryptedBytes = cipher.doFinal(Base64.decode(encryptedSecret, Base64.DEFAULT));
            return new String(decryptedBytes, StandardCharsets.UTF_8);
        } catch (NoSuchAlgorithmException | KeyStoreException | NoSuchPaddingException | InvalidKeyException | CredentialsNotFoundException | BadPaddingException | IllegalBlockSizeException | CertificateException | IOException | UnrecoverableEntryException e) {
            Log.e(LOG_TAG, ExceptionUtils.getStackTrace(e));
            ACRA.getErrorReporter().handleSilentException(e);
            RuntimeException runtimeException = new RuntimeException(e.getMessage());
            runtimeException.setStackTrace(e.getStackTrace());
            throw runtimeException;
        }
    }

    public void setSecret(String secretName, String secretValue) {
        try {
            KeyStore keyStore = KeyStore.getInstance("AndroidKeyStore");
            keyStore.load(null);
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
        } catch (CertificateException | IOException | UnrecoverableEntryException e) {
            e.printStackTrace();
        }
    }

    public boolean hasSecret(String loginUserName) {
        return sharedPreferences.contains(loginUserName);
    }
}
