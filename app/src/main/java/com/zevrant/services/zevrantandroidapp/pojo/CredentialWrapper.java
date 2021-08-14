package com.zevrant.services.zevrantandroidapp.pojo;

import com.google.android.gms.auth.api.credentials.Credential;

import java.util.Observable;

public class CredentialWrapper extends Observable {

    private Credential credential;

    public CredentialWrapper() {
    }

    public CredentialWrapper(Credential credential) {
        this.credential = credential;
    }

    public void setCredential(Credential credential) {
        this.credential = credential;
        setChanged();
        notifyObservers();
    }

    public Credential getCredential() {
        return credential;
    }
}
