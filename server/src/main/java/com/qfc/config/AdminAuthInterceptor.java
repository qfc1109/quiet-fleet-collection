package com.qfc.config;

import com.qfc.auth.LoginUser;
import com.qfc.auth.CurrentUserResolver;
import com.qfc.admin.RbacUserSessionService;
import com.qfc.common.ApiException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
public class AdminAuthInterceptor implements HandlerInterceptor {

    private final RbacUserSessionService rbacUserSessionService;
    private final CurrentUserResolver currentUserResolver;

    public AdminAuthInterceptor(RbacUserSessionService rbacUserSessionService, CurrentUserResolver currentUserResolver) {
        this.rbacUserSessionService = rbacUserSessionService;
        this.currentUserResolver = currentUserResolver;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        LoginUser loginUser = currentUserResolver.resolveAdmin(request);
        if (!RbacUserSessionService.ACCOUNT_TYPE_ADMIN.equals(loginUser.getAccountType())) {
            throw new ApiException("FORBIDDEN", "没有后台操作权限", HttpServletResponse.SC_FORBIDDEN);
        }
        String permission = requiredPermission(request);
        if (!rbacUserSessionService.hasPermission(loginUser.getId(), permission)) {
            throw new ApiException("FORBIDDEN", "没有后台操作权限", HttpServletResponse.SC_FORBIDDEN);
        }
        return true;
    }

    private String requiredPermission(HttpServletRequest request) {
        String path = request.getRequestURI();
        String method = request.getMethod();
        if (path.startsWith("/api/admin/site-users")) {
            return HttpMethod.GET.matches(method) ? "USER_VIEW" : "USER_MANAGE";
        }
        if (path.startsWith("/api/admin/admin-users")) {
            return "ROLE_MANAGE";
        }
        if (path.startsWith("/api/admin/roles")) {
            return HttpMethod.GET.matches(method) ? "ROLE_VIEW" : "ROLE_MANAGE";
        }
        if (path.startsWith("/api/admin/permissions")) {
            return "ROLE_VIEW";
        }
        if (path.startsWith("/api/admin/files")) {
            return "FILE_MANAGE";
        }
        if (path.startsWith("/api/admin/feedback")) {
            return "ISSUE_MANAGE";
        }
        return "PROJECT_MANAGE";
    }
}
