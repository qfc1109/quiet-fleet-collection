package com.qfc.config;

import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;
import org.springframework.util.FileCopyUtils;

import static org.junit.jupiter.api.Assertions.assertTrue;

class SeedDataTest {

    @Test
    void dataSqlSeedsMatchingAdminAccountInSiteAndAdminDatabases() throws Exception {
        String sql = new String(
            FileCopyUtils.copyToByteArray(new ClassPathResource("db/data.sql").getInputStream()),
            StandardCharsets.UTF_8
        );

        assertTrue(sql.contains("USE qfc_site;"));
        assertTrue(sql.contains("INSERT INTO site_user"));
        assertTrue(sql.contains("USE qfc_admin;"));
        assertTrue(sql.contains("INSERT INTO admin_user"));
        assertTrue(sql.contains("'admin'"));
        assertTrue(sql.contains("'$2a$10$gZpEOHdFL.moqMkEGGAV.u6T3OCE4uV3OQRMcmEG76SHJBlvcoFMy'"));
        assertTrue(sql.contains("'轻帆管理员'"));
        assertTrue(sql.contains("'轻帆集第一版网站账号'"));
        assertTrue(sql.contains("'轻帆集第一版管理员账号'"));
    }
}
