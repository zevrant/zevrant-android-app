package com.zevrant.services.zevrantandroidapp.pojo;

public class AuthBody {

    private String sessionState;
    private String code;

    public AuthBody() {
    }

    public AuthBody(String sessionState, String code) {
        this.sessionState = sessionState;
        this.code = code;
    }

    public String getSessionState() {
        return sessionState;
    }

    public void setSessionState(String sessionState) {
        this.sessionState = sessionState;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }
}
