package com.qfc.config;

import com.qfc.common.ApiException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertThrows;

class CsrfTokenInterceptorTest {

    private CsrfTokenInterceptor interceptor;

    @BeforeEach
    void setUp() {
        interceptor = new CsrfTokenInterceptor();
    }

    @Test
    void allowsSafeGetWithoutToken() {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/public/projects");

        assertTrue(interceptor.preHandle(request, new MockHttpServletResponse(), new Object()));
    }

    @Test
    void rejectsUnsafeRequestWithoutSessionToken() {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/space/projects");

        ApiException exception = assertThrows(
            ApiException.class,
            () -> interceptor.preHandle(request, new MockHttpServletResponse(), new Object())
        );

        assertEquals("CSRF_TOKEN_INVALID", exception.getCode());
        assertEquals(403, exception.getHttpStatus());
    }

    @Test
    void acceptsUnsafeRequestWhenHeaderMatchesSessionToken() {
        MockHttpServletRequest request = new MockHttpServletRequest("PUT", "/api/space/projects/1");
        request.getSession(true).setAttribute(SessionKeys.CSRF_TOKEN, "known-token");
        request.addHeader("X-CSRF-Token", "known-token");

        assertTrue(interceptor.preHandle(request, new MockHttpServletResponse(), new Object()));
    }

    @Test
    void bearerTokenPostSkipsCsrfCheck() {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/space/projects");
        request.addHeader("Authorization", "Bearer access-token");

        assertTrue(interceptor.preHandle(request, new MockHttpServletResponse(), new Object()));
    }

    @Test
    void tokenEndpointsSkipCsrfCheck() {
        assertTrue(interceptor.preHandle(new MockHttpServletRequest("POST", "/api/auth/token"), new MockHttpServletResponse(), new Object()));
        assertTrue(interceptor.preHandle(new MockHttpServletRequest("POST", "/api/auth/admin/token"), new MockHttpServletResponse(), new Object()));
        assertTrue(interceptor.preHandle(new MockHttpServletRequest("POST", "/api/auth/refresh"), new MockHttpServletResponse(), new Object()));
    }
}
