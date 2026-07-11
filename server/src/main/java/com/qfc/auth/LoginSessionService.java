package com.qfc.auth;

import com.qfc.common.ApiException;
import com.qfc.config.SessionKeys;
import java.io.Serializable;
import java.time.Clock;
import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class LoginSessionService {

    private static final String CODE_UNAUTHORIZED = "UNAUTHORIZED";
    private static final String CODE_LOGGED_IN_ELSEWHERE = "ACCOUNT_LOGGED_IN_ELSEWHERE";

    private final LoginSessionProperties properties;
    private final Clock clock;
    private final ConcurrentMap<String, ActiveLoginSession> activeSessions = new ConcurrentHashMap<String, ActiveLoginSession>();

    @Autowired
    public LoginSessionService(LoginSessionProperties properties) {
        this(properties, Clock.systemDefaultZone());
    }

    LoginSessionService(LoginSessionProperties properties, Clock clock) {
        this.properties = properties;
        this.clock = clock;
    }

    public void establishLogin(HttpServletRequest request, String sessionKey, LoginUser loginUser) {
        HttpSession session = request.getSession(true);
        int lifetimeSeconds = properties.normalizedSessionLifetimeSeconds();
        String accountKey = accountKey(loginUser.getAccountType(), loginUser.getId());
        String loginToken = UUID.randomUUID().toString();
        Instant now = clock.instant();
        LoginSessionMetadata metadata = new LoginSessionMetadata(
            accountKey,
            loginToken,
            clientIp(request),
            clientUserAgent(request),
            now.plusSeconds(lifetimeSeconds)
        );

        session.setMaxInactiveInterval(lifetimeSeconds);
        session.setAttribute(sessionKey, loginUser);
        session.setAttribute(metadataKey(sessionKey), metadata);
        activeSessions.put(accountKey, new ActiveLoginSession(loginToken));
    }

    public LoginUser requireLogin(HttpServletRequest request, String sessionKey) {
        HttpSession session = request == null ? null : request.getSession(false);
        if (session == null || session.getAttribute(sessionKey) == null) {
            throw unauthorized("请先登录");
        }
        LoginUser loginUser = (LoginUser) session.getAttribute(sessionKey);
        LoginSessionMetadata metadata = loginMetadata(session, sessionKey);
        if (metadata == null) {
            return loginUser;
        }
        String accountKey = accountKey(loginUser.getAccountType(), loginUser.getId());
        if (!accountKey.equals(metadata.getAccountKey())) {
            clearSessionAttributes(session, sessionKey);
            throw unauthorized("登录状态已失效，请重新登录");
        }
        if (!clock.instant().isBefore(metadata.getExpiresAt())) {
            clearLogin(session, sessionKey, metadata);
            throw unauthorized("登录已过期，请重新登录");
        }
        ActiveLoginSession activeSession = activeSessions.get(metadata.getAccountKey());
        if (activeSession == null) {
            clearSessionAttributes(session, sessionKey);
            throw unauthorized("登录状态已失效，请重新登录");
        }
        if (!metadata.getLoginToken().equals(activeSession.getLoginToken())) {
            clearSessionAttributes(session, sessionKey);
            throw new ApiException(CODE_LOGGED_IN_ELSEWHERE, "账号已在其他设备或位置登录，请重新登录", 401);
        }
        return loginUser;
    }

    public void clearLogin(HttpServletRequest request, String sessionKey) {
        HttpSession session = request == null ? null : request.getSession(false);
        if (session == null) {
            return;
        }
        LoginSessionMetadata metadata = loginMetadata(session, sessionKey);
        if (metadata != null) {
            clearLogin(session, sessionKey, metadata);
            return;
        }
        clearSessionAttributes(session, sessionKey);
    }

    public void invalidateAccount(String accountType, Long userId) {
        if (!StringUtils.hasText(accountType) || userId == null) {
            return;
        }
        activeSessions.remove(accountKey(accountType, userId));
    }

    private void clearLogin(HttpSession session, String sessionKey, LoginSessionMetadata metadata) {
        clearSessionAttributes(session, sessionKey);
        activeSessions.computeIfPresent(metadata.getAccountKey(), (accountKey, activeSession) ->
            metadata.getLoginToken().equals(activeSession.getLoginToken()) ? null : activeSession
        );
    }

    private void clearSessionAttributes(HttpSession session, String sessionKey) {
        session.removeAttribute(sessionKey);
        session.removeAttribute(metadataKey(sessionKey));
    }

    private LoginSessionMetadata loginMetadata(HttpSession session, String sessionKey) {
        Object value = session.getAttribute(metadataKey(sessionKey));
        return value instanceof LoginSessionMetadata ? (LoginSessionMetadata) value : null;
    }

    private String metadataKey(String sessionKey) {
        if (SessionKeys.ADMIN_LOGIN_USER.equals(sessionKey)) {
            return SessionKeys.ADMIN_LOGIN_METADATA;
        }
        return SessionKeys.SITE_LOGIN_METADATA;
    }

    private String accountKey(String accountType, Long userId) {
        return accountType + ":" + userId;
    }

    private String clientIp(HttpServletRequest request) {
        String forwardedFor = request.getHeader("X-Forwarded-For");
        if (StringUtils.hasText(forwardedFor)) {
            int commaIndex = forwardedFor.indexOf(',');
            return commaIndex >= 0 ? forwardedFor.substring(0, commaIndex).trim() : forwardedFor.trim();
        }
        return request.getRemoteAddr() == null ? "" : request.getRemoteAddr();
    }

    private String clientUserAgent(HttpServletRequest request) {
        String userAgent = request.getHeader("User-Agent");
        return userAgent == null ? "" : userAgent;
    }

    private ApiException unauthorized(String message) {
        return new ApiException(CODE_UNAUTHORIZED, message, 401);
    }

    private static final class ActiveLoginSession {
        private final String loginToken;

        private ActiveLoginSession(String loginToken) {
            this.loginToken = loginToken;
        }

        private String getLoginToken() {
            return loginToken;
        }
    }

    private static final class LoginSessionMetadata implements Serializable {
        private static final long serialVersionUID = 1L;

        private final String accountKey;
        private final String loginToken;
        private final String clientIp;
        private final String userAgent;
        private final Instant expiresAt;

        private LoginSessionMetadata(String accountKey, String loginToken, String clientIp, String userAgent, Instant expiresAt) {
            this.accountKey = accountKey;
            this.loginToken = loginToken;
            this.clientIp = clientIp;
            this.userAgent = userAgent;
            this.expiresAt = expiresAt;
        }

        private String getAccountKey() {
            return accountKey;
        }

        private String getLoginToken() {
            return loginToken;
        }

        private String getClientIp() {
            return clientIp;
        }

        private String getUserAgent() {
            return userAgent;
        }

        private Instant getExpiresAt() {
            return expiresAt;
        }
    }
}
