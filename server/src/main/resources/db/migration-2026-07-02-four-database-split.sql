SET NAMES utf8mb4;

-- Run this script while connected to the old single database, usually quiet_fleet_collection.

CREATE DATABASE IF NOT EXISTS qfc_site DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE DATABASE IF NOT EXISTS qfc_site_log DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE DATABASE IF NOT EXISTS qfc_admin DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE DATABASE IF NOT EXISTS qfc_admin_log DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS qfc_site.site_user (
  id BIGINT NOT NULL AUTO_INCREMENT,
  username VARCHAR(64) NOT NULL,
  password_hash VARCHAR(100) NOT NULL,
  display_name VARCHAR(100) NOT NULL,
  bio VARCHAR(500) NOT NULL DEFAULT '',
  avatar_url VARCHAR(500) NOT NULL DEFAULT '',
  status VARCHAR(20) NOT NULL DEFAULT 'ENABLED',
  created_at DATETIME NOT NULL,
  updated_at DATETIME NOT NULL,
  PRIMARY KEY (id),
  UNIQUE KEY uk_site_user_username (username)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS qfc_site.project (
  id BIGINT NOT NULL AUTO_INCREMENT,
  owner_user_id BIGINT NULL,
  name VARCHAR(120) NOT NULL,
  slug VARCHAR(120) NOT NULL,
  active_slug VARCHAR(120) GENERATED ALWAYS AS (IF(deleted_at IS NULL, slug, NULL)) STORED,
  description VARCHAR(1000) NOT NULL DEFAULT '',
  cover_url VARCHAR(500) NOT NULL DEFAULT '',
  visibility VARCHAR(20) NOT NULL,
  sort_order INT NOT NULL DEFAULT 0,
  created_at DATETIME NOT NULL,
  updated_at DATETIME NOT NULL,
  deleted_at DATETIME NULL,
  deleted_by_user_id BIGINT NULL,
  PRIMARY KEY (id),
  KEY idx_project_owner_user_id (owner_user_id),
  KEY idx_project_slug (slug),
  KEY idx_project_deleted_at (deleted_at),
  UNIQUE KEY uk_project_active_slug (active_slug)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS qfc_site.project_file (
  id BIGINT NOT NULL AUTO_INCREMENT,
  project_id BIGINT NOT NULL,
  original_name VARCHAR(255) NOT NULL,
  stored_name VARCHAR(255) NOT NULL,
  file_ext VARCHAR(32) NOT NULL DEFAULT '',
  mime_type VARCHAR(120) NOT NULL DEFAULT '',
  file_size BIGINT NOT NULL,
  storage_path VARCHAR(500) NOT NULL,
  relative_path VARCHAR(500) NOT NULL DEFAULT '',
  preview_type VARCHAR(32) NOT NULL,
  created_at DATETIME NOT NULL,
  updated_at DATETIME NOT NULL,
  PRIMARY KEY (id),
  KEY idx_project_file_project_id (project_id),
  KEY idx_project_file_relative_path (project_id, relative_path)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS qfc_admin.admin_user (
  id BIGINT NOT NULL AUTO_INCREMENT,
  username VARCHAR(64) NOT NULL,
  password_hash VARCHAR(100) NOT NULL,
  display_name VARCHAR(100) NOT NULL,
  bio VARCHAR(500) NOT NULL DEFAULT '',
  avatar_url VARCHAR(500) NOT NULL DEFAULT '',
  status VARCHAR(20) NOT NULL DEFAULT 'ENABLED',
  created_at DATETIME NOT NULL,
  updated_at DATETIME NOT NULL,
  PRIMARY KEY (id),
  UNIQUE KEY uk_admin_user_username (username)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS qfc_admin.admin_role (
  id BIGINT NOT NULL AUTO_INCREMENT,
  code VARCHAR(64) NOT NULL,
  name VARCHAR(100) NOT NULL,
  description VARCHAR(500) NOT NULL DEFAULT '',
  built_in TINYINT(1) NOT NULL DEFAULT 0,
  created_at DATETIME NOT NULL,
  updated_at DATETIME NOT NULL,
  PRIMARY KEY (id),
  UNIQUE KEY uk_admin_role_code (code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS qfc_admin.permission (
  id BIGINT NOT NULL AUTO_INCREMENT,
  code VARCHAR(64) NOT NULL,
  name VARCHAR(100) NOT NULL,
  module VARCHAR(64) NOT NULL,
  description VARCHAR(500) NOT NULL DEFAULT '',
  created_at DATETIME NOT NULL,
  updated_at DATETIME NOT NULL,
  PRIMARY KEY (id),
  UNIQUE KEY uk_permission_code (code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS qfc_admin.admin_user_role (
  id BIGINT NOT NULL AUTO_INCREMENT,
  user_id BIGINT NOT NULL,
  role_id BIGINT NOT NULL,
  PRIMARY KEY (id),
  UNIQUE KEY uk_admin_user_role_user_role (user_id, role_id),
  KEY idx_admin_user_role_user_id (user_id),
  KEY idx_admin_user_role_role_id (role_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS qfc_admin.role_permission (
  id BIGINT NOT NULL AUTO_INCREMENT,
  role_id BIGINT NOT NULL,
  permission_id BIGINT NOT NULL,
  PRIMARY KEY (id),
  UNIQUE KEY uk_role_permission_role_permission (role_id, permission_id),
  KEY idx_role_permission_role_id (role_id),
  KEY idx_role_permission_permission_id (permission_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS qfc_site_log.site_login_log (
  id BIGINT NOT NULL AUTO_INCREMENT,
  user_id BIGINT NULL,
  username VARCHAR(64) NOT NULL DEFAULT '',
  result VARCHAR(20) NOT NULL,
  ip_address VARCHAR(64) NOT NULL DEFAULT '',
  user_agent VARCHAR(500) NOT NULL DEFAULT '',
  message VARCHAR(500) NOT NULL DEFAULT '',
  created_at DATETIME NOT NULL,
  PRIMARY KEY (id),
  KEY idx_site_login_log_user_id (user_id),
  KEY idx_site_login_log_created_at (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS qfc_site_log.site_operation_log (
  id BIGINT NOT NULL AUTO_INCREMENT,
  user_id BIGINT NULL,
  username VARCHAR(64) NOT NULL DEFAULT '',
  action VARCHAR(64) NOT NULL,
  target_type VARCHAR(64) NOT NULL DEFAULT '',
  target_id VARCHAR(64) NOT NULL DEFAULT '',
  detail TEXT NULL,
  ip_address VARCHAR(64) NOT NULL DEFAULT '',
  created_at DATETIME NOT NULL,
  PRIMARY KEY (id),
  KEY idx_site_operation_log_user_id (user_id),
  KEY idx_site_operation_log_action (action),
  KEY idx_site_operation_log_created_at (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS qfc_admin_log.admin_login_log (
  id BIGINT NOT NULL AUTO_INCREMENT,
  admin_user_id BIGINT NULL,
  username VARCHAR(64) NOT NULL DEFAULT '',
  result VARCHAR(20) NOT NULL,
  ip_address VARCHAR(64) NOT NULL DEFAULT '',
  user_agent VARCHAR(500) NOT NULL DEFAULT '',
  message VARCHAR(500) NOT NULL DEFAULT '',
  created_at DATETIME NOT NULL,
  PRIMARY KEY (id),
  KEY idx_admin_login_log_user_id (admin_user_id),
  KEY idx_admin_login_log_created_at (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS qfc_admin_log.admin_operation_log (
  id BIGINT NOT NULL AUTO_INCREMENT,
  admin_user_id BIGINT NULL,
  username VARCHAR(64) NOT NULL DEFAULT '',
  action VARCHAR(64) NOT NULL,
  target_type VARCHAR(64) NOT NULL DEFAULT '',
  target_id VARCHAR(64) NOT NULL DEFAULT '',
  detail TEXT NULL,
  ip_address VARCHAR(64) NOT NULL DEFAULT '',
  created_at DATETIME NOT NULL,
  PRIMARY KEY (id),
  KEY idx_admin_operation_log_user_id (admin_user_id),
  KEY idx_admin_operation_log_action (action),
  KEY idx_admin_operation_log_created_at (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS qfc_admin_log.admin_console_log (
  id BIGINT NOT NULL AUTO_INCREMENT,
  level VARCHAR(20) NOT NULL,
  logger VARCHAR(200) NOT NULL DEFAULT '',
  message TEXT NOT NULL,
  trace_id VARCHAR(100) NOT NULL DEFAULT '',
  created_at DATETIME NOT NULL,
  PRIMARY KEY (id),
  KEY idx_admin_console_log_level (level),
  KEY idx_admin_console_log_created_at (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

INSERT INTO qfc_site.site_user (id, username, password_hash, display_name, bio, avatar_url, status, created_at, updated_at)
SELECT id, username, password_hash, display_name, bio, avatar_url, status, created_at, updated_at
FROM user_account
WHERE account_type = 'SITE_USER'
ON DUPLICATE KEY UPDATE
  password_hash = VALUES(password_hash),
  display_name = VALUES(display_name),
  bio = VALUES(bio),
  avatar_url = VALUES(avatar_url),
  status = VALUES(status),
  updated_at = VALUES(updated_at);

INSERT INTO qfc_site.site_user (
  username,
  password_hash,
  display_name,
  bio,
  avatar_url,
  status,
  created_at,
  updated_at
) VALUES (
  'admin',
  '$2a$10$gZpEOHdFL.moqMkEGGAV.u6T3OCE4uV3OQRMcmEG76SHJBlvcoFMy',
  '轻帆管理员',
  '轻帆集第一版网站账号',
  '',
  'ENABLED',
  NOW(),
  NOW()
) ON DUPLICATE KEY UPDATE
  password_hash = VALUES(password_hash),
  display_name = VALUES(display_name),
  bio = VALUES(bio),
  avatar_url = VALUES(avatar_url),
  status = VALUES(status),
  updated_at = VALUES(updated_at);

INSERT INTO qfc_admin.admin_user (id, username, password_hash, display_name, bio, avatar_url, status, created_at, updated_at)
SELECT id, username, password_hash, display_name, bio, avatar_url, status, created_at, updated_at
FROM user_account
WHERE account_type = 'ADMIN'
ON DUPLICATE KEY UPDATE
  password_hash = VALUES(password_hash),
  display_name = VALUES(display_name),
  bio = VALUES(bio),
  avatar_url = VALUES(avatar_url),
  status = VALUES(status),
  updated_at = VALUES(updated_at);

INSERT INTO qfc_site.project (id, owner_user_id, name, slug, description, cover_url, visibility, sort_order, created_at, updated_at)
SELECT id, (SELECT id FROM qfc_site.site_user WHERE username = 'admin' LIMIT 1), name, slug, description, cover_url, visibility, sort_order, created_at, updated_at
FROM project
ON DUPLICATE KEY UPDATE
  owner_user_id = VALUES(owner_user_id),
  name = VALUES(name),
  description = VALUES(description),
  cover_url = VALUES(cover_url),
  visibility = VALUES(visibility),
  sort_order = VALUES(sort_order),
  updated_at = VALUES(updated_at);

INSERT INTO qfc_site.project_file (id, project_id, original_name, stored_name, file_ext, mime_type, file_size, storage_path, relative_path, preview_type, created_at, updated_at)
SELECT id, project_id, original_name, stored_name, file_ext, mime_type, file_size, storage_path, relative_path, preview_type, created_at, updated_at
FROM project_file
ON DUPLICATE KEY UPDATE
  original_name = VALUES(original_name),
  stored_name = VALUES(stored_name),
  file_ext = VALUES(file_ext),
  mime_type = VALUES(mime_type),
  file_size = VALUES(file_size),
  storage_path = VALUES(storage_path),
  relative_path = VALUES(relative_path),
  preview_type = VALUES(preview_type),
  updated_at = VALUES(updated_at);

INSERT INTO qfc_admin.admin_role (id, code, name, description, built_in, created_at, updated_at)
SELECT id, code, name, description, built_in, created_at, updated_at
FROM admin_role
ON DUPLICATE KEY UPDATE
  code = VALUES(code),
  name = VALUES(name),
  description = VALUES(description),
  built_in = VALUES(built_in),
  updated_at = VALUES(updated_at);

INSERT INTO qfc_admin.permission (id, code, name, module, description, created_at, updated_at)
SELECT id, code, name, module, description, created_at, updated_at
FROM permission
ON DUPLICATE KEY UPDATE
  code = VALUES(code),
  name = VALUES(name),
  module = VALUES(module),
  description = VALUES(description),
  updated_at = VALUES(updated_at);

INSERT INTO qfc_admin.admin_user_role (id, user_id, role_id)
SELECT id, user_id, role_id
FROM user_role
ON DUPLICATE KEY UPDATE role_id = VALUES(role_id);

INSERT INTO qfc_admin.role_permission (id, role_id, permission_id)
SELECT id, role_id, permission_id
FROM role_permission
ON DUPLICATE KEY UPDATE permission_id = VALUES(permission_id);

INSERT INTO qfc_admin.admin_user (
  username,
  password_hash,
  display_name,
  bio,
  avatar_url,
  status,
  created_at,
  updated_at
) VALUES (
  'admin',
  '$2a$10$gZpEOHdFL.moqMkEGGAV.u6T3OCE4uV3OQRMcmEG76SHJBlvcoFMy',
  '轻帆管理员',
  '轻帆集第一版管理员账号',
  '',
  'ENABLED',
  NOW(),
  NOW()
) ON DUPLICATE KEY UPDATE
  password_hash = VALUES(password_hash),
  display_name = VALUES(display_name),
  bio = VALUES(bio),
  avatar_url = VALUES(avatar_url),
  status = VALUES(status),
  updated_at = VALUES(updated_at);
