package com.qfc.auth;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.qfc.common.ApiException;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.SetOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

@ExtendWith(MockitoExtension.class)
class RefreshTokenServiceTest {

    @Mock
    private StringRedisTemplate redisTemplate;

    @Mock
    private ValueOperations<String, String> valueOps;

    @Mock
    private SetOperations<String, String> setOps;

    private TokenProperties properties;
    private RefreshTokenService service;

    @BeforeEach
    void setUp() {
        properties = new TokenProperties();
        properties.setRefreshTokenExpirySeconds(604800);
        lenient().when(redisTemplate.opsForValue()).thenReturn(valueOps);
        lenient().when(redisTemplate.opsForSet()).thenReturn(setOps);
        service = new RefreshTokenService(redisTemplate, properties);
    }

    @Test
    void issueRefreshTokenStoresHashInRedis() {
        String rawToken = service.issueRefreshToken("SITE_USER", 42L, "10.0.0.1", "Chrome");

        ArgumentCaptor<String> keyCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> valueCaptor = ArgumentCaptor.forClass(String.class);
        verify(valueOps).set(keyCaptor.capture(), valueCaptor.capture(), eq(604800L), eq(TimeUnit.SECONDS));

        assertTrue(keyCaptor.getValue().startsWith("qfc:refresh:"));
        assertTrue(valueCaptor.getValue().contains("\"accountType\":\"SITE_USER\""));
        assertTrue(valueCaptor.getValue().contains("\"userId\":42"));
        assertTrue(valueCaptor.getValue().contains("10.0.0.1"));
        assertTrue(valueCaptor.getValue().contains("Chrome"));
        verify(setOps).add(anyString(), anyString());
    }

    @Test
    void consumeValidRefreshTokenReturnsAccountInfo() {
        String rawToken = "test-raw-token";
        String storedHash = sha256(rawToken);
        String storedValue = "{\"accountType\":\"ADMIN\",\"userId\":1,\"deviceId\":\"device-1\",\"clientIp\":\"10.0.0.1\",\"userAgent\":\"Firefox\"}";

        when(valueOps.getAndDelete("qfc:refresh:" + storedHash)).thenReturn(storedValue);

        String[] result = service.consumeRefreshToken(rawToken);
        assertEquals("ADMIN", result[0]);
        assertEquals("1", result[1]);
        assertEquals("device-1", result[2]);
        verify(setOps).remove("qfc:refresh:list:ADMIN:1", storedHash);
    }

    @Test
    void consumeInvalidRefreshTokenThrows() {
        when(valueOps.getAndDelete(anyString())).thenReturn(null);
        assertThrows(ApiException.class, () -> service.consumeRefreshToken("invalid-token"));
    }

    @Test
    void revokeAllDeletesAllTokens() {
        when(setOps.members("qfc:refresh:list:SITE_USER:5"))
            .thenReturn(java.util.Collections.singleton("hash1"));

        service.revokeAll("SITE_USER", 5L);

        verify(redisTemplate).delete(java.util.Arrays.asList("qfc:refresh:hash1"));
        verify(redisTemplate).delete("qfc:refresh:list:SITE_USER:5");
    }

    @Test
    void revokeAllWithNoTokensDoesNotCrash() {
        when(setOps.members(anyString()))
            .thenReturn(java.util.Collections.emptySet());
        service.revokeAll("SITE_USER", 99L);
        // Should complete without error
    }

    private static String sha256(String value) {
        try {
            java.security.MessageDigest digest = java.security.MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(value.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder(hash.length * 2);
            for (byte b : hash) {
                hex.append("0123456789abcdef".charAt((b >> 4) & 0xf));
                hex.append("0123456789abcdef".charAt(b & 0xf));
            }
            return hex.toString();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
