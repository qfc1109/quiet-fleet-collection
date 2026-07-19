package com.qfc.auth;

import com.qfc.admin.RbacUserSessionService;
import com.qfc.common.ApiException;
import com.qfc.config.SessionKeys;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.util.Arrays;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.mock.web.MockHttpServletRequest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class LoginSessionServiceTest {

    private static final Instant BASE_TIME = Instant.parse("2026-07-08T10:00:00Z");

    @Test
    void establishLoginStoresUserAndUsesTwentyFourHourSessionLifetime() {
        ActiveSessionRepository repo = mock(ActiveSessionRepository.class);
        Map<String, String> activeStore = stubActiveRepo(repo);
        LoginSessionService service = new LoginSessionService(defaultProperties(), repo, fixedClock(BASE_TIME));
        MockHttpServletRequest request = request("10.0.0.1", "Chrome");
        LoginUser user = loginUser(5L, RbacUserSessionService.ACCOUNT_TYPE_SITE_USER);

        service.establishLogin(request, SessionKeys.SITE_LOGIN_USER, user);

        assertEquals(86400, request.getSession(false).getMaxInactiveInterval());
        assertSame(user, request.getSession(false).getAttribute(SessionKeys.SITE_LOGIN_USER));

        ArgumentCaptor<String> deviceCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> tokenCaptor = ArgumentCaptor.forClass(String.class);
        verify(repo).store(eq("SITE_USER:5"), deviceCaptor.capture(), tokenCaptor.capture(), eq("10.0.0.1"), eq("Chrome"), eq(86400L));

        when(repo.getLoginToken("SITE_USER:5", deviceCaptor.getValue())).thenReturn(tokenCaptor.getValue());
        assertSame(user, service.requireLogin(request, SessionKeys.SITE_LOGIN_USER));
    }

    @Test
    void multipleLoginsRemainValidUntilOtherDevicesAreInvalidated() {
        ActiveSessionRepository repo = mock(ActiveSessionRepository.class);
        stubActiveRepo(repo);
        LoginSessionService service = new LoginSessionService(defaultProperties(), repo, fixedClock(BASE_TIME));
        LoginUser user = loginUser(5L, RbacUserSessionService.ACCOUNT_TYPE_SITE_USER);
        MockHttpServletRequest firstRequest = request("10.0.0.1", "Chrome");

        service.establishLogin(firstRequest, SessionKeys.SITE_LOGIN_USER, user);
        MockHttpServletRequest secondRequest = request("10.0.0.2", "Firefox");
        service.establishLogin(secondRequest, SessionKeys.SITE_LOGIN_USER, user);

        assertSame(user, service.requireLogin(firstRequest, SessionKeys.SITE_LOGIN_USER));
        assertSame(user, service.requireLogin(secondRequest, SessionKeys.SITE_LOGIN_USER));

        service.invalidateOtherDevices(
            RbacUserSessionService.ACCOUNT_TYPE_SITE_USER,
            5L,
            service.currentDeviceId(secondRequest, SessionKeys.SITE_LOGIN_USER)
        );
        ApiException exception = assertThrows(
            ApiException.class,
            () -> service.requireLogin(firstRequest, SessionKeys.SITE_LOGIN_USER)
        );
        assertEquals("UNAUTHORIZED", exception.getCode());
        assertNull(firstRequest.getSession(false).getAttribute(SessionKeys.SITE_LOGIN_USER));
        assertSame(user, service.requireLogin(secondRequest, SessionKeys.SITE_LOGIN_USER));
    }

    @Test
    void expiredLoginRequiresRelogin() {
        ActiveSessionRepository repo = mock(ActiveSessionRepository.class);
        stubActiveRepo(repo);
        MutableClock clock = new MutableClock(BASE_TIME);
        LoginSessionService service = new LoginSessionService(defaultProperties(), repo, clock);
        MockHttpServletRequest request = request("10.0.0.1", "Chrome");
        LoginUser user = loginUser(5L, RbacUserSessionService.ACCOUNT_TYPE_SITE_USER);
        service.establishLogin(request, SessionKeys.SITE_LOGIN_USER, user);

        clock.plus(Duration.ofHours(24).plusSeconds(1));

        ApiException exception = assertThrows(
            ApiException.class,
            () -> service.requireLogin(request, SessionKeys.SITE_LOGIN_USER)
        );
        assertEquals("UNAUTHORIZED", exception.getCode());
        assertEquals("登录已过期，请重新登录", exception.getMessage());
        assertNull(request.getSession(false).getAttribute(SessionKeys.SITE_LOGIN_USER));
    }

    @Test
    void invalidatedAccountRequiresRelogin() {
        ActiveSessionRepository repo = mock(ActiveSessionRepository.class);
        stubActiveRepo(repo);
        LoginSessionService service = new LoginSessionService(defaultProperties(), repo, fixedClock(BASE_TIME));
        MockHttpServletRequest request = request("10.0.0.1", "Chrome");
        LoginUser user = loginUser(5L, RbacUserSessionService.ACCOUNT_TYPE_SITE_USER);
        service.establishLogin(request, SessionKeys.SITE_LOGIN_USER, user);
        assertSame(user, service.requireLogin(request, SessionKeys.SITE_LOGIN_USER));

        service.invalidateAccount(RbacUserSessionService.ACCOUNT_TYPE_SITE_USER, 5L);
        verify(repo).remove("SITE_USER:5");

        ApiException exception = assertThrows(
            ApiException.class,
            () -> service.requireLogin(request, SessionKeys.SITE_LOGIN_USER)
        );
        assertEquals("UNAUTHORIZED", exception.getCode());
        assertEquals("登录状态已失效，请重新登录", exception.getMessage());
        assertNull(request.getSession(false).getAttribute(SessionKeys.SITE_LOGIN_USER));
    }

    private LoginSessionProperties defaultProperties() {
        LoginSessionProperties properties = new LoginSessionProperties();
        properties.setSessionLifetimeSeconds(86400);
        return properties;
    }

    private Clock fixedClock(Instant instant) {
        return Clock.fixed(instant, ZoneId.of("UTC"));
    }

    private MockHttpServletRequest request(String ip, String userAgent) {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr(ip);
        request.addHeader("User-Agent", userAgent);
        return request;
    }

    private LoginUser loginUser(Long id, String accountType) {
        return new LoginUser(
            id,
            "user" + id,
            "用户" + id,
            accountType,
            "ENABLED",
            Arrays.asList(),
            Arrays.asList()
        );
    }

    private Map<String, String> stubActiveRepo(ActiveSessionRepository repo) {
        Map<String, String> store = new ConcurrentHashMap<String, String>();
        lenient().doAnswer(invocation -> {
            store.put(invocation.getArgument(0) + ":" + invocation.getArgument(1), invocation.getArgument(2));
            return null;
        }).when(repo).store(anyString(), anyString(), anyString(), anyString(), anyString(), anyLong());
        lenient().when(repo.getLoginToken(anyString(), anyString()))
            .thenAnswer(invocation -> store.get(invocation.getArgument(0) + ":" + invocation.getArgument(1)));
        lenient().when(repo.removeIfMatch(anyString(), anyString(), anyString()))
            .thenAnswer(invocation -> {
                String key = invocation.getArgument(0) + ":" + invocation.getArgument(1);
                String token = invocation.getArgument(2);
                if (token != null && token.equals(store.get(key))) {
                    store.remove(key);
                    return true;
                }
                return false;
            });
        lenient().doAnswer(invocation -> {
            String prefix = invocation.getArgument(0) + ":";
            store.keySet().removeIf(key -> key.startsWith(prefix));
            return null;
        }).when(repo).remove(anyString());
        lenient().doAnswer(invocation -> {
            String prefix = invocation.getArgument(0) + ":";
            String currentDeviceId = invocation.getArgument(1);
            store.keySet().removeIf(key -> key.startsWith(prefix) && !key.equals(prefix + currentDeviceId));
            return null;
        }).when(repo).removeOthers(anyString(), anyString());
        return store;
    }

    private static final class MutableClock extends Clock {
        private Instant instant;

        private MutableClock(Instant instant) {
            this.instant = instant;
        }

        private void plus(Duration duration) {
            instant = instant.plus(duration);
        }

        @Override
        public ZoneId getZone() {
            return ZoneId.of("UTC");
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return Clock.fixed(instant, zone);
        }

        @Override
        public Instant instant() {
            return instant;
        }
    }
}
