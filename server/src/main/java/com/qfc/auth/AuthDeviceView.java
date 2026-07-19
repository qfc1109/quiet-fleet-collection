package com.qfc.auth;

public class AuthDeviceView {

    private String deviceId;
    private String clientIp;
    private String userAgent;
    private boolean current;

    public AuthDeviceView() {
    }

    public AuthDeviceView(String deviceId, String clientIp, String userAgent, boolean current) {
        this.deviceId = deviceId;
        this.clientIp = clientIp;
        this.userAgent = userAgent;
        this.current = current;
    }

    public String getDeviceId() {
        return deviceId;
    }

    public void setDeviceId(String deviceId) {
        this.deviceId = deviceId;
    }

    public String getClientIp() {
        return clientIp;
    }

    public void setClientIp(String clientIp) {
        this.clientIp = clientIp;
    }

    public String getUserAgent() {
        return userAgent;
    }

    public void setUserAgent(String userAgent) {
        this.userAgent = userAgent;
    }

    public boolean isCurrent() {
        return current;
    }

    public void setCurrent(boolean current) {
        this.current = current;
    }
}
