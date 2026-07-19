package com.qfc.auth;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.util.StringUtils;

@Repository
public class ActiveSessionRepository {

    private static final String TOKEN_KEY_PREFIX = "qfc:session:active:";
    private static final String DEVICE_SET_PREFIX = "qfc:session:devices:";
    private static final String DEVICE_INFO_PREFIX = "qfc:session:device:";
    private static final String FIELD_TOKEN = "loginToken";
    private static final String FIELD_IP = "clientIp";
    private static final String FIELD_USER_AGENT = "userAgent";

    private final StringRedisTemplate redisTemplate;

    public ActiveSessionRepository(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    public void store(String accountKey, String loginToken, long ttlSeconds) {
        store(accountKey, "default", loginToken, "", "", ttlSeconds);
    }

    public void store(String accountKey, String deviceId, String loginToken, String clientIp, String userAgent, long ttlSeconds) {
        String tokenKey = tokenKey(accountKey, deviceId);
        String deviceSetKey = deviceSetKey(accountKey);
        String deviceInfoKey = deviceInfoKey(accountKey, deviceId);

        redisTemplate.opsForValue().set(
            tokenKey,
            loginToken,
            ttlSeconds,
            TimeUnit.SECONDS
        );
        redisTemplate.opsForSet().add(deviceSetKey, deviceId);
        redisTemplate.expire(deviceSetKey, ttlSeconds, TimeUnit.SECONDS);

        HashOperations<String, Object, Object> hash = redisTemplate.opsForHash();
        hash.put(deviceInfoKey, FIELD_TOKEN, loginToken);
        hash.put(deviceInfoKey, FIELD_IP, clientIp == null ? "" : clientIp);
        hash.put(deviceInfoKey, FIELD_USER_AGENT, userAgent == null ? "" : userAgent);
        redisTemplate.expire(deviceInfoKey, ttlSeconds, TimeUnit.SECONDS);
    }

    public String getLoginToken(String accountKey) {
        return getLoginToken(accountKey, "default");
    }

    public String getLoginToken(String accountKey, String deviceId) {
        if (!StringUtils.hasText(deviceId)) {
            return null;
        }
        return redisTemplate.opsForValue().get(tokenKey(accountKey, deviceId));
    }

    public boolean isDeviceActive(String accountKey, String deviceId) {
        return getLoginToken(accountKey, deviceId) != null;
    }

    public boolean removeIfMatch(String accountKey, String loginToken) {
        return removeIfMatch(accountKey, "default", loginToken);
    }

    public boolean removeIfMatch(String accountKey, String deviceId, String loginToken) {
        if (loginToken == null) {
            return false;
        }
        String key = tokenKey(accountKey, deviceId);
        String current = redisTemplate.opsForValue().get(key);
        if (loginToken.equals(current)) {
            remove(accountKey, deviceId);
            return true;
        }
        return false;
    }

    public void remove(String accountKey) {
        removeAll(accountKey);
    }

    public void remove(String accountKey, String deviceId) {
        redisTemplate.delete(tokenKey(accountKey, deviceId));
        redisTemplate.delete(deviceInfoKey(accountKey, deviceId));
        redisTemplate.opsForSet().remove(deviceSetKey(accountKey), deviceId);
    }

    public void removeAll(String accountKey) {
        java.util.Set<String> deviceIds = redisTemplate.opsForSet().members(deviceSetKey(accountKey));
        if (deviceIds != null) {
            for (String deviceId : deviceIds) {
                redisTemplate.delete(tokenKey(accountKey, deviceId));
                redisTemplate.delete(deviceInfoKey(accountKey, deviceId));
            }
        }
        redisTemplate.delete(deviceSetKey(accountKey));
    }

    public void removeOthers(String accountKey, String currentDeviceId) {
        java.util.Set<String> deviceIds = redisTemplate.opsForSet().members(deviceSetKey(accountKey));
        if (deviceIds == null) {
            return;
        }
        for (String deviceId : deviceIds) {
            if (!deviceId.equals(currentDeviceId)) {
                remove(accountKey, deviceId);
            }
        }
    }

    public List<AuthDeviceView> listDevices(String accountKey, String currentDeviceId) {
        java.util.Set<String> deviceIds = redisTemplate.opsForSet().members(deviceSetKey(accountKey));
        List<AuthDeviceView> devices = new ArrayList<AuthDeviceView>();
        if (deviceIds == null) {
            return devices;
        }
        for (String deviceId : deviceIds) {
            Map<Object, Object> values = redisTemplate.opsForHash().entries(deviceInfoKey(accountKey, deviceId));
            if (values.isEmpty() && getLoginToken(accountKey, deviceId) == null) {
                continue;
            }
            devices.add(new AuthDeviceView(
                deviceId,
                stringValue(values.get(FIELD_IP)),
                stringValue(values.get(FIELD_USER_AGENT)),
                deviceId.equals(currentDeviceId)
            ));
        }
        return devices;
    }

    private static String stringValue(Object value) {
        return value == null ? "" : String.valueOf(value);
    }

    private static String tokenKey(String accountKey, String deviceId) {
        return TOKEN_KEY_PREFIX + accountKey + ":" + deviceId;
    }

    private static String deviceSetKey(String accountKey) {
        return DEVICE_SET_PREFIX + accountKey;
    }

    private static String deviceInfoKey(String accountKey, String deviceId) {
        return DEVICE_INFO_PREFIX + accountKey + ":" + deviceId;
    }
}
