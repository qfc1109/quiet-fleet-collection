package com.qfc.auth;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "qfc.auth.token")
public class TokenProperties {

    /** JWT signing secret (minimum 256-bit for HMAC-SHA256) */
    private String secret = "default-qfc-jwt-secret-change-in-production-32chars";

    /** Access token expiry in seconds (default: 15 minutes) */
    private int accessTokenExpirySeconds = 900;

    /** Refresh token expiry in seconds (default: 7 days) */
    private int refreshTokenExpirySeconds = 604800;

    public String getSecret() {
        return secret;
    }

    public void setSecret(String secret) {
        this.secret = secret;
    }

    public int getAccessTokenExpirySeconds() {
        return accessTokenExpirySeconds;
    }

    public void setAccessTokenExpirySeconds(int accessTokenExpirySeconds) {
        this.accessTokenExpirySeconds = accessTokenExpirySeconds;
    }

    public int getRefreshTokenExpirySeconds() {
        return refreshTokenExpirySeconds;
    }

    public void setRefreshTokenExpirySeconds(int refreshTokenExpirySeconds) {
        this.refreshTokenExpirySeconds = refreshTokenExpirySeconds;
    }
}
