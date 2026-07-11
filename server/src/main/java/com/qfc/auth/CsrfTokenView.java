package com.qfc.auth;

public class CsrfTokenView {

    private String token;

    public CsrfTokenView(String token) {
        this.token = token;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }
}
