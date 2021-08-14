package com.zevrant.services.zevrantandroidapp.services;

import com.google.android.gms.auth.api.credentials.Credential;
import com.zevrant.services.zevrantandroidapp.pojo.CredentialWrapper;

import java.util.Observer;

public class CredentialsService {

    private static CredentialWrapper credentialWrapper;

    public static void init() {
        credentialWrapper = new CredentialWrapper();
    }

    public void setCredentials(Credential credential) {
        credentialWrapper.setCredential(credential);
    }

    public void addObserver(Observer observer) {
        credentialWrapper.addObserver(observer);
    }

    public static Credential getCredential() {
        return credentialWrapper.getCredential();
    }

    public static void setCredential(Credential credential) {
        credentialWrapper.setCredential(credential);
    }
}
