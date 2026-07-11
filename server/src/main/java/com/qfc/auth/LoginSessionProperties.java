package com.qfc.auth;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "qfc.auth")
public class LoginSessionProperties {

    private int sessionLifetimeSeconds = 86400;

    public int getSessionLifetimeSeconds() {
        return sessionLifetimeSeconds;
    }

    public void setSessionLifetimeSeconds(int sessionLifetimeSeconds) {
        this.sessionLifetimeSeconds = sessionLifetimeSeconds;
    }

    public int normalizedSessionLifetimeSeconds() {
        return Math.max(1, sessionLifetimeSeconds);
    }
}
