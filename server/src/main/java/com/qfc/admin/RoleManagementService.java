package com.qfc.admin;

import com.qfc.common.ApiException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class RoleManagementService {

    private static final String SUPER_ADMIN = "SUPER_ADMIN";

    private final AdminRoleMapper adminRoleMapper;
    private final PermissionMapper permissionMapper;
    private final RolePermissionMapper rolePermissionMapper;

    public RoleManagementService(
        AdminRoleMapper adminRoleMapper,
        PermissionMapper permissionMapper,
        RolePermissionMapper rolePermissionMapper
    ) {
        this.adminRoleMapper = adminRoleMapper;
        this.permissionMapper = permissionMapper;
        this.rolePermissionMapper = rolePermissionMapper;
    }

    public List<AdminRoleView> listRoles() {
        List<AdminRoleView> views = new ArrayList<AdminRoleView>();
        for (AdminRole role : adminRoleMapper.selectAllRoles()) {
            views.add(AdminRoleView.from(role, permissionMapper.selectPermissionCodesByRoleId(role.getId())));
        }
        return views;
    }

    public List<PermissionView> listPermissions() {
        List<PermissionView> views = new ArrayList<PermissionView>();
        for (Permission permission : permissionMapper.selectAllPermissions()) {
            views.add(PermissionView.from(permission));
        }
        return views;
    }

    @Transactional(transactionManager = "adminTransactionManager")
    public AdminRoleView createRole(CreateRoleRequest request) {
        throw new ApiException("ROLE_CREATION_DISABLED", "第一版只保留超级管理员和普通管理员两种角色", 400);
    }

    @Transactional(transactionManager = "adminTransactionManager")
    public AdminRoleView updateRole(Long roleId, UpdateRoleRequest request) {
        AdminRole role = adminRoleMapper.selectById(roleId);
        if (role == null) {
            throw new ApiException("ROLE_NOT_FOUND", "角色不存在", 404);
        }
        role.setName(request.getName());
        role.setDescription(defaultString(request.getDescription()));
        role.setUpdatedAt(LocalDateTime.now());
        adminRoleMapper.updateById(role);
        assignPermissions(role.getId(), request.getPermissionCodes(), SUPER_ADMIN.equals(role.getCode()));
        return AdminRoleView.from(role, permissionMapper.selectPermissionCodesByRoleId(role.getId()));
    }

    private void assignPermissions(Long roleId, List<String> permissionCodes, boolean protectAllPermissions) {
        List<String> normalizedCodes = normalizedCodes(permissionCodes);
        List<Permission> permissions = normalizedCodes.isEmpty()
            ? new ArrayList<Permission>()
            : permissionMapper.selectByCodes(normalizedCodes);
        if (permissions.size() != normalizedCodes.size()) {
            throw new ApiException("PERMISSION_NOT_FOUND", "存在无效权限", 400);
        }
        if (protectAllPermissions && permissions.size() != permissionMapper.selectAllPermissions().size()) {
            throw new ApiException("SUPER_ADMIN_PROTECTED", "不能清空超级管理员权限", 400);
        }
        rolePermissionMapper.deleteByRoleId(roleId);
        for (Permission permission : permissions) {
            RolePermission rolePermission = new RolePermission();
            rolePermission.setRoleId(roleId);
            rolePermission.setPermissionId(permission.getId());
            rolePermissionMapper.insert(rolePermission);
        }
    }

    private List<String> normalizedCodes(List<String> codes) {
        Set<String> values = new HashSet<String>();
        if (codes != null) {
            for (String code : codes) {
                if (StringUtils.hasText(code)) {
                    values.add(trimUpper(code));
                }
            }
        }
        return new ArrayList<String>(values);
    }

    private String trimUpper(String value) {
        return value == null ? "" : value.trim().toUpperCase();
    }

    private String defaultString(String value) {
        return value == null ? "" : value;
    }
}
