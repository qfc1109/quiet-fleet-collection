package com.qfc.auth;

import com.qfc.common.ApiResponse;
import com.qfc.config.SessionKeys;
import java.util.UUID;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import javax.validation.Valid;
import org.springframework.http.MediaType;
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

    public AuthController(AuthService authService, LoginSessionService loginSessionService) {
        this.authService = authService;
        this.loginSessionService = loginSessionService;
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
        LoginUser refreshed = currentSessionUser(request, SessionKeys.SITE_LOGIN_USER);
        return ApiResponse.success(refreshed);
    }

    @GetMapping("/admin/me")
    public ApiResponse<LoginUser> adminMe(HttpServletRequest request) {
        LoginUser refreshed = currentSessionUser(request, SessionKeys.ADMIN_LOGIN_USER);
        return ApiResponse.success(refreshed);
    }

    @PutMapping("/profile")
    public ApiResponse<LoginUser> updateProfile(
        @Valid @RequestBody UpdateCurrentUserRequest request,
        HttpServletRequest servletRequest
    ) {
        LoginUser current = requireSessionUser(servletRequest, SessionKeys.SITE_LOGIN_USER);
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
        LoginUser current = requireSessionUser(servletRequest, SessionKeys.SITE_LOGIN_USER);
        LoginUser updated = authService.uploadCurrentUserAvatar(current.getId(), current.getAccountType(), file);
        HttpSession session = servletRequest.getSession(false);
        session.setAttribute(SessionKeys.SITE_LOGIN_USER, updated);
        return ApiResponse.success(updated);
    }

    private LoginUser currentSessionUser(HttpServletRequest request, String sessionKey) {
        LoginUser current = requireSessionUser(request, sessionKey);
        LoginUser refreshed = authService.currentUser(current.getId(), current.getAccountType());
        HttpSession session = request.getSession(false);
        session.setAttribute(sessionKey, refreshed);
        return refreshed;
    }

    private LoginUser requireSessionUser(HttpServletRequest request, String sessionKey) {
        return loginSessionService.requireLogin(request, sessionKey);
    }

    private HttpSession renewSession(HttpServletRequest request) {
        HttpSession session = request.getSession(true);
        request.changeSessionId();
        return session;
    }
}
