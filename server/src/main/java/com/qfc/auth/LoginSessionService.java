package com.qfc.auth;

import com.qfc.common.ApiException;
import com.qfc.config.SessionKeys;
import java.io.Serializable;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
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
    private final ActiveSessionRepository activeSessionRepository;

    @Autowired
    public LoginSessionService(LoginSessionProperties properties, ActiveSessionRepository activeSessionRepository) {
        this(properties, activeSessionRepository, Clock.systemDefaultZone());
    }

    LoginSessionService(LoginSessionProperties properties, ActiveSessionRepository activeSessionRepository, Clock clock) {
        this.properties = properties;
        this.activeSessionRepository = activeSessionRepository;
        this.clock = clock;
    }

    public void establishLogin(HttpServletRequest request, String sessionKey, LoginUser loginUser) {
        HttpSession session = request.getSession(true);
        int lifetimeSeconds = properties.normalizedSessionLifetimeSeconds();
        String accountKey = accountKey(loginUser.getAccountType(), loginUser.getId());
        String deviceId = UUID.randomUUID().toString();
        String loginToken = UUID.randomUUID().toString();
        String clientIp = clientIp(request);
        String userAgent = clientUserAgent(request);
        Instant now = clock.instant();
        LoginSessionMetadata metadata = new LoginSessionMetadata(
            accountKey,
            deviceId,
            loginToken,
            clientIp,
            userAgent,
            now.plusSeconds(lifetimeSeconds)
        );

        session.setMaxInactiveInterval(lifetimeSeconds);
        session.setAttribute(sessionKey, loginUser);
        session.setAttribute(metadataKey(sessionKey), metadata);
        activeSessionRepository.store(accountKey, deviceId, loginToken, clientIp, userAgent, lifetimeSeconds);
    }

    public String establishTokenLogin(String accountType, Long userId, HttpServletRequest request) {
        int lifetimeSeconds = properties.normalizedSessionLifetimeSeconds();
        String accountKey = accountKey(accountType, userId);
        String deviceId = UUID.randomUUID().toString();
        String loginToken = UUID.randomUUID().toString();
        activeSessionRepository.store(
            accountKey,
            deviceId,
            loginToken,
            clientIp(request),
            clientUserAgent(request),
            lifetimeSeconds
        );
        return deviceId;
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
        String activeToken = activeSessionRepository.getLoginToken(metadata.getAccountKey(), metadata.getDeviceId());
        if (activeToken == null) {
            clearSessionAttributes(session, sessionKey);
            throw unauthorized("登录状态已失效，请重新登录");
        }
        if (!metadata.getLoginToken().equals(activeToken)) {
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
        activeSessionRepository.remove(accountKey(accountType, userId));
    }

    public void invalidateOtherDevices(String accountType, Long userId, String currentDeviceId) {
        if (!StringUtils.hasText(accountType) || userId == null || !StringUtils.hasText(currentDeviceId)) {
            return;
        }
        activeSessionRepository.removeOthers(accountKey(accountType, userId), currentDeviceId);
    }

    public List<AuthDeviceView> listDevices(String accountType, Long userId, String currentDeviceId) {
        return activeSessionRepository.listDevices(accountKey(accountType, userId), currentDeviceId);
    }

    public boolean isDeviceActive(String accountType, Long userId, String deviceId) {
        if (!StringUtils.hasText(accountType) || userId == null || !StringUtils.hasText(deviceId)) {
            return false;
        }
        return activeSessionRepository.isDeviceActive(accountKey(accountType, userId), deviceId);
    }

    public String currentDeviceId(HttpServletRequest request, String sessionKey) {
        HttpSession session = request == null ? null : request.getSession(false);
        if (session == null) {
            return "";
        }
        LoginSessionMetadata metadata = loginMetadata(session, sessionKey);
        return metadata == null ? "" : metadata.getDeviceId();
    }

    private void clearLogin(HttpSession session, String sessionKey, LoginSessionMetadata metadata) {
        clearSessionAttributes(session, sessionKey);
        activeSessionRepository.removeIfMatch(metadata.getAccountKey(), metadata.getDeviceId(), metadata.getLoginToken());
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

    private static final class LoginSessionMetadata implements Serializable {
        private static final long serialVersionUID = 1L;

        private final String accountKey;
        private final String deviceId;
        private final String loginToken;
        private final String clientIp;
        private final String userAgent;
        private final Instant expiresAt;

        private LoginSessionMetadata(String accountKey, String deviceId, String loginToken, String clientIp, String userAgent, Instant expiresAt) {
            this.accountKey = accountKey;
            this.deviceId = deviceId;
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

        private String getDeviceId() {
            return deviceId;
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
