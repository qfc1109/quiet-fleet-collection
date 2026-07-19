package com.qfc.auth;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.qfc.common.ApiException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class JwtTokenProviderTest {

    private JwtTokenProvider provider;

    @BeforeEach
    void setUp() {
        TokenProperties props = new TokenProperties();
        props.setSecret("test-secret-key-for-jwt-test-at-least-32-char");
        props.setAccessTokenExpirySeconds(900);
        provider = new JwtTokenProvider(props);
    }

    @Test
    void createAndValidateAccessToken() {
        String token = provider.createAccessToken(42L, "SITE_USER");
        assertNotNull(token);
        assertTrue(token.split("\\.").length == 3, "JWT should have 3 parts");

        assertEquals(Long.valueOf(42L), provider.getUserId(token));
        assertEquals("SITE_USER", provider.getAccountType(token));
    }

    @Test
    void adminAccountTypeIsPreservedInToken() {
        String token = provider.createAccessToken(1L, "ADMIN");
        assertEquals(Long.valueOf(1L), provider.getUserId(token));
        assertEquals("ADMIN", provider.getAccountType(token));
    }

    @Test
    void expiredTokenThrowsApiException() {
        TokenProperties shortLived = new TokenProperties();
        shortLived.setSecret("test-secret-for-expiry-test-32-chars-long!!");
        shortLived.setAccessTokenExpirySeconds(0); // already expired
        JwtTokenProvider shortProvider = new JwtTokenProvider(shortLived);

        String token = shortProvider.createAccessToken(5L, "SITE_USER");
        assertThrows(ApiException.class, () -> shortProvider.getUserId(token));
    }

    @Test
    void invalidTokenThrowsApiException() {
        assertThrows(ApiException.class, () -> provider.getUserId("invalid.jwt.token"));
        assertThrows(ApiException.class, () -> provider.getUserId(""));
        assertThrows(ApiException.class, () -> provider.getUserId("eyJhbGciOiJIUzI1NiJ9.dGVzdA.signature"));
    }

    @Test
    void shortJwtSecretFailsFast() {
        TokenProperties props = new TokenProperties();
        props.setSecret("too-short");
        props.setAccessTokenExpirySeconds(900);

        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> new JwtTokenProvider(props)
        );
        assertTrue(exception.getMessage().contains("qfc.auth.token.secret"));
    }
}
