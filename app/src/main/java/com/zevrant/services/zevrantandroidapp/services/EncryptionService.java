package com.zevrant.services.zevrantandroidapp.services;

import android.content.Context;
import android.content.SharedPreferences;
import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyProperties;
import android.util.Base64;

import com.zevrant.services.zevrantandroidapp.exceptions.CredentialsNotFoundException;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.KeyPairGenerator;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.Security;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;

public class EncryptionService {

    private static KeyStore keyStore;
    private static SharedPreferences sharedPreferences;

    private static final String RSA_ALIAS = "rsaKey";
    private static final Logger logger = LoggerFactory.getLogger(EncryptionService.class);
    private static final String PADDING = "RSA/ECB/PKCS1Padding";
    public static void init(Context context) {
        try {
            sharedPreferences = context.getSharedPreferences("zevrant-services-preferences", Context.MODE_PRIVATE);
            keyStore = KeyStore.getInstance("AndroidKeyStore");
            keyStore.load(null);
            if (!keyStore.containsAlias(RSA_ALIAS)) {
                Security.addProvider(new org.bouncycastle.jce.provider.BouncyCastleProvider());

                KeyGenParameterSpec spec = new KeyGenParameterSpec.Builder(RSA_ALIAS, KeyProperties.PURPOSE_DECRYPT)
                        .setDigests(KeyProperties.DIGEST_SHA256, KeyProperties.DIGEST_SHA512)
                        .setKeySize(4096)
                        .build();

                KeyPairGenerator generator = KeyPairGenerator.getInstance(KeyProperties.KEY_ALGORITHM_RSA, "AndroidKeyStore");
                generator.initialize(spec);

                SecureRandom random = SecureRandom.getInstanceStrong();
                generator.generateKeyPair();

                assert keyStore.containsAlias(RSA_ALIAS);
            }
        } catch (NoSuchAlgorithmException | NoSuchProviderException | CertificateException | KeyStoreException | IOException | InvalidAlgorithmParameterException e) {
            e.printStackTrace();
            RuntimeException rex = new RuntimeException(e.getMessage());
            rex.setStackTrace(e.getStackTrace());
            throw rex;
        }

    }

    public static String getSecret(String secretName) {

        try {
            String encryptedSecret = getEncryptedSecret(secretName);
            PrivateKey privateKey = (PrivateKey) keyStore.getKey(RSA_ALIAS, "".toCharArray());
            final Cipher cipher = Cipher.getInstance(PADDING);
            cipher.init(Cipher.DECRYPT_MODE, privateKey);

            return new String(
                    cipher.doFinal(
                            Base64.decode(encryptedSecret, Base64.DEFAULT)), StandardCharsets.UTF_8);
        } catch (UnrecoverableKeyException | NoSuchAlgorithmException | KeyStoreException |
                NoSuchPaddingException | InvalidKeyException | CredentialsNotFoundException |
                BadPaddingException | IllegalBlockSizeException e) {
            logger.error(ExceptionUtils.getStackTrace(e));
            RuntimeException runtimeException = new RuntimeException(e.getMessage());
            runtimeException.setStackTrace(e.getStackTrace());
            throw runtimeException;
        }
    }

    private static String getEncryptedSecret(String secretName) throws CredentialsNotFoundException {
        String encryptedString = sharedPreferences.getString(secretName, null);
        if (StringUtils.isBlank(encryptedString)) {
            throw new CredentialsNotFoundException("Credential for secret ".concat(secretName).concat(" could not be found"));
        }
        return encryptedString;
    }

    public static void setSecret(String secretName, String secretValue) {
        try {
            PublicKey publicRsaKey = keyStore.getCertificate(RSA_ALIAS).getPublicKey();
            Cipher cipher = Cipher.getInstance(PADDING);
            cipher.init(Cipher.ENCRYPT_MODE, publicRsaKey);
            String encryptedSecret = Base64.encodeToString(cipher.doFinal(secretValue.getBytes(StandardCharsets.UTF_8)), Base64.DEFAULT);
            sharedPreferences.edit()
                    .putString(secretName, encryptedSecret)
                    .apply();
        } catch (NoSuchAlgorithmException | NoSuchPaddingException | InvalidKeyException | BadPaddingException | IllegalBlockSizeException | KeyStoreException e) {
            e.printStackTrace();
            RuntimeException runtimeException = new RuntimeException(e.getMessage());
            runtimeException.setStackTrace(e.getStackTrace());
            throw runtimeException;
        }
    }
}
