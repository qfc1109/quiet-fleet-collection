package com.qfc.admin;

import com.qfc.auth.LoginSessionService;
import com.qfc.common.ApiException;
import com.qfc.user.SiteUser;
import com.qfc.user.SiteUserMapper;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class UserManagementService {

    private static final String SUPER_ADMIN = "SUPER_ADMIN";
    private static final String NORMAL_ADMIN = "NORMAL_ADMIN";
    private static final String BUILT_IN_ADMIN_USERNAME = "admin";

    private final SiteUserMapper siteUserMapper;
    private final AdminUserMapper adminUserMapper;
    private final AdminRoleMapper adminRoleMapper;
    private final UserRoleMapper userRoleMapper;
    private final PasswordEncoder passwordEncoder;
    private final LoginSessionService loginSessionService;

    public UserManagementService(
        SiteUserMapper siteUserMapper,
        AdminUserMapper adminUserMapper,
        AdminRoleMapper adminRoleMapper,
        UserRoleMapper userRoleMapper,
        PasswordEncoder passwordEncoder,
        LoginSessionService loginSessionService
    ) {
        this.siteUserMapper = siteUserMapper;
        this.adminUserMapper = adminUserMapper;
        this.adminRoleMapper = adminRoleMapper;
        this.userRoleMapper = userRoleMapper;
        this.passwordEncoder = passwordEncoder;
        this.loginSessionService = loginSessionService;
    }

    public List<UserAccountView> listSiteUsers() {
        List<UserAccountView> views = new ArrayList<UserAccountView>();
        for (SiteUser account : siteUserMapper.selectAllUsers()) {
            views.add(UserAccountView.from(account, new ArrayList<String>()));
        }
        return views;
    }

    public List<UserAccountView> listAdminUsers() {
        List<UserAccountView> views = new ArrayList<UserAccountView>();
        for (AdminUser account : adminUserMapper.selectAllUsers()) {
            views.add(UserAccountView.from(account, adminRoleMapper.selectRoleCodesByUserId(account.getId())));
        }
        return views;
    }

    @Transactional(transactionManager = "siteTransactionManager")
    public UserAccountView createSiteUser(CreateUserRequest request) {
        ensureSiteUsernameAvailable(request.getUsername());
        SiteUser account = newSiteUser(
            request.getUsername(),
            request.getPassword(),
            request.getDisplayName(),
            request.getBio(),
            request.getAvatarUrl()
        );
        siteUserMapper.insert(account);
        return UserAccountView.from(account, new ArrayList<String>());
    }

    @Transactional(transactionManager = "adminTransactionManager")
    public UserAccountView createAdminUser(CreateAdminUserRequest request) {
        ensureAdminUsernameAvailable(request.getUsername());
        List<AdminRole> roles = resolveRoles(request.getRoleCodes(), true);
        AdminUser account = newAdminUser(
            request.getUsername(),
            request.getPassword(),
            request.getDisplayName(),
            request.getBio(),
            request.getAvatarUrl()
        );
        adminUserMapper.insert(account);
        assignRoles(account.getId(), roles);
        return UserAccountView.from(account, roleCodes(roles));
    }

    @Transactional(transactionManager = "siteTransactionManager")
    public UserAccountView updateSiteUser(Long userId, UpdateUserRequest request) {
        SiteUser account = findSiteUser(userId);
        applyUserUpdate(account, request);
        siteUserMapper.updateById(account);
        return UserAccountView.from(account, new ArrayList<String>());
    }

    @Transactional(transactionManager = "adminTransactionManager")
    public UserAccountView updateAdminUser(Long userId, UpdateAdminUserRequest request) {
        AdminUser account = findAdminUser(userId);
        if (BUILT_IN_ADMIN_USERNAME.equals(account.getUsername()) && RbacUserSessionService.STATUS_DISABLED.equals(request.getStatus())) {
            throw new ApiException("BUILT_IN_ADMIN_PROTECTED", "不能停用初始管理员", 400);
        }
        List<AdminRole> roles = resolveRoles(request.getRoleCodes(), true);
        if (BUILT_IN_ADMIN_USERNAME.equals(account.getUsername()) && !roleCodes(roles).contains(SUPER_ADMIN)) {
            throw new ApiException("BUILT_IN_ADMIN_PROTECTED", "不能移除初始管理员的超级管理员角色", 400);
        }
        applyUserUpdate(account, request);
        adminUserMapper.updateById(account);
        assignRoles(account.getId(), roles);
        return UserAccountView.from(account, roleCodes(roles));
    }

    @Transactional(transactionManager = "siteTransactionManager")
    public UserAccountView resetSiteUserPassword(Long userId, ResetPasswordRequest request) {
        SiteUser account = findSiteUser(userId);
        account.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        account.setUpdatedAt(LocalDateTime.now());
        siteUserMapper.updateById(account);
        loginSessionService.invalidateAccount(RbacUserSessionService.ACCOUNT_TYPE_SITE_USER, userId);
        return UserAccountView.from(account, new ArrayList<String>());
    }

    @Transactional(transactionManager = "adminTransactionManager")
    public UserAccountView resetAdminUserPassword(Long userId, ResetPasswordRequest request) {
        AdminUser account = findAdminUser(userId);
        account.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        account.setUpdatedAt(LocalDateTime.now());
        adminUserMapper.updateById(account);
        loginSessionService.invalidateAccount(RbacUserSessionService.ACCOUNT_TYPE_ADMIN, userId);
        return UserAccountView.from(account, adminRoleMapper.selectRoleCodesByUserId(userId));
    }

    private SiteUser newSiteUser(
        String username,
        String password,
        String displayName,
        String bio,
        String avatarUrl
    ) {
        LocalDateTime now = LocalDateTime.now();
        SiteUser account = new SiteUser();
        account.setUsername(username.trim());
        account.setPasswordHash(passwordEncoder.encode(password));
        account.setDisplayName(displayName);
        account.setBio(defaultString(bio));
        account.setAvatarUrl(defaultString(avatarUrl));
        account.setStatus(RbacUserSessionService.STATUS_ENABLED);
        account.setCreatedAt(now);
        account.setUpdatedAt(now);
        return account;
    }

    private AdminUser newAdminUser(
        String username,
        String password,
        String displayName,
        String bio,
        String avatarUrl
    ) {
        LocalDateTime now = LocalDateTime.now();
        AdminUser account = new AdminUser();
        account.setUsername(username.trim());
        account.setPasswordHash(passwordEncoder.encode(password));
        account.setDisplayName(displayName);
        account.setBio(defaultString(bio));
        account.setAvatarUrl(defaultString(avatarUrl));
        account.setStatus(RbacUserSessionService.STATUS_ENABLED);
        account.setCreatedAt(now);
        account.setUpdatedAt(now);
        return account;
    }

    private void ensureSiteUsernameAvailable(String username) {
        String normalizedUsername = username.trim();
        if (siteUserMapper.findByUsername(normalizedUsername) != null) {
            throw new ApiException("USERNAME_EXISTS", "账号已存在", 400);
        }
    }

    private void ensureAdminUsernameAvailable(String username) {
        String normalizedUsername = username.trim();
        if (adminUserMapper.findByUsername(normalizedUsername) != null) {
            throw new ApiException("USERNAME_EXISTS", "账号已存在", 400);
        }
    }

    private SiteUser findSiteUser(Long userId) {
        SiteUser account = siteUserMapper.selectById(userId);
        if (account == null) {
            throw new ApiException("USER_NOT_FOUND", "账号不存在", 404);
        }
        return account;
    }

    private AdminUser findAdminUser(Long userId) {
        AdminUser account = adminUserMapper.selectById(userId);
        if (account == null) {
            throw new ApiException("USER_NOT_FOUND", "账号不存在", 404);
        }
        return account;
    }

    private void applyUserUpdate(SiteUser account, UpdateUserRequest request) {
        applyCommonUserUpdate(account, request);
    }

    private void applyUserUpdate(AdminUser account, UpdateUserRequest request) {
        applyCommonUserUpdate(account, request);
    }

    private void applyCommonUserUpdate(SiteUser account, UpdateUserRequest request) {
        String status = request.getStatus() == null ? "" : request.getStatus().trim().toUpperCase();
        if (!RbacUserSessionService.STATUS_ENABLED.equals(status) && !RbacUserSessionService.STATUS_DISABLED.equals(status)) {
            throw new ApiException("INVALID_STATUS", "账号状态不正确", 400);
        }
        account.setDisplayName(request.getDisplayName());
        account.setBio(defaultString(request.getBio()));
        account.setAvatarUrl(defaultString(request.getAvatarUrl()));
        account.setStatus(status);
        account.setUpdatedAt(LocalDateTime.now());
    }

    private void applyCommonUserUpdate(AdminUser account, UpdateUserRequest request) {
        String status = request.getStatus() == null ? "" : request.getStatus().trim().toUpperCase();
        if (!RbacUserSessionService.STATUS_ENABLED.equals(status) && !RbacUserSessionService.STATUS_DISABLED.equals(status)) {
            throw new ApiException("INVALID_STATUS", "账号状态不正确", 400);
        }
        account.setDisplayName(request.getDisplayName());
        account.setBio(defaultString(request.getBio()));
        account.setAvatarUrl(defaultString(request.getAvatarUrl()));
        account.setStatus(status);
        account.setUpdatedAt(LocalDateTime.now());
    }

    private List<AdminRole> resolveRoles(List<String> roleCodes, boolean requireAny) {
        List<String> normalizedCodes = normalizedCodes(roleCodes);
        if (requireAny && normalizedCodes.isEmpty()) {
            throw new ApiException("ROLE_REQUIRED", "后台账号至少需要一个角色", 400);
        }
        List<AdminRole> roles = normalizedCodes.isEmpty()
            ? new ArrayList<AdminRole>()
            : adminRoleMapper.selectByCodes(normalizedCodes);
        if (roles.size() != normalizedCodes.size()) {
            throw new ApiException("ROLE_NOT_FOUND", "存在无效角色", 400);
        }
        return roles;
    }

    private void assignRoles(Long userId, List<AdminRole> roles) {
        userRoleMapper.deleteByUserId(userId);
        for (AdminRole role : roles) {
            UserRole userRole = new UserRole();
            userRole.setUserId(userId);
            userRole.setRoleId(role.getId());
            userRoleMapper.insert(userRole);
        }
    }

    private List<String> roleCodes(List<AdminRole> roles) {
        List<String> codes = new ArrayList<String>();
        for (AdminRole role : roles) {
            codes.add(role.getCode());
        }
        return codes;
    }

    private List<String> normalizedCodes(List<String> codes) {
        Set<String> values = new HashSet<String>();
        if (codes != null) {
            for (String code : codes) {
                if (StringUtils.hasText(code)) {
                    values.add(code.trim().toUpperCase());
                }
            }
        }
        return new ArrayList<String>(values);
    }

    private String defaultString(String value) {
        return value == null ? "" : value;
    }
}
