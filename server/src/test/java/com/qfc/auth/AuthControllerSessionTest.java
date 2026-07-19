package com.qfc.auth;

import com.qfc.common.ApiException;
import com.qfc.common.ApiResponse;
import com.qfc.config.SessionKeys;
import java.util.Arrays;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpSession;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthControllerSessionTest {

    @Mock
    private AuthService authService;

    @Mock
    private ActiveSessionRepository activeSessionRepository;

    @Mock
    private JwtTokenProvider jwtTokenProvider;

    @Mock
    private RefreshTokenService refreshTokenService;

    @Mock
    private TokenProperties tokenProperties;

    private final Map<String, String> sessionStore = new ConcurrentHashMap<String, String>();

    private AuthController controller;
    private LoginSessionService loginSessionService;

    @BeforeEach
    void setUp() {
        sessionStore.clear();

        lenient().doAnswer(invocation -> {
            sessionStore.put(invocation.getArgument(0) + ":" + invocation.getArgument(1), invocation.getArgument(2));
            return null;
        }).when(activeSessionRepository).store(anyString(), anyString(), anyString(), anyString(), anyString(), anyLong());

        lenient().when(activeSessionRepository.getLoginToken(anyString(), anyString()))
            .thenAnswer(invocation -> sessionStore.get(invocation.getArgument(0) + ":" + invocation.getArgument(1)));

        lenient().when(activeSessionRepository.isDeviceActive(anyString(), anyString()))
            .thenAnswer(invocation -> sessionStore.containsKey(invocation.getArgument(0) + ":" + invocation.getArgument(1)));

        lenient().when(activeSessionRepository.removeIfMatch(anyString(), anyString(), anyString()))
            .thenAnswer(invocation -> {
                String key = invocation.getArgument(0) + ":" + invocation.getArgument(1);
                String token = invocation.getArgument(2);
                if (token != null && token.equals(sessionStore.get(key))) {
                    sessionStore.remove(key);
                    return true;
                }
                return false;
            });

        lenient().doAnswer(invocation -> {
            sessionStore.remove(invocation.getArgument(0));
            return null;
        }).when(activeSessionRepository).remove(anyString());

        lenient().doAnswer(invocation -> {
            String prefix = invocation.getArgument(0) + ":";
            String currentDeviceId = invocation.getArgument(1);
            sessionStore.keySet().removeIf(key -> key.startsWith(prefix) && !key.equals(prefix + currentDeviceId));
            return null;
        }).when(activeSessionRepository).removeOthers(anyString(), anyString());

        LoginSessionProperties properties = new LoginSessionProperties();
        properties.setSessionLifetimeSeconds(86400);
        loginSessionService = new LoginSessionService(properties, activeSessionRepository);
        CurrentUserResolver currentUserResolver = new CurrentUserResolver(jwtTokenProvider, authService, loginSessionService);
        controller = new AuthController(authService, loginSessionService, jwtTokenProvider, refreshTokenService, tokenProperties, currentUserResolver);
    }

    @Test
    void siteAndAdminLoginUseSeparateSessionAttributes() {
        MockHttpSession session = new MockHttpSession();
        LoginRequest siteRequest = loginRequest("admin", "123456");
        LoginRequest adminRequest = loginRequest("admin", "123456");
        LoginUser siteUser = loginUser(10L, "SITE_USER");
        LoginUser adminUser = loginUser(1L, "ADMIN");
        when(authService.loginSite("admin", "123456")).thenReturn(siteUser);
        when(authService.loginAdmin("admin", "123456")).thenReturn(adminUser);

        controller.login(siteRequest, requestWithSession(session));
        controller.adminLogin(adminRequest, requestWithSession(session));

        assertSame(siteUser, session.getAttribute(SessionKeys.SITE_LOGIN_USER));
        assertSame(adminUser, session.getAttribute(SessionKeys.ADMIN_LOGIN_USER));
        assertNull(session.getAttribute(SessionKeys.LOGIN_USER));
    }

    @Test
    void siteLoginChangesSessionIdBeforeStoringLoginUser() {
        MockHttpServletRequest servletRequest = new MockHttpServletRequest();
        MockHttpSession session = (MockHttpSession) servletRequest.getSession(true);
        String originalSessionId = session.getId();
        LoginRequest request = loginRequest("player01", "123456");
        LoginUser siteUser = loginUser(10L, "SITE_USER");
        when(authService.loginSite("player01", "123456")).thenReturn(siteUser);

        controller.login(request, servletRequest);

        assertNotEquals(originalSessionId, servletRequest.getSession(false).getId());
        assertSame(siteUser, servletRequest.getSession(false).getAttribute(SessionKeys.SITE_LOGIN_USER));
    }

    @Test
    void adminLoginChangesSessionIdBeforeStoringLoginUser() {
        MockHttpServletRequest servletRequest = new MockHttpServletRequest();
        MockHttpSession session = (MockHttpSession) servletRequest.getSession(true);
        String originalSessionId = session.getId();
        LoginRequest request = loginRequest("admin", "123456");
        LoginUser adminUser = loginUser(1L, "ADMIN");
        when(authService.loginAdmin("admin", "123456")).thenReturn(adminUser);

        controller.adminLogin(request, servletRequest);

        assertNotEquals(originalSessionId, servletRequest.getSession(false).getId());
        assertSame(adminUser, servletRequest.getSession(false).getAttribute(SessionKeys.ADMIN_LOGIN_USER));
    }

    @Test
    void multipleSiteSessionsRemainValidUntilRevoked() {
        MockHttpServletRequest firstRequest = new MockHttpServletRequest();
        firstRequest.setRemoteAddr("10.0.0.1");
        firstRequest.addHeader("User-Agent", "Chrome");
        MockHttpServletRequest secondRequest = new MockHttpServletRequest();
        secondRequest.setRemoteAddr("10.0.0.2");
        secondRequest.addHeader("User-Agent", "Firefox");
        LoginRequest request = loginRequest("player01", "123456");
        LoginUser siteUser = loginUser(10L, "SITE_USER");
        when(authService.loginSite("player01", "123456")).thenReturn(siteUser);
        controller.login(request, firstRequest);
        controller.login(request, secondRequest);
        when(authService.currentUser(10L, "SITE_USER")).thenReturn(siteUser);

        assertSame(siteUser, controller.me(firstRequest).getData());
        assertSame(siteUser, controller.me(secondRequest).getData());
    }

    @Test
    void revokeOtherSessionsInvalidatesPreviousSiteSession() {
        MockHttpServletRequest firstRequest = new MockHttpServletRequest();
        firstRequest.setRemoteAddr("10.0.0.1");
        firstRequest.addHeader("User-Agent", "Chrome");
        MockHttpServletRequest secondRequest = new MockHttpServletRequest();
        secondRequest.setRemoteAddr("10.0.0.2");
        secondRequest.addHeader("User-Agent", "Firefox");
        LoginRequest request = loginRequest("player01", "123456");
        LoginUser siteUser = loginUser(10L, "SITE_USER");
        when(authService.loginSite("player01", "123456")).thenReturn(siteUser);
        when(authService.currentUser(10L, "SITE_USER")).thenReturn(siteUser);
        controller.login(request, firstRequest);
        controller.login(request, secondRequest);

        controller.revokeOtherSessions(secondRequest);

        ApiException exception = assertThrows(ApiException.class, () -> controller.me(firstRequest));

        assertEquals("UNAUTHORIZED", exception.getCode());
        assertNull(firstRequest.getSession(false).getAttribute(SessionKeys.SITE_LOGIN_USER));
        assertSame(siteUser, controller.me(secondRequest).getData());
    }

    @Test
    void siteLogoutKeepsAdminSession() {
        MockHttpSession session = new MockHttpSession();
        LoginUser siteUser = loginUser(10L, "SITE_USER");
        LoginUser adminUser = loginUser(1L, "ADMIN");
        session.setAttribute(SessionKeys.SITE_LOGIN_USER, siteUser);
        session.setAttribute(SessionKeys.ADMIN_LOGIN_USER, adminUser);
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setSession(session);

        controller.logout(request);

        assertNull(session.getAttribute(SessionKeys.SITE_LOGIN_USER));
        assertSame(adminUser, session.getAttribute(SessionKeys.ADMIN_LOGIN_USER));
    }

    @Test
    void adminLogoutKeepsSiteSession() {
        MockHttpSession session = new MockHttpSession();
        LoginUser siteUser = loginUser(10L, "SITE_USER");
        LoginUser adminUser = loginUser(1L, "ADMIN");
        session.setAttribute(SessionKeys.SITE_LOGIN_USER, siteUser);
        session.setAttribute(SessionKeys.ADMIN_LOGIN_USER, adminUser);
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setSession(session);

        controller.adminLogout(request);

        assertSame(siteUser, session.getAttribute(SessionKeys.SITE_LOGIN_USER));
        assertNull(session.getAttribute(SessionKeys.ADMIN_LOGIN_USER));
    }

    @Test
    void siteMeReadsOnlySiteSession() {
        MockHttpSession session = new MockHttpSession();
        LoginUser siteUser = loginUser(10L, "SITE_USER");
        LoginUser adminUser = loginUser(1L, "ADMIN");
        LoginUser refreshedSiteUser = loginUser(10L, "SITE_USER");
        session.setAttribute(SessionKeys.SITE_LOGIN_USER, siteUser);
        session.setAttribute(SessionKeys.ADMIN_LOGIN_USER, adminUser);
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setSession(session);
        when(authService.currentUser(10L, "SITE_USER")).thenReturn(refreshedSiteUser);

        ApiResponse<LoginUser> response = controller.me(request);

        assertSame(refreshedSiteUser, response.getData());
        assertSame(refreshedSiteUser, session.getAttribute(SessionKeys.SITE_LOGIN_USER));
        assertSame(adminUser, session.getAttribute(SessionKeys.ADMIN_LOGIN_USER));
        verify(authService).currentUser(10L, "SITE_USER");
    }

    @Test
    void adminMeReadsOnlyAdminSession() {
        MockHttpSession session = new MockHttpSession();
        LoginUser siteUser = loginUser(10L, "SITE_USER");
        LoginUser adminUser = loginUser(1L, "ADMIN");
        LoginUser refreshedAdminUser = loginUser(1L, "ADMIN");
        session.setAttribute(SessionKeys.SITE_LOGIN_USER, siteUser);
        session.setAttribute(SessionKeys.ADMIN_LOGIN_USER, adminUser);
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setSession(session);
        when(authService.currentUser(1L, "ADMIN")).thenReturn(refreshedAdminUser);

        ApiResponse<LoginUser> response = controller.adminMe(request);

        assertSame(refreshedAdminUser, response.getData());
        assertSame(siteUser, session.getAttribute(SessionKeys.SITE_LOGIN_USER));
        assertSame(refreshedAdminUser, session.getAttribute(SessionKeys.ADMIN_LOGIN_USER));
        verify(authService).currentUser(1L, "ADMIN");
    }

    @Test
    void siteMeIgnoresAdminOnlySession() {
        MockHttpSession session = new MockHttpSession();
        session.setAttribute(SessionKeys.ADMIN_LOGIN_USER, loginUser(1L, "ADMIN"));
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setSession(session);

        ApiException exception = assertThrows(ApiException.class, () -> controller.me(request));

        assertEquals("UNAUTHORIZED", exception.getCode());
    }

    private LoginRequest loginRequest(String username, String password) {
        LoginRequest request = new LoginRequest();
        request.setUsername(username);
        request.setPassword(password);
        return request;
    }

    private MockHttpServletRequest requestWithSession(MockHttpSession session) {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setSession(session);
        return request;
    }

    private LoginUser loginUser(Long id, String accountType) {
        return new LoginUser(
            id,
            "admin",
            "admin",
            accountType,
            "ENABLED",
            "ADMIN".equals(accountType) ? Arrays.asList("SUPER_ADMIN") : Arrays.asList(),
            "ADMIN".equals(accountType) ? Arrays.asList("USER_VIEW") : Arrays.asList()
        );
    }
}
