package com.qfc.auth;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.qfc.common.ApiException;
import java.security.SecureRandom;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import java.util.Collections;
import java.util.concurrent.TimeUnit;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class RefreshTokenService {

    private static final String REFRESH_KEY_PREFIX = "qfc:refresh:";
    private static final String REFRESH_LIST_PREFIX = "qfc:refresh:list:";

    private final StringRedisTemplate redisTemplate;
    private final TokenProperties properties;
    private final ObjectMapper objectMapper = new ObjectMapper();

    private static final char[] HEX_CHARS = "0123456789abcdef".toCharArray();
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    public RefreshTokenService(StringRedisTemplate redisTemplate, TokenProperties properties) {
        this.redisTemplate = redisTemplate;
        this.properties = properties;
    }

    /** Issue a new refresh token for the given account. */
    public String issueRefreshToken(String accountType, Long userId, String clientIp, String userAgent) {
        return issueRefreshToken(accountType, userId, "", clientIp, userAgent);
    }

    public String issueRefreshToken(String accountType, Long userId, String deviceId, String clientIp, String userAgent) {
        String rawToken = createRawToken();
        String tokenHash = sha256(rawToken);
        long ttl = properties.getRefreshTokenExpirySeconds();
        String value = serialize(new RefreshTokenRecord(accountType, userId, deviceId, clientIp, userAgent));
        String listKey = refreshListKey(accountType, userId);

        redisTemplate.opsForValue().set(
            REFRESH_KEY_PREFIX + tokenHash,
            value,
            ttl,
            TimeUnit.SECONDS
        );
        redisTemplate.opsForSet().add(
            listKey,
            tokenHash
        );
        redisTemplate.expire(
            listKey,
            ttl,
            TimeUnit.SECONDS
        );
        return rawToken;
    }

    /**
     * Atomically consume (validate + delete) a refresh token.
     * Returns [accountType, userId] or throws if invalid/expired.
     */
    public String[] consumeRefreshToken(String rawToken) {
        String tokenHash = sha256(rawToken);
        String key = REFRESH_KEY_PREFIX + tokenHash;
        String value = redisTemplate.opsForValue().getAndDelete(key);
        if (value == null) {
            throw new ApiException("INVALID_REFRESH_TOKEN", "刷新令牌无效或已过期", 401);
        }
        RefreshTokenRecord record = deserialize(value);
        redisTemplate.opsForSet().remove(refreshListKey(record.getAccountType(), record.getUserId()), tokenHash);
        return new String[]{
            record.getAccountType(),
            String.valueOf(record.getUserId()),
            record.getDeviceId() == null ? "" : record.getDeviceId()
        };
    }

    /** Revoke all refresh tokens for the given account. */
    public void revokeAll(String accountType, Long userId) {
        String listKey = REFRESH_LIST_PREFIX + accountType + ":" + userId;
        java.util.Set<String> members = redisTemplate.opsForSet().members(listKey);
        String[] tokenHashes = members == null ? new String[0] : members.toArray(new String[0]);
        if (tokenHashes.length > 0) {
            String[] keysToDelete = new String[tokenHashes.length];
            for (int i = 0; i < tokenHashes.length; i++) {
                keysToDelete[i] = REFRESH_KEY_PREFIX + tokenHashes[i];
            }
            redisTemplate.delete(java.util.Arrays.asList(keysToDelete));
        }
        redisTemplate.delete(listKey);
    }

    public void revokeOthers(String accountType, Long userId, String currentDeviceId) {
        String listKey = refreshListKey(accountType, userId);
        java.util.Set<String> tokenHashes = redisTemplate.opsForSet().members(listKey);
        if (tokenHashes == null || tokenHashes.isEmpty()) {
            return;
        }
        for (String tokenHash : tokenHashes) {
            String tokenKey = REFRESH_KEY_PREFIX + tokenHash;
            String value = redisTemplate.opsForValue().get(tokenKey);
            if (value == null) {
                redisTemplate.opsForSet().remove(listKey, tokenHash);
                continue;
            }
            RefreshTokenRecord record = deserialize(value);
            if (!StringUtils.hasText(record.getDeviceId()) || !record.getDeviceId().equals(currentDeviceId)) {
                redisTemplate.delete(tokenKey);
                redisTemplate.opsForSet().remove(listKey, tokenHash);
            }
        }
    }

    /** Count active refresh tokens for an account (for listing active sessions). */
    public long countRefreshTokens(String accountType, Long userId) {
        Long count = redisTemplate.opsForSet().size(
            REFRESH_LIST_PREFIX + accountType + ":" + userId
        );
        return count == null ? 0L : count;
    }

    private static String createRawToken() {
        byte[] bytes = new byte[32];
        SECURE_RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private static String refreshListKey(String accountType, Long userId) {
        return REFRESH_LIST_PREFIX + accountType + ":" + userId;
    }

    private String serialize(RefreshTokenRecord record) {
        try {
            return objectMapper.writeValueAsString(record);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Failed to serialize refresh token", exception);
        }
    }

    private RefreshTokenRecord deserialize(String value) {
        try {
            if (value != null && value.startsWith("{")) {
                return objectMapper.readValue(value, RefreshTokenRecord.class);
            }
            String[] result = value.split(":", 4);
            return new RefreshTokenRecord(result[0], Long.valueOf(result[1]), "", result.length > 2 ? result[2] : "", result.length > 3 ? result[3] : "");
        } catch (Exception exception) {
            throw new ApiException("INVALID_REFRESH_TOKEN", "刷新令牌无效或已过期", 401);
        }
    }

    private static String sha256(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(value.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder(hash.length * 2);
            for (byte b : hash) {
                hex.append(HEX_CHARS[(b >> 4) & 0xf]);
                hex.append(HEX_CHARS[b & 0xf]);
            }
            return hex.toString();
        } catch (NoSuchAlgorithmException exception) {
            throw new RuntimeException("SHA-256 not available", exception);
        }
    }
}
