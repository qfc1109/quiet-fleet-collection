package com.qfc.auth;

import com.qfc.admin.RbacUserSessionService;
import com.qfc.common.ApiException;
import com.qfc.config.SessionKeys;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class CurrentUserResolver {

    private final JwtTokenProvider jwtTokenProvider;
    private final AuthService authService;
    private final LoginSessionService loginSessionService;

    public CurrentUserResolver(JwtTokenProvider jwtTokenProvider, AuthService authService, LoginSessionService loginSessionService) {
        this.jwtTokenProvider = jwtTokenProvider;
        this.authService = authService;
        this.loginSessionService = loginSessionService;
    }

    public LoginUser resolveSite(HttpServletRequest request) {
        LoginUser bearer = resolveBearer(request);
        if (bearer != null) {
            if (!RbacUserSessionService.ACCOUNT_TYPE_SITE_USER.equals(bearer.getAccountType())) {
                throw new ApiException("FORBIDDEN", "没有前台账号权限", 403);
            }
            return bearer;
        }
        return resolveSession(request, SessionKeys.SITE_LOGIN_USER);
    }

    public LoginUser resolveAdmin(HttpServletRequest request) {
        LoginUser bearer = resolveBearer(request);
        if (bearer != null) {
            if (!RbacUserSessionService.ACCOUNT_TYPE_ADMIN.equals(bearer.getAccountType())) {
                throw new ApiException("FORBIDDEN", "没有后台操作权限", 403);
            }
            return bearer;
        }
        return resolveSession(request, SessionKeys.ADMIN_LOGIN_USER);
    }

    public LoginUser resolveAny(HttpServletRequest request) {
        LoginUser bearer = resolveBearer(request);
        if (bearer != null) {
            return bearer;
        }
        try {
            return resolveSession(request, SessionKeys.SITE_LOGIN_USER);
        } catch (ApiException exception) {
            return resolveSession(request, SessionKeys.ADMIN_LOGIN_USER);
        }
    }

    public String currentDeviceId(HttpServletRequest request, String sessionKey) {
        String bearerToken = bearerToken(request);
        if (StringUtils.hasText(bearerToken)) {
            String deviceId = jwtTokenProvider.getDeviceId(bearerToken);
            return deviceId == null ? "" : deviceId;
        }
        return loginSessionService.currentDeviceId(request, sessionKey);
    }

    private LoginUser resolveBearer(HttpServletRequest request) {
        String token = bearerToken(request);
        if (!StringUtils.hasText(token)) {
            return null;
        }
        Long userId = jwtTokenProvider.getUserId(token);
        String accountType = jwtTokenProvider.getAccountType(token);
        String deviceId = jwtTokenProvider.getDeviceId(token);
        if (StringUtils.hasText(deviceId) && !loginSessionService.isDeviceActive(accountType, userId, deviceId)) {
            throw new ApiException("UNAUTHORIZED", "登录状态已失效，请重新登录", 401);
        }
        return authService.currentUser(userId, accountType);
    }

    private LoginUser resolveSession(HttpServletRequest request, String sessionKey) {
        LoginUser current = loginSessionService.requireLogin(request, sessionKey);
        LoginUser refreshed = authService.currentUser(current.getId(), current.getAccountType());
        HttpSession session = request == null ? null : request.getSession(false);
        if (session != null) {
            session.setAttribute(sessionKey, refreshed);
        }
        return refreshed;
    }

    private String bearerToken(HttpServletRequest request) {
        String header = request == null ? "" : request.getHeader("Authorization");
        if (!StringUtils.hasText(header) || !header.startsWith("Bearer ")) {
            return "";
        }
        return header.substring(7);
    }
}
