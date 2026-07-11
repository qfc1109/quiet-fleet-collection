package com.qfc.admin;

import com.qfc.auth.LoginSessionService;
import com.qfc.user.SiteUser;
import com.qfc.user.SiteUserMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserManagementServiceTest {

    @Mock
    private SiteUserMapper siteUserMapper;

    @Mock
    private AdminUserMapper adminUserMapper;

    @Mock
    private AdminRoleMapper adminRoleMapper;

    @Mock
    private UserRoleMapper userRoleMapper;

    @Mock
    private LoginSessionService loginSessionService;

    private UserManagementService userManagementService;
    private PasswordEncoder passwordEncoder;

    @BeforeEach
    void setUp() {
        passwordEncoder = new BCryptPasswordEncoder();
        userManagementService = new UserManagementService(
            siteUserMapper,
            adminUserMapper,
            adminRoleMapper,
            userRoleMapper,
            passwordEncoder,
            loginSessionService
        );
    }

    @Test
    void createSiteUserStoresEnabledSiteUserWithEncodedPassword() {
        CreateUserRequest request = new CreateUserRequest();
        request.setUsername("designer01");
        request.setPassword("123456");
        request.setDisplayName("策划同事");
        request.setBio("查看项目资料");
        request.setAvatarUrl("");
        when(siteUserMapper.insert(any(SiteUser.class))).thenAnswer(invocation -> {
            SiteUser account = invocation.getArgument(0);
            account.setId(21L);
            return 1;
        });

        UserAccountView view = userManagementService.createSiteUser(request);

        ArgumentCaptor<SiteUser> captor = ArgumentCaptor.forClass(SiteUser.class);
        verify(siteUserMapper).insert(captor.capture());
        SiteUser saved = captor.getValue();
        assertEquals("designer01", saved.getUsername());
        assertEquals("ENABLED", saved.getStatus());
        assertTrue(passwordEncoder.matches("123456", saved.getPasswordHash()));
        assertEquals(21L, view.getId());
        assertEquals("SITE_USER", view.getAccountType());
    }

    @Test
    void createSiteUserAllowsSameUsernameAsAdminUser() {
        CreateUserRequest request = new CreateUserRequest();
        request.setUsername("admin");
        request.setPassword("123456");
        request.setDisplayName("网站管理员");
        request.setBio("");
        request.setAvatarUrl("");
        when(siteUserMapper.findByUsername("admin")).thenReturn(null);
        when(siteUserMapper.insert(any(SiteUser.class))).thenAnswer(invocation -> {
            SiteUser account = invocation.getArgument(0);
            account.setId(22L);
            return 1;
        });

        UserAccountView view = userManagementService.createSiteUser(request);

        assertEquals("admin", view.getUsername());
        assertEquals("SITE_USER", view.getAccountType());
        verify(adminUserMapper, never()).findByUsername("admin");
    }

    @Test
    void createAdminUserAssignsSelectedRoles() {
        CreateAdminUserRequest request = new CreateAdminUserRequest();
        request.setUsername("ops-admin");
        request.setPassword("123456");
        request.setDisplayName("运维管理员");
        request.setRoleCodes(Arrays.asList("NORMAL_ADMIN"));
        AdminRole role = new AdminRole();
        role.setId(3L);
        role.setCode("NORMAL_ADMIN");
        role.setName("普通管理员");
        when(adminRoleMapper.selectByCodes(Arrays.asList("NORMAL_ADMIN"))).thenReturn(Arrays.asList(role));
        when(adminUserMapper.insert(any(AdminUser.class))).thenAnswer(invocation -> {
            AdminUser account = invocation.getArgument(0);
            account.setId(31L);
            return 1;
        });

        UserAccountView view = userManagementService.createAdminUser(request);

        ArgumentCaptor<AdminUser> accountCaptor = ArgumentCaptor.forClass(AdminUser.class);
        verify(adminUserMapper).insert(accountCaptor.capture());
        assertEquals("ops-admin", accountCaptor.getValue().getUsername());
        verify(userRoleMapper).deleteByUserId(31L);
        ArgumentCaptor<UserRole> roleCaptor = ArgumentCaptor.forClass(UserRole.class);
        verify(userRoleMapper).insert(roleCaptor.capture());
        assertEquals(31L, roleCaptor.getValue().getUserId());
        assertEquals(3L, roleCaptor.getValue().getRoleId());
        assertEquals(Arrays.asList("NORMAL_ADMIN"), view.getRoleCodes());
        assertEquals("ADMIN", view.getAccountType());
    }

    @Test
    void resetSiteUserPasswordInvalidatesSiteLoginSession() {
        SiteUser account = new SiteUser();
        account.setId(5L);
        account.setUsername("designer01");
        account.setDisplayName("策划同事");
        account.setStatus(RbacUserSessionService.STATUS_ENABLED);
        when(siteUserMapper.selectById(5L)).thenReturn(account);
        ResetPasswordRequest request = new ResetPasswordRequest();
        request.setPassword("new-password");

        userManagementService.resetSiteUserPassword(5L, request);

        verify(siteUserMapper).updateById(account);
        verify(loginSessionService).invalidateAccount(RbacUserSessionService.ACCOUNT_TYPE_SITE_USER, 5L);
    }

    @Test
    void resetAdminUserPasswordInvalidatesAdminLoginSession() {
        AdminUser account = new AdminUser();
        account.setId(1L);
        account.setUsername("admin");
        account.setDisplayName("后台管理员");
        account.setStatus(RbacUserSessionService.STATUS_ENABLED);
        when(adminUserMapper.selectById(1L)).thenReturn(account);
        when(adminRoleMapper.selectRoleCodesByUserId(1L)).thenReturn(Arrays.asList("SUPER_ADMIN"));
        ResetPasswordRequest request = new ResetPasswordRequest();
        request.setPassword("new-password");

        userManagementService.resetAdminUserPassword(1L, request);

        verify(adminUserMapper).updateById(account);
        verify(loginSessionService).invalidateAccount(RbacUserSessionService.ACCOUNT_TYPE_ADMIN, 1L);
    }
}
