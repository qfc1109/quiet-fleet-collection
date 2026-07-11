package com.qfc.auth;

import com.qfc.admin.AdminUser;
import com.qfc.admin.AdminUserMapper;
import com.qfc.admin.RbacUserSessionService;
import com.qfc.common.ApiException;
import com.qfc.config.QfcStorageProperties;
import com.qfc.user.SiteUser;
import com.qfc.user.SiteUserMapper;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.mock.web.MockMultipartFile;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @TempDir
    private Path tempDir;

    @Mock
    private SiteUserMapper siteUserMapper;

    @Mock
    private AdminUserMapper adminUserMapper;

    private AuthService authService;
    private PasswordEncoder passwordEncoder;
    private RbacUserSessionService rbacUserSessionService;

    @BeforeEach
    void setUp() {
        passwordEncoder = new BCryptPasswordEncoder();
        rbacUserSessionService = mock(RbacUserSessionService.class);
        QfcStorageProperties properties = new QfcStorageProperties();
        properties.setRoot(tempDir.toString());
        authService = new AuthService(siteUserMapper, adminUserMapper, passwordEncoder, rbacUserSessionService, properties);
    }

    @Test
    void siteLoginUsesSiteUserWhenSameUsernameExistsInBothDatabases() {
        SiteUser siteAccount = new SiteUser();
        siteAccount.setId(1L);
        siteAccount.setUsername("admin");
        siteAccount.setDisplayName("网站管理员");
        siteAccount.setPasswordHash(passwordEncoder.encode("123456"));
        siteAccount.setStatus("ENABLED");
        AdminUser adminAccount = new AdminUser();
        adminAccount.setId(1L);
        adminAccount.setUsername("admin");
        adminAccount.setDisplayName("后台管理员");
        adminAccount.setPasswordHash(passwordEncoder.encode("123456"));
        adminAccount.setStatus("ENABLED");
        when(siteUserMapper.findByUsername("admin")).thenReturn(siteAccount);
        when(rbacUserSessionService.toLoginUser(siteAccount)).thenReturn(
            new LoginUser(
                1L,
                "admin",
                "网站管理员",
                "SITE_USER",
                "ENABLED",
                Arrays.asList(),
                Arrays.asList()
            )
        );

        LoginUser loginUser = authService.loginSite("admin", "123456");

        assertEquals("SITE_USER", loginUser.getAccountType());
        assertEquals("网站管理员", loginUser.getDisplayName());
        verify(adminUserMapper, never()).findByUsername("admin");
    }

    @Test
    void adminLoginUsesAdminUserWhenSameUsernameExistsInBothDatabases() {
        AdminUser account = new AdminUser();
        account.setId(1L);
        account.setUsername("admin");
        account.setDisplayName("轻帆管理员");
        account.setPasswordHash(passwordEncoder.encode("123456"));
        account.setStatus("ENABLED");
        when(adminUserMapper.findByUsername("admin")).thenReturn(account);
        when(rbacUserSessionService.toLoginUser(account)).thenReturn(
            new LoginUser(
                1L,
                "admin",
                "轻帆管理员",
                "ADMIN",
                "ENABLED",
                Arrays.asList("SUPER_ADMIN"),
                Arrays.asList("ROLE_MANAGE", "USER_MANAGE")
            )
        );

        LoginUser loginUser = authService.loginAdmin("admin", "123456");

        assertEquals(1L, loginUser.getId());
        assertEquals("admin", loginUser.getUsername());
        assertEquals("轻帆管理员", loginUser.getDisplayName());
        assertEquals("ADMIN", loginUser.getAccountType());
        assertEquals(Arrays.asList("SUPER_ADMIN"), loginUser.getRoleCodes());
        assertEquals(Arrays.asList("ROLE_MANAGE", "USER_MANAGE"), loginUser.getPermissionCodes());
    }

    @Test
    void loginRejectsWrongPassword() {
        AdminUser account = new AdminUser();
        account.setId(1L);
        account.setUsername("admin");
        account.setDisplayName("轻帆管理员");
        account.setPasswordHash(passwordEncoder.encode("123456"));
        account.setStatus("ENABLED");
        when(adminUserMapper.findByUsername("admin")).thenReturn(account);

        ApiException exception = assertThrows(
            ApiException.class,
            () -> authService.loginAdmin("admin", "wrong-password")
        );

        assertEquals("INVALID_CREDENTIALS", exception.getCode());
    }

    @Test
    void siteLoginRejectsDisabledAccount() {
        SiteUser account = new SiteUser();
        account.setId(2L);
        account.setUsername("disabled");
        account.setDisplayName("停用账号");
        account.setPasswordHash(passwordEncoder.encode("123456"));
        account.setStatus("DISABLED");
        when(siteUserMapper.findByUsername("disabled")).thenReturn(account);

        ApiException exception = assertThrows(
            ApiException.class,
            () -> authService.loginSite("disabled", "123456")
        );

        assertEquals("ACCOUNT_DISABLED", exception.getCode());
    }

    @Test
    void registerCreatesEnabledSiteUserAndReturnsLoginUser() {
        RegisterRequest request = new RegisterRequest();
        request.setUsername("player01");
        request.setPassword("123456");
        request.setDisplayName("项目访客");
        when(siteUserMapper.findByUsername("player01")).thenReturn(null);
        when(siteUserMapper.insert(any(SiteUser.class))).thenAnswer(invocation -> {
            SiteUser account = invocation.getArgument(0);
            account.setId(8L);
            return 1;
        });
        when(rbacUserSessionService.toLoginUser(any(SiteUser.class))).thenAnswer(invocation -> {
            SiteUser account = invocation.getArgument(0);
            return new LoginUser(
                account.getId(),
                account.getUsername(),
                account.getDisplayName(),
                "SITE_USER",
                account.getStatus(),
                Arrays.asList(),
                Arrays.asList()
            );
        });

        LoginUser loginUser = authService.register(request);

        assertEquals(8L, loginUser.getId());
        assertEquals("player01", loginUser.getUsername());
        assertEquals("项目访客", loginUser.getDisplayName());
        assertEquals("SITE_USER", loginUser.getAccountType());
        assertEquals("ENABLED", loginUser.getStatus());
        verify(siteUserMapper).insert(any(SiteUser.class));
    }

    @Test
    void registerAllowsSameUsernameAsAdminUserBecauseTablesAreSeparated() {
        RegisterRequest request = new RegisterRequest();
        request.setUsername("admin");
        request.setPassword("123456");
        request.setDisplayName("网站管理员");
        when(siteUserMapper.findByUsername("admin")).thenReturn(null);
        when(siteUserMapper.insert(any(SiteUser.class))).thenAnswer(invocation -> {
            SiteUser account = invocation.getArgument(0);
            account.setId(11L);
            return 1;
        });
        when(rbacUserSessionService.toLoginUser(any(SiteUser.class))).thenAnswer(invocation -> {
            SiteUser account = invocation.getArgument(0);
            return new LoginUser(
                account.getId(),
                account.getUsername(),
                account.getDisplayName(),
                "SITE_USER",
                account.getStatus(),
                Arrays.asList(),
                Arrays.asList()
            );
        });

        LoginUser loginUser = authService.register(request);

        assertEquals("admin", loginUser.getUsername());
        assertEquals("SITE_USER", loginUser.getAccountType());
        verify(adminUserMapper, never()).findByUsername("admin");
    }

    @Test
    void updateCurrentUserProfileUpdatesDisplayNameAndBio() {
        AdminUser account = new AdminUser();
        account.setId(1L);
        account.setUsername("admin");
        account.setDisplayName("旧昵称");
        account.setBio("旧简介");
        account.setAvatarUrl("/api/public/avatars/1/avatar.png");
        account.setStatus("ENABLED");
        when(adminUserMapper.selectById(1L)).thenReturn(account);
        when(rbacUserSessionService.toLoginUser(any(AdminUser.class))).thenAnswer(invocation -> {
            AdminUser updated = invocation.getArgument(0);
            return new LoginUser(
                updated.getId(),
                updated.getUsername(),
                updated.getDisplayName(),
                updated.getBio(),
                updated.getAvatarUrl(),
                "ADMIN",
                updated.getStatus(),
                Arrays.asList("SUPER_ADMIN"),
                Arrays.asList("PROJECT_MANAGE")
            );
        });
        UpdateCurrentUserRequest request = new UpdateCurrentUserRequest();
        request.setDisplayName("新昵称");
        request.setBio("新的账号简介");

        LoginUser loginUser = authService.updateCurrentUserProfile(1L, "ADMIN", request);

        ArgumentCaptor<AdminUser> captor = ArgumentCaptor.forClass(AdminUser.class);
        verify(adminUserMapper).updateById(captor.capture());
        assertEquals("新昵称", captor.getValue().getDisplayName());
        assertEquals("新的账号简介", captor.getValue().getBio());
        assertEquals("/api/public/avatars/1/avatar.png", captor.getValue().getAvatarUrl());
        assertEquals("新昵称", loginUser.getDisplayName());
        assertEquals("新的账号简介", loginUser.getBio());
        assertEquals("/api/public/avatars/1/avatar.png", loginUser.getAvatarUrl());
    }

    @Test
    void uploadCurrentUserAvatarStoresImageAndUpdatesAvatarUrl() throws Exception {
        AdminUser account = new AdminUser();
        account.setId(1L);
        account.setUsername("admin");
        account.setDisplayName("轻帆管理员");
        account.setBio("");
        account.setAvatarUrl("");
        account.setStatus("ENABLED");
        when(adminUserMapper.selectById(1L)).thenReturn(account);
        when(rbacUserSessionService.toLoginUser(any(AdminUser.class))).thenAnswer(invocation -> {
            AdminUser updated = invocation.getArgument(0);
            return new LoginUser(
                updated.getId(),
                updated.getUsername(),
                updated.getDisplayName(),
                updated.getBio(),
                updated.getAvatarUrl(),
                "ADMIN",
                updated.getStatus(),
                Arrays.asList("SUPER_ADMIN"),
                Arrays.asList("PROJECT_MANAGE")
            );
        });
        MockMultipartFile avatar = new MockMultipartFile(
            "file",
            "me.png",
            "image/png",
            new byte[] {
                (byte) 0x89, 0x50, 0x4E, 0x47,
                0x0D, 0x0A, 0x1A, 0x0A,
                0x00, 0x00, 0x00, 0x00
            }
        );

        LoginUser loginUser = authService.uploadCurrentUserAvatar(1L, "ADMIN", avatar);

        ArgumentCaptor<AdminUser> captor = ArgumentCaptor.forClass(AdminUser.class);
        verify(adminUserMapper).updateById(captor.capture());
        assertEquals("/api/public/avatars/ADMIN/1/avatar.png", captor.getValue().getAvatarUrl());
        assertEquals("/api/public/avatars/ADMIN/1/avatar.png", loginUser.getAvatarUrl());
        assertTrue(Files.exists(tempDir.resolve("avatars/admin/1/avatar.png")));
    }

    @Test
    void uploadCurrentUserAvatarRejectsSpoofedImageContent() {
        MockMultipartFile avatar = new MockMultipartFile(
            "file",
            "me.png",
            "image/png",
            "not really an image".getBytes()
        );

        ApiException exception = assertThrows(
            ApiException.class,
            () -> authService.uploadCurrentUserAvatar(1L, "ADMIN", avatar)
        );

        assertEquals("INVALID_AVATAR_TYPE", exception.getCode());
        verify(adminUserMapper, never()).updateById(any(AdminUser.class));
    }
}
