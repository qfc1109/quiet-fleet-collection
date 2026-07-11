package com.qfc.config;

import com.qfc.admin.RbacUserSessionService;
import com.qfc.auth.LoginSessionProperties;
import com.qfc.auth.LoginSessionService;
import com.qfc.auth.LoginUser;
import com.qfc.common.ApiException;
import java.util.Arrays;
import javax.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
class AdminAuthInterceptorTest {

    @Mock
    private RbacUserSessionService rbacUserSessionService;

    private AdminAuthInterceptor interceptor;
    private LoginSessionService loginSessionService;

    @BeforeEach
    void setUp() {
        LoginSessionProperties properties = new LoginSessionProperties();
        properties.setSessionLifetimeSeconds(86400);
        loginSessionService = new LoginSessionService(properties);
        interceptor = new AdminAuthInterceptor(rbacUserSessionService, loginSessionService);
    }

    @Test
    void rejectsSiteUserBeforeCheckingAdminPermissionEvenWhenIdsOverlap() {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/admin/projects");
        request.getSession(true).setAttribute(
            SessionKeys.SITE_LOGIN_USER,
            new LoginUser(
                1L,
                "player01",
                "网站用户",
                "SITE_USER",
                "ENABLED",
                Arrays.asList(),
                Arrays.asList()
            )
        );
        MockHttpServletResponse response = new MockHttpServletResponse();

        ApiException exception = assertThrows(
            ApiException.class,
            () -> interceptor.preHandle(request, response, new Object())
        );

        assertEquals("UNAUTHORIZED", exception.getCode());
        assertEquals(HttpServletResponse.SC_UNAUTHORIZED, exception.getHttpStatus());
        verifyNoInteractions(rbacUserSessionService);
    }

    @Test
    void acceptsAdminSessionWithRequiredPermission() {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/admin/projects");
        request.getSession(true).setAttribute(
            SessionKeys.ADMIN_LOGIN_USER,
            new LoginUser(
                1L,
                "admin",
                "后台管理员",
                "ADMIN",
                "ENABLED",
                Arrays.asList("SUPER_ADMIN"),
                Arrays.asList("PROJECT_MANAGE")
            )
        );
        MockHttpServletResponse response = new MockHttpServletResponse();
        when(rbacUserSessionService.hasPermission(1L, "PROJECT_MANAGE")).thenReturn(true);

        assertTrue(interceptor.preHandle(request, response, new Object()));
    }

    @Test
    void feedbackAdminApiRequiresIssueManagePermission() {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/admin/feedback");
        request.getSession(true).setAttribute(
            SessionKeys.ADMIN_LOGIN_USER,
            new LoginUser(
                1L,
                "admin",
                "后台管理员",
                "ADMIN",
                "ENABLED",
                Arrays.asList("NORMAL_ADMIN"),
                Arrays.asList("ISSUE_MANAGE")
            )
        );
        MockHttpServletResponse response = new MockHttpServletResponse();
        when(rbacUserSessionService.hasPermission(1L, "ISSUE_MANAGE")).thenReturn(true);

        assertTrue(interceptor.preHandle(request, response, new Object()));
    }

    @Test
    void rejectsOldAdminSessionAfterLatestLogin() {
        LoginUser admin = new LoginUser(
            1L,
            "admin",
            "后台管理员",
            "ADMIN",
            "ENABLED",
            Arrays.asList("SUPER_ADMIN"),
            Arrays.asList("PROJECT_MANAGE")
        );
        MockHttpServletRequest firstRequest = new MockHttpServletRequest("GET", "/api/admin/projects");
        firstRequest.setRemoteAddr("10.0.0.1");
        firstRequest.addHeader("User-Agent", "Chrome");
        MockHttpServletRequest secondRequest = new MockHttpServletRequest("GET", "/api/admin/projects");
        secondRequest.setRemoteAddr("10.0.0.2");
        secondRequest.addHeader("User-Agent", "Firefox");
        loginSessionService.establishLogin(firstRequest, SessionKeys.ADMIN_LOGIN_USER, admin);
        loginSessionService.establishLogin(secondRequest, SessionKeys.ADMIN_LOGIN_USER, admin);

        ApiException exception = assertThrows(
            ApiException.class,
            () -> interceptor.preHandle(firstRequest, new MockHttpServletResponse(), new Object())
        );

        assertEquals("ACCOUNT_LOGGED_IN_ELSEWHERE", exception.getCode());
        verifyNoInteractions(rbacUserSessionService);
    }
}
