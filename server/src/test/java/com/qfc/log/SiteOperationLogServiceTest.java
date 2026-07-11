package com.qfc.log;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDateTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.startsWith;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class SiteOperationLogServiceTest {

    @Mock
    private JdbcTemplate siteLogJdbcTemplate;

    private SiteOperationLogService service;

    @BeforeEach
    void setUp() {
        service = new SiteOperationLogService(siteLogJdbcTemplate, new ObjectMapper());
    }

    @Test
    void logProjectSoftDeleteWritesSiteOperationLog() {
        service.logProjectSoftDelete(5L, "player01", 31L, "个人排障记录", "personal-debug-notes", "127.0.0.1");

        verify(siteLogJdbcTemplate).update(
            startsWith("insert into site_operation_log"),
            eq(5L),
            eq("player01"),
            eq("PROJECT_SOFT_DELETE"),
            eq("PROJECT"),
            eq("31"),
            contains("\"slug\":\"personal-debug-notes\""),
            eq("127.0.0.1"),
            any(LocalDateTime.class)
        );
    }
}
