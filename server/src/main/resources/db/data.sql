SET NAMES utf8mb4;

USE qfc_site;

INSERT INTO site_user (
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

USE qfc_admin;

INSERT INTO admin_user (
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

INSERT INTO permission (code, name, module, description, created_at, updated_at) VALUES
('USER_VIEW', '查看网站用户', '用户管理', '查看网站使用者账号列表', NOW(), NOW()),
('USER_MANAGE', '管理网站用户', '用户管理', '创建、编辑、停用网站使用者账号', NOW(), NOW()),
('ROLE_VIEW', '查看角色权限', '后台管理', '查看后台角色和权限配置', NOW(), NOW()),
('ROLE_MANAGE', '管理角色权限', '后台管理', '调整权限、管理后台管理员账号', NOW(), NOW()),
('PROJECT_MANAGE', '管理项目', '内容管理', '创建和编辑公开项目展示', NOW(), NOW()),
('FILE_MANAGE', '管理文件', '内容管理', '上传、删除和维护项目文件', NOW(), NOW()),
('ISSUE_MANAGE', '管理反馈', '反馈管理', '查看和处理后续 issue 反馈', NOW(), NOW())
ON DUPLICATE KEY UPDATE
  name = VALUES(name),
  module = VALUES(module),
  description = VALUES(description),
  updated_at = VALUES(updated_at);

INSERT INTO admin_role (code, name, description, built_in, created_at, updated_at) VALUES
('SUPER_ADMIN', '超级管理员', '拥有后台全部权限，默认绑定初始 admin 账号', 1, NOW(), NOW()),
('NORMAL_ADMIN', '普通管理员', '维护网站用户、项目、文件和反馈，不管理后台角色权限', 1, NOW(), NOW())
ON DUPLICATE KEY UPDATE
  name = VALUES(name),
  description = VALUES(description),
  built_in = VALUES(built_in),
  updated_at = VALUES(updated_at);

INSERT INTO role_permission (role_id, permission_id)
SELECT r.id, p.id
FROM admin_role r
JOIN permission p
WHERE r.code = 'SUPER_ADMIN'
ON DUPLICATE KEY UPDATE permission_id = VALUES(permission_id);

INSERT INTO role_permission (role_id, permission_id)
SELECT r.id, p.id
FROM admin_role r
JOIN permission p ON p.code IN ('USER_VIEW', 'USER_MANAGE', 'PROJECT_MANAGE', 'FILE_MANAGE', 'ISSUE_MANAGE')
WHERE r.code = 'NORMAL_ADMIN'
ON DUPLICATE KEY UPDATE permission_id = VALUES(permission_id);

INSERT INTO admin_user_role (user_id, role_id)
SELECT u.id, r.id
FROM admin_user u
JOIN admin_role r ON r.code = 'SUPER_ADMIN'
WHERE u.username = 'admin'
ON DUPLICATE KEY UPDATE role_id = VALUES(role_id);
