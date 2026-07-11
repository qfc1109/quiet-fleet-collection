SET NAMES utf8mb4;

USE qfc_site;

SET @drop_old_slug_unique := (
  SELECT IF(
    COUNT(*) > 0,
    'ALTER TABLE project DROP INDEX uk_project_slug',
    'SELECT 1'
  )
  FROM INFORMATION_SCHEMA.STATISTICS
  WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'project'
    AND INDEX_NAME = 'uk_project_slug'
);
PREPARE drop_old_slug_unique_stmt FROM @drop_old_slug_unique;
EXECUTE drop_old_slug_unique_stmt;
DEALLOCATE PREPARE drop_old_slug_unique_stmt;

SET @add_active_slug := (
  SELECT IF(
    COUNT(*) = 0,
    'ALTER TABLE project ADD COLUMN active_slug VARCHAR(120) GENERATED ALWAYS AS (IF(deleted_at IS NULL, slug, NULL)) STORED AFTER slug',
    'SELECT 1'
  )
  FROM INFORMATION_SCHEMA.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'project'
    AND COLUMN_NAME = 'active_slug'
);
PREPARE add_active_slug_stmt FROM @add_active_slug;
EXECUTE add_active_slug_stmt;
DEALLOCATE PREPARE add_active_slug_stmt;

SET @add_slug_index := (
  SELECT IF(
    COUNT(*) = 0,
    'ALTER TABLE project ADD KEY idx_project_slug (slug)',
    'SELECT 1'
  )
  FROM INFORMATION_SCHEMA.STATISTICS
  WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'project'
    AND INDEX_NAME = 'idx_project_slug'
);
PREPARE add_slug_index_stmt FROM @add_slug_index;
EXECUTE add_slug_index_stmt;
DEALLOCATE PREPARE add_slug_index_stmt;

SET @add_active_slug_unique := (
  SELECT IF(
    COUNT(*) = 0,
    'ALTER TABLE project ADD UNIQUE KEY uk_project_active_slug (active_slug)',
    'SELECT 1'
  )
  FROM INFORMATION_SCHEMA.STATISTICS
  WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'project'
    AND INDEX_NAME = 'uk_project_active_slug'
);
PREPARE add_active_slug_unique_stmt FROM @add_active_slug_unique;
EXECUTE add_active_slug_unique_stmt;
DEALLOCATE PREPARE add_active_slug_unique_stmt;
