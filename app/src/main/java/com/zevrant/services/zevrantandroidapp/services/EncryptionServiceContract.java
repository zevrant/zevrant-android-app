package com.zevrant.services.zevrantandroidapp.services;

public interface EncryptionServiceContract {

    //You cannot access private key from tests only in the application
    String getSecret(String secretName);

    void createKeys();

    void setSecret(String secretName, String secretValue);

    boolean hasSecret(String loginUserName);
}
