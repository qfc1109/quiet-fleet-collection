package com.qfc.admin;

import com.qfc.auth.LoginUser;
import com.qfc.user.SiteUser;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RbacUserSessionServiceTest {

    @Mock
    private AdminUserMapper adminUserMapper;

    @Mock
    private AdminRoleMapper adminRoleMapper;

    @Mock
    private PermissionMapper permissionMapper;

    private RbacUserSessionService service;

    @BeforeEach
    void setUp() {
        service = new RbacUserSessionService(adminUserMapper, adminRoleMapper, permissionMapper);
    }

    @Test
    void toLoginUserIncludesRoleAndPermissionCodes() {
        AdminUser account = new AdminUser();
        account.setId(1L);
        account.setUsername("admin");
        account.setDisplayName("轻帆管理员");
        account.setStatus("ENABLED");
        when(adminRoleMapper.selectRoleCodesByUserId(1L)).thenReturn(Arrays.asList("SUPER_ADMIN"));
        when(permissionMapper.selectPermissionCodesByUserId(1L)).thenReturn(Arrays.asList("ROLE_MANAGE", "USER_MANAGE"));

        LoginUser loginUser = service.toLoginUser(account);

        assertEquals("ADMIN", loginUser.getAccountType());
        assertEquals(Arrays.asList("SUPER_ADMIN"), loginUser.getRoleCodes());
        assertEquals(Arrays.asList("ROLE_MANAGE", "USER_MANAGE"), loginUser.getPermissionCodes());
    }

    @Test
    void toLoginUserBuildsSiteUserWithoutAdminPermissions() {
        SiteUser account = new SiteUser();
        account.setId(3L);
        account.setUsername("player01");
        account.setDisplayName("网站用户");
        account.setStatus("ENABLED");

        LoginUser loginUser = service.toLoginUser(account);

        assertEquals("SITE_USER", loginUser.getAccountType());
        assertTrue(loginUser.getRoleCodes().isEmpty());
        assertTrue(loginUser.getPermissionCodes().isEmpty());
    }

    @Test
    void hasPermissionOnlyAllowsEnabledAdminWithPermission() {
        AdminUser account = new AdminUser();
        account.setId(2L);
        account.setUsername("ops");
        account.setDisplayName("运维");
        account.setStatus("ENABLED");
        when(adminUserMapper.selectById(2L)).thenReturn(account);
        when(permissionMapper.selectPermissionCodesByUserId(2L)).thenReturn(Arrays.asList("PROJECT_MANAGE"));

        assertTrue(service.hasPermission(2L, "PROJECT_MANAGE"));
        assertFalse(service.hasPermission(2L, "ROLE_MANAGE"));
    }
}
