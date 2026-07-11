package com.qfc.log;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

@Service
public class SiteOperationLogService {

    private static final String ACTION_PROJECT_SOFT_DELETE = "PROJECT_SOFT_DELETE";
    private static final String TARGET_PROJECT = "PROJECT";

    private final JdbcTemplate siteLogJdbcTemplate;
    private final ObjectMapper objectMapper;

    public SiteOperationLogService(
        @Qualifier("siteLogJdbcTemplate") JdbcTemplate siteLogJdbcTemplate,
        ObjectMapper objectMapper
    ) {
        this.siteLogJdbcTemplate = siteLogJdbcTemplate;
        this.objectMapper = objectMapper;
    }

    public void logProjectSoftDelete(
        Long userId,
        String username,
        Long projectId,
        String projectName,
        String projectSlug,
        String ipAddress
    ) {
        Map<String, Object> detail = new LinkedHashMap<String, Object>();
        detail.put("projectId", projectId);
        detail.put("name", projectName);
        detail.put("slug", projectSlug);
        insertOperationLog(
            userId,
            username,
            ACTION_PROJECT_SOFT_DELETE,
            TARGET_PROJECT,
            String.valueOf(projectId),
            toJson(detail),
            ipAddress
        );
    }

    private void insertOperationLog(
        Long userId,
        String username,
        String action,
        String targetType,
        String targetId,
        String detail,
        String ipAddress
    ) {
        siteLogJdbcTemplate.update(
            "insert into site_operation_log (user_id, username, action, target_type, target_id, detail, ip_address, created_at) values (?, ?, ?, ?, ?, ?, ?, ?)",
            userId,
            username == null ? "" : username,
            action,
            targetType,
            targetId,
            detail,
            ipAddress == null ? "" : ipAddress,
            LocalDateTime.now()
        );
    }

    private String toJson(Map<String, Object> detail) {
        try {
            return objectMapper.writeValueAsString(detail);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Failed to serialize site operation log detail", exception);
        }
    }
}
