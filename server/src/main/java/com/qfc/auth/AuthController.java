package com.qfc.auth;

import com.qfc.common.ApiException;
import com.qfc.common.ApiResponse;
import com.qfc.config.SessionKeys;
import java.util.List;
import java.util.UUID;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import javax.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;
    private final LoginSessionService loginSessionService;
    private final JwtTokenProvider jwtTokenProvider;
    private final RefreshTokenService refreshTokenService;
    private final TokenProperties tokenProperties;
    private final CurrentUserResolver currentUserResolver;

    public AuthController(
        AuthService authService,
        LoginSessionService loginSessionService,
        JwtTokenProvider jwtTokenProvider,
        RefreshTokenService refreshTokenService,
        TokenProperties tokenProperties,
        CurrentUserResolver currentUserResolver
    ) {
        this.authService = authService;
        this.loginSessionService = loginSessionService;
        this.jwtTokenProvider = jwtTokenProvider;
        this.refreshTokenService = refreshTokenService;
        this.tokenProperties = tokenProperties;
        this.currentUserResolver = currentUserResolver;
    }

    @GetMapping("/csrf")
    public ApiResponse<CsrfTokenView> csrfToken(HttpServletRequest request) {
        HttpSession session = request.getSession(true);
        String token = (String) session.getAttribute(SessionKeys.CSRF_TOKEN);
        if (token == null || token.isEmpty()) {
            token = UUID.randomUUID().toString();
            session.setAttribute(SessionKeys.CSRF_TOKEN, token);
        }
        return ApiResponse.success(new CsrfTokenView(token));
    }

    @PostMapping("/login")
    public ApiResponse<LoginUser> login(@Valid @RequestBody LoginRequest request, HttpServletRequest servletRequest) {
        LoginUser loginUser = authService.loginSite(request.getUsername(), request.getPassword());
        renewSession(servletRequest);
        loginSessionService.establishLogin(servletRequest, SessionKeys.SITE_LOGIN_USER, loginUser);
        return ApiResponse.success(loginUser);
    }

    @PostMapping("/admin/login")
    public ApiResponse<LoginUser> adminLogin(@Valid @RequestBody LoginRequest request, HttpServletRequest servletRequest) {
        LoginUser loginUser = authService.loginAdmin(request.getUsername(), request.getPassword());
        renewSession(servletRequest);
        loginSessionService.establishLogin(servletRequest, SessionKeys.ADMIN_LOGIN_USER, loginUser);
        return ApiResponse.success(loginUser);
    }

    /** Token-based login: returns access + refresh tokens instead of session cookie. */
    @PostMapping("/token")
    public ApiResponse<LoginResult> tokenLogin(@Valid @RequestBody LoginRequest request, HttpServletRequest servletRequest) {
        LoginUser loginUser = authService.loginSite(request.getUsername(), request.getPassword());
        String deviceId = loginSessionService.establishTokenLogin(loginUser.getAccountType(), loginUser.getId(), servletRequest);
        String accessToken = jwtTokenProvider.createAccessToken(loginUser.getId(), loginUser.getAccountType(), deviceId);
        String refreshToken = refreshTokenService.issueRefreshToken(
            loginUser.getAccountType(), loginUser.getId(), deviceId,
            clientIp(servletRequest), clientUserAgent(servletRequest)
        );
        return ApiResponse.success(new LoginResult(
            loginUser, accessToken, refreshToken,
            tokenProperties.getAccessTokenExpirySeconds()
        ));
    }

    /** Token-based admin login. */
    @PostMapping("/admin/token")
    public ApiResponse<LoginResult> adminTokenLogin(@Valid @RequestBody LoginRequest request, HttpServletRequest servletRequest) {
        LoginUser loginUser = authService.loginAdmin(request.getUsername(), request.getPassword());
        String deviceId = loginSessionService.establishTokenLogin(loginUser.getAccountType(), loginUser.getId(), servletRequest);
        String accessToken = jwtTokenProvider.createAccessToken(loginUser.getId(), loginUser.getAccountType(), deviceId);
        String refreshToken = refreshTokenService.issueRefreshToken(
            loginUser.getAccountType(), loginUser.getId(), deviceId,
            clientIp(servletRequest), clientUserAgent(servletRequest)
        );
        return ApiResponse.success(new LoginResult(
            loginUser, accessToken, refreshToken,
            tokenProperties.getAccessTokenExpirySeconds()
        ));
    }

    /** Exchange a refresh token for a new access+refresh pair (rotation). */
    @PostMapping("/refresh")
    public ApiResponse<LoginResult> refresh(@Valid @RequestBody RefreshTokenRequest request, HttpServletRequest servletRequest) {
        String[] accountInfo = refreshTokenService.consumeRefreshToken(request.getRefreshToken());
        String accountType = accountInfo[0];
        Long userId = Long.valueOf(accountInfo[1]);
        String deviceId = accountInfo.length > 2 ? accountInfo[2] : "";

        if (StringUtils.hasText(deviceId) && !loginSessionService.isDeviceActive(accountType, userId, deviceId)) {
            throw new ApiException("UNAUTHORIZED", "登录状态已失效，请重新登录", 401);
        }
        LoginUser loginUser = authService.currentUser(userId, accountType);

        String newAccessToken = jwtTokenProvider.createAccessToken(userId, accountType, deviceId);
        String newRefreshToken = refreshTokenService.issueRefreshToken(
            accountType, userId, deviceId,
            clientIp(servletRequest), clientUserAgent(servletRequest)
        );
        return ApiResponse.success(new LoginResult(
            loginUser, newAccessToken, newRefreshToken,
            tokenProperties.getAccessTokenExpirySeconds()
        ));
    }

    /** Revoke all refresh tokens for the current user (requires valid access token in Authorization header). */
    @PostMapping("/revoke")
    public ApiResponse<Void> revokeTokens(HttpServletRequest request) {
        LoginUser current = currentUserResolver.resolveAny(request);
        refreshTokenService.revokeAll(current.getAccountType(), current.getId());
        loginSessionService.invalidateAccount(current.getAccountType(), current.getId());
        return ApiResponse.success(null);
    }

    @GetMapping("/sessions")
    public ApiResponse<List<AuthDeviceView>> sessions(HttpServletRequest request) {
        LoginUser current = currentUserResolver.resolveSite(request);
        String deviceId = currentDeviceId(request, current.getAccountType());
        return ApiResponse.success(loginSessionService.listDevices(current.getAccountType(), current.getId(), deviceId));
    }

    @PostMapping("/sessions/revoke-others")
    public ApiResponse<Void> revokeOtherSessions(HttpServletRequest request) {
        LoginUser current = currentUserResolver.resolveSite(request);
        String deviceId = currentDeviceId(request, current.getAccountType());
        if (!StringUtils.hasText(deviceId)) {
            throw new ApiException("UNAUTHORIZED", "登录状态已失效，请重新登录", 401);
        }
        loginSessionService.invalidateOtherDevices(current.getAccountType(), current.getId(), deviceId);
        refreshTokenService.revokeOthers(current.getAccountType(), current.getId(), deviceId);
        return ApiResponse.success(null);
    }

    @PostMapping("/register")
    public ApiResponse<LoginUser> register(@Valid @RequestBody RegisterRequest request, HttpServletRequest servletRequest) {
        LoginUser loginUser = authService.register(request);
        renewSession(servletRequest);
        loginSessionService.establishLogin(servletRequest, SessionKeys.SITE_LOGIN_USER, loginUser);
        return ApiResponse.success(loginUser);
    }

    @PostMapping("/logout")
    public ApiResponse<Boolean> logout(HttpServletRequest request) {
        loginSessionService.clearLogin(request, SessionKeys.SITE_LOGIN_USER);
        return ApiResponse.success(Boolean.TRUE);
    }

    @PostMapping("/admin/logout")
    public ApiResponse<Boolean> adminLogout(HttpServletRequest request) {
        loginSessionService.clearLogin(request, SessionKeys.ADMIN_LOGIN_USER);
        return ApiResponse.success(Boolean.TRUE);
    }

    @GetMapping("/me")
    public ApiResponse<LoginUser> me(HttpServletRequest request) {
        LoginUser refreshed = currentUserResolver.resolveSite(request);
        return ApiResponse.success(refreshed);
    }

    @GetMapping("/admin/me")
    public ApiResponse<LoginUser> adminMe(HttpServletRequest request) {
        LoginUser refreshed = currentUserResolver.resolveAdmin(request);
        return ApiResponse.success(refreshed);
    }

    @PutMapping("/profile")
    public ApiResponse<LoginUser> updateProfile(
        @Valid @RequestBody UpdateCurrentUserRequest request,
        HttpServletRequest servletRequest
    ) {
        LoginUser current = currentUserResolver.resolveSite(servletRequest);
        LoginUser updated = authService.updateCurrentUserProfile(current.getId(), current.getAccountType(), request);
        HttpSession session = servletRequest.getSession(false);
        session.setAttribute(SessionKeys.SITE_LOGIN_USER, updated);
        return ApiResponse.success(updated);
    }

    @PostMapping(value = "/avatar", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ApiResponse<LoginUser> uploadAvatar(
        @RequestParam("file") MultipartFile file,
        HttpServletRequest servletRequest
    ) {
        LoginUser current = currentUserResolver.resolveSite(servletRequest);
        LoginUser updated = authService.uploadCurrentUserAvatar(current.getId(), current.getAccountType(), file);
        HttpSession session = servletRequest.getSession(false);
        session.setAttribute(SessionKeys.SITE_LOGIN_USER, updated);
        return ApiResponse.success(updated);
    }

    // -- helpers --

    private LoginUser requireSessionUser(HttpServletRequest request, String sessionKey) {
        return loginSessionService.requireLogin(request, sessionKey);
    }

    private HttpSession renewSession(HttpServletRequest request) {
        HttpSession session = request.getSession(true);
        request.changeSessionId();
        return session;
    }

    private String currentDeviceId(HttpServletRequest request, String accountType) {
        String sessionKey = "ADMIN".equals(accountType) ? SessionKeys.ADMIN_LOGIN_USER : SessionKeys.SITE_LOGIN_USER;
        return currentUserResolver.currentDeviceId(request, sessionKey);
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

}
