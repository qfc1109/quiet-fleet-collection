package com.qfc.auth;

import com.qfc.common.ApiException;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import javax.crypto.SecretKey;
import org.springframework.stereotype.Component;

@Component
public class JwtTokenProvider {

    private static final String CLAIM_ACCOUNT_TYPE = "act";
    private static final String CLAIM_USER_ID = "uid";

    private final SecretKey secretKey;
    private final TokenProperties properties;

    public JwtTokenProvider(TokenProperties properties) {
        this.properties = properties;
        byte[] keyBytes = properties.getSecret().getBytes(StandardCharsets.UTF_8);
        if (keyBytes.length < 32) {
            throw new IllegalArgumentException("qfc.auth.token.secret must be at least 32 bytes");
        }
        this.secretKey = Keys.hmacShaKeyFor(keyBytes);
    }

    public String createAccessToken(Long userId, String accountType) {
        return createAccessToken(userId, accountType, null);
    }

    public String createAccessToken(Long userId, String accountType, String deviceId) {
        long now = System.currentTimeMillis();
        io.jsonwebtoken.JwtBuilder builder = Jwts.builder()
            .claim(CLAIM_USER_ID, userId)
            .claim(CLAIM_ACCOUNT_TYPE, accountType)
            .issuedAt(new Date(now))
            .expiration(new Date(now + properties.getAccessTokenExpirySeconds() * 1000L))
            .signWith(secretKey);
        if (deviceId != null && !deviceId.isEmpty()) {
            builder.claim("did", deviceId);
        }
        return builder.compact();
    }

    public Long getUserId(String token) {
        return parseClaims(token).get(CLAIM_USER_ID, Long.class);
    }

    public String getAccountType(String token) {
        return parseClaims(token).get(CLAIM_ACCOUNT_TYPE, String.class);
    }

    public String getDeviceId(String token) {
        return parseClaims(token).get("did", String.class);
    }

    private Claims parseClaims(String token) {
        try {
            return Jwts.parser()
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
        } catch (Exception exception) {
            throw new ApiException("INVALID_TOKEN", "登录令牌无效或已过期", 401);
        }
    }
}
