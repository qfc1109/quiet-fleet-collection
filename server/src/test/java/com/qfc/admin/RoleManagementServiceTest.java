package com.qfc.admin;

import com.qfc.common.ApiException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@ExtendWith(MockitoExtension.class)
class RoleManagementServiceTest {

    @Mock
    private AdminRoleMapper adminRoleMapper;

    @Mock
    private PermissionMapper permissionMapper;

    @Mock
    private RolePermissionMapper rolePermissionMapper;

    private RoleManagementService roleManagementService;

    @BeforeEach
    void setUp() {
        roleManagementService = new RoleManagementService(
            adminRoleMapper,
            permissionMapper,
            rolePermissionMapper
        );
    }

    @Test
    void createRoleIsDisabledBecauseRolesAreFixed() {
        CreateRoleRequest request = new CreateRoleRequest();
        request.setCode("CUSTOM_ADMIN");
        request.setName("自定义管理员");

        ApiException exception = assertThrows(
            ApiException.class,
            () -> roleManagementService.createRole(request)
        );

        assertEquals("ROLE_CREATION_DISABLED", exception.getCode());
    }
}
