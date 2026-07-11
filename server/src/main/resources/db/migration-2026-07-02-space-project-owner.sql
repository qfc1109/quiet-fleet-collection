SET NAMES utf8mb4;

USE qfc_site;

SET @add_owner_user_id := (
  SELECT IF(
    COUNT(*) = 0,
    'ALTER TABLE project ADD COLUMN owner_user_id BIGINT NULL AFTER id',
    'SELECT 1'
  )
  FROM INFORMATION_SCHEMA.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'project'
    AND COLUMN_NAME = 'owner_user_id'
);
PREPARE add_owner_user_id_stmt FROM @add_owner_user_id;
EXECUTE add_owner_user_id_stmt;
DEALLOCATE PREPARE add_owner_user_id_stmt;

SET @add_owner_index := (
  SELECT IF(
    COUNT(*) = 0,
    'ALTER TABLE project ADD KEY idx_project_owner_user_id (owner_user_id)',
    'SELECT 1'
  )
  FROM INFORMATION_SCHEMA.STATISTICS
  WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'project'
    AND INDEX_NAME = 'idx_project_owner_user_id'
);
PREPARE add_owner_index_stmt FROM @add_owner_index;
EXECUTE add_owner_index_stmt;
DEALLOCATE PREPARE add_owner_index_stmt;

UPDATE project
SET owner_user_id = (SELECT id FROM site_user WHERE username = 'admin' LIMIT 1)
WHERE owner_user_id IS NULL;
