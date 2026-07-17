SET NAMES utf8mb4;

USE qfc_site;

UPDATE site_user
SET display_name = '轻帆管理员',
    bio = '轻帆集第一版网站账号',
    updated_at = NOW()
WHERE username = 'admin';

USE qfc_admin;

UPDATE admin_user
SET display_name = '轻帆管理员',
    bio = '轻帆集第一版管理员账号',
    updated_at = NOW()
WHERE username = 'admin';

UPDATE permission
SET name = CASE code
      WHEN 'USER_VIEW' THEN '查看网站用户'
      WHEN 'USER_MANAGE' THEN '管理网站用户'
      WHEN 'ROLE_VIEW' THEN '查看角色权限'
      WHEN 'ROLE_MANAGE' THEN '管理角色权限'
      WHEN 'PROJECT_MANAGE' THEN '管理项目'
      WHEN 'FILE_MANAGE' THEN '管理文件'
      WHEN 'ISSUE_MANAGE' THEN '管理反馈'
      ELSE name
    END,
    module = CASE code
      WHEN 'USER_VIEW' THEN '用户管理'
      WHEN 'USER_MANAGE' THEN '用户管理'
      WHEN 'ROLE_VIEW' THEN '后台管理'
      WHEN 'ROLE_MANAGE' THEN '后台管理'
      WHEN 'PROJECT_MANAGE' THEN '内容管理'
      WHEN 'FILE_MANAGE' THEN '内容管理'
      WHEN 'ISSUE_MANAGE' THEN '反馈管理'
      ELSE module
    END,
    description = CASE code
      WHEN 'USER_VIEW' THEN '查看网站使用者账号列表'
      WHEN 'USER_MANAGE' THEN '创建、编辑、停用网站使用者账号'
      WHEN 'ROLE_VIEW' THEN '查看后台角色和权限配置'
      WHEN 'ROLE_MANAGE' THEN '调整权限、管理后台管理员账号'
      WHEN 'PROJECT_MANAGE' THEN '创建和编辑公开项目展示'
      WHEN 'FILE_MANAGE' THEN '上传、删除和维护项目文件'
      WHEN 'ISSUE_MANAGE' THEN '查看和处理后续 issue 反馈'
      ELSE description
    END,
    updated_at = NOW()
WHERE code IN ('USER_VIEW', 'USER_MANAGE', 'ROLE_VIEW', 'ROLE_MANAGE', 'PROJECT_MANAGE', 'FILE_MANAGE', 'ISSUE_MANAGE');

UPDATE admin_role
SET name = CASE code
      WHEN 'SUPER_ADMIN' THEN '超级管理员'
      WHEN 'NORMAL_ADMIN' THEN '普通管理员'
      ELSE name
    END,
    description = CASE code
      WHEN 'SUPER_ADMIN' THEN '拥有后台全部权限，默认绑定初始 admin 账号'
      WHEN 'NORMAL_ADMIN' THEN '维护网站用户、项目、文件和反馈，不管理后台角色权限'
      ELSE description
    END,
    updated_at = NOW()
WHERE code IN ('SUPER_ADMIN', 'NORMAL_ADMIN');
