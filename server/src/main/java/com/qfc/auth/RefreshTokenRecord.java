package com.qfc.auth;

public class RefreshTokenRecord {

    private String accountType;
    private Long userId;
    private String deviceId;
    private String clientIp;
    private String userAgent;

    public RefreshTokenRecord() {
    }

    public RefreshTokenRecord(String accountType, Long userId, String deviceId, String clientIp, String userAgent) {
        this.accountType = accountType;
        this.userId = userId;
        this.deviceId = deviceId;
        this.clientIp = clientIp;
        this.userAgent = userAgent;
    }

    public String getAccountType() {
        return accountType;
    }

    public void setAccountType(String accountType) {
        this.accountType = accountType;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
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
}
