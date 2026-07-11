package com.qfc.admin;

import com.qfc.common.ApiResponse;
import java.util.List;
import javax.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class AdminManagementController {

    private final UserManagementService userManagementService;
    private final RoleManagementService roleManagementService;

    public AdminManagementController(
        UserManagementService userManagementService,
        RoleManagementService roleManagementService
    ) {
        this.userManagementService = userManagementService;
        this.roleManagementService = roleManagementService;
    }

    @GetMapping("/api/admin/site-users")
    public ApiResponse<List<UserAccountView>> listSiteUsers() {
        return ApiResponse.success(userManagementService.listSiteUsers());
    }

    @PostMapping("/api/admin/site-users")
    public ApiResponse<UserAccountView> createSiteUser(@Valid @RequestBody CreateUserRequest request) {
        return ApiResponse.success(userManagementService.createSiteUser(request));
    }

    @PutMapping("/api/admin/site-users/{userId}")
    public ApiResponse<UserAccountView> updateSiteUser(
        @PathVariable Long userId,
        @Valid @RequestBody UpdateUserRequest request
    ) {
        return ApiResponse.success(userManagementService.updateSiteUser(userId, request));
    }

    @PostMapping("/api/admin/site-users/{userId}/reset-password")
    public ApiResponse<UserAccountView> resetSiteUserPassword(
        @PathVariable Long userId,
        @Valid @RequestBody ResetPasswordRequest request
    ) {
        return ApiResponse.success(userManagementService.resetSiteUserPassword(userId, request));
    }

    @GetMapping("/api/admin/admin-users")
    public ApiResponse<List<UserAccountView>> listAdminUsers() {
        return ApiResponse.success(userManagementService.listAdminUsers());
    }

    @PostMapping("/api/admin/admin-users")
    public ApiResponse<UserAccountView> createAdminUser(@Valid @RequestBody CreateAdminUserRequest request) {
        return ApiResponse.success(userManagementService.createAdminUser(request));
    }

    @PutMapping("/api/admin/admin-users/{userId}")
    public ApiResponse<UserAccountView> updateAdminUser(
        @PathVariable Long userId,
        @Valid @RequestBody UpdateAdminUserRequest request
    ) {
        return ApiResponse.success(userManagementService.updateAdminUser(userId, request));
    }

    @PostMapping("/api/admin/admin-users/{userId}/reset-password")
    public ApiResponse<UserAccountView> resetAdminUserPassword(
        @PathVariable Long userId,
        @Valid @RequestBody ResetPasswordRequest request
    ) {
        return ApiResponse.success(userManagementService.resetAdminUserPassword(userId, request));
    }

    @GetMapping("/api/admin/roles")
    public ApiResponse<List<AdminRoleView>> listRoles() {
        return ApiResponse.success(roleManagementService.listRoles());
    }

    @PostMapping("/api/admin/roles")
    public ApiResponse<AdminRoleView> createRole(@Valid @RequestBody CreateRoleRequest request) {
        return ApiResponse.success(roleManagementService.createRole(request));
    }

    @PutMapping("/api/admin/roles/{roleId}")
    public ApiResponse<AdminRoleView> updateRole(
        @PathVariable Long roleId,
        @Valid @RequestBody UpdateRoleRequest request
    ) {
        return ApiResponse.success(roleManagementService.updateRole(roleId, request));
    }

    @GetMapping("/api/admin/permissions")
    public ApiResponse<List<PermissionView>> listPermissions() {
        return ApiResponse.success(roleManagementService.listPermissions());
    }
}
