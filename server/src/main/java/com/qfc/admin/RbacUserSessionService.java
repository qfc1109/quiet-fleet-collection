package com.qfc.admin;

import com.qfc.auth.LoginUser;
import com.qfc.user.SiteUser;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class RbacUserSessionService {

    public static final String ACCOUNT_TYPE_ADMIN = "ADMIN";
    public static final String ACCOUNT_TYPE_SITE_USER = "SITE_USER";
    public static final String STATUS_ENABLED = "ENABLED";
    public static final String STATUS_DISABLED = "DISABLED";

    private final AdminUserMapper adminUserMapper;
    private final AdminRoleMapper adminRoleMapper;
    private final PermissionMapper permissionMapper;

    public RbacUserSessionService(
        AdminUserMapper adminUserMapper,
        AdminRoleMapper adminRoleMapper,
        PermissionMapper permissionMapper
    ) {
        this.adminUserMapper = adminUserMapper;
        this.adminRoleMapper = adminRoleMapper;
        this.permissionMapper = permissionMapper;
    }

    public LoginUser toLoginUser(SiteUser account) {
        return new LoginUser(
            account.getId(),
            account.getUsername(),
            account.getDisplayName(),
            account.getBio(),
            account.getAvatarUrl(),
            ACCOUNT_TYPE_SITE_USER,
            account.getStatus(),
            new ArrayList<String>(),
            new ArrayList<String>()
        );
    }

    public LoginUser toLoginUser(AdminUser account) {
        List<String> roleCodes = adminRoleMapper.selectRoleCodesByUserId(account.getId());
        List<String> permissionCodes = permissionMapper.selectPermissionCodesByUserId(account.getId());
        return new LoginUser(
            account.getId(),
            account.getUsername(),
            account.getDisplayName(),
            account.getBio(),
            account.getAvatarUrl(),
            ACCOUNT_TYPE_ADMIN,
            account.getStatus(),
            roleCodes,
            permissionCodes
        );
    }

    public boolean hasPermission(Long userId, String permissionCode) {
        AdminUser account = adminUserMapper.selectById(userId);
        if (account == null || !STATUS_ENABLED.equals(account.getStatus())) {
            return false;
        }
        return permissionMapper.selectPermissionCodesByUserId(userId).contains(permissionCode);
    }
}
