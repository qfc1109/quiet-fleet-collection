package com.qfc.auth;

import com.qfc.admin.RbacUserSessionService;
import com.qfc.common.ApiException;
import com.qfc.config.SessionKeys;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.util.Arrays;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class LoginSessionServiceTest {

    private static final Instant BASE_TIME = Instant.parse("2026-07-08T10:00:00Z");

    @Test
    void establishLoginStoresUserAndUsesTwentyFourHourSessionLifetime() {
        LoginSessionService service = new LoginSessionService(defaultProperties(), fixedClock(BASE_TIME));
        MockHttpServletRequest request = request("10.0.0.1", "Chrome");
        LoginUser user = loginUser(5L, RbacUserSessionService.ACCOUNT_TYPE_SITE_USER);

        service.establishLogin(request, SessionKeys.SITE_LOGIN_USER, user);

        assertEquals(86400, request.getSession(false).getMaxInactiveInterval());
        assertSame(user, request.getSession(false).getAttribute(SessionKeys.SITE_LOGIN_USER));
        assertSame(user, service.requireLogin(request, SessionKeys.SITE_LOGIN_USER));
    }

    @Test
    void latestLoginInvalidatesPreviousSessionForSameAccount() {
        LoginSessionService service = new LoginSessionService(defaultProperties(), fixedClock(BASE_TIME));
        LoginUser user = loginUser(5L, RbacUserSessionService.ACCOUNT_TYPE_SITE_USER);
        MockHttpServletRequest firstRequest = request("10.0.0.1", "Chrome");
        MockHttpServletRequest secondRequest = request("10.0.0.2", "Firefox");
        service.establishLogin(firstRequest, SessionKeys.SITE_LOGIN_USER, user);

        service.establishLogin(secondRequest, SessionKeys.SITE_LOGIN_USER, user);

        ApiException exception = assertThrows(
            ApiException.class,
            () -> service.requireLogin(firstRequest, SessionKeys.SITE_LOGIN_USER)
        );
        assertEquals("ACCOUNT_LOGGED_IN_ELSEWHERE", exception.getCode());
        assertNull(firstRequest.getSession(false).getAttribute(SessionKeys.SITE_LOGIN_USER));
        assertSame(user, service.requireLogin(secondRequest, SessionKeys.SITE_LOGIN_USER));
    }

    @Test
    void expiredLoginRequiresRelogin() {
        MutableClock clock = new MutableClock(BASE_TIME);
        LoginSessionService service = new LoginSessionService(defaultProperties(), clock);
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
        LoginSessionService service = new LoginSessionService(defaultProperties(), fixedClock(BASE_TIME));
        MockHttpServletRequest request = request("10.0.0.1", "Chrome");
        LoginUser user = loginUser(5L, RbacUserSessionService.ACCOUNT_TYPE_SITE_USER);
        service.establishLogin(request, SessionKeys.SITE_LOGIN_USER, user);

        service.invalidateAccount(RbacUserSessionService.ACCOUNT_TYPE_SITE_USER, 5L);

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
