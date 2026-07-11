package com.qfc.project;

import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;
import org.springframework.util.FileCopyUtils;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ProjectSchemaTest {

    @Test
    void schemaUsesActiveSlugUniqueKeyForSoftDeletedProjects() throws Exception {
        String sql = readSql("db/schema.sql");

        assertActiveSlugUniqueKey(sql);
        assertFalse(sql.contains("UNIQUE KEY uk_project_slug (slug)"));
    }

    @Test
    void migrationReplacesProjectSlugUniqueKeyWithActiveSlugUniqueKey() throws Exception {
        String sql = readSql("db/migration-2026-07-11-project-active-slug.sql");

        assertTrue(sql.contains("DROP INDEX uk_project_slug"));
        assertActiveSlugUniqueKey(sql);
        assertFalse(sql.contains("ADD UNIQUE KEY uk_project_slug (slug)"));
    }

    private void assertActiveSlugUniqueKey(String sql) {
        assertTrue(sql.contains("active_slug VARCHAR(120) GENERATED ALWAYS AS (IF(deleted_at IS NULL, slug, NULL)) STORED"));
        assertTrue(sql.contains("UNIQUE KEY uk_project_active_slug (active_slug)"));
    }

    private String readSql(String path) throws Exception {
        return new String(
            FileCopyUtils.copyToByteArray(new ClassPathResource(path).getInputStream()),
            StandardCharsets.UTF_8
        );
    }
}
