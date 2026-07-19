package com.qfc;

import com.qfc.admin.AdminRoleMapper;
import com.qfc.admin.AdminUserMapper;
import com.qfc.admin.PermissionMapper;
import com.qfc.admin.RolePermissionMapper;
import com.qfc.admin.UserRoleMapper;
import com.qfc.project.ProjectMapper;
import com.qfc.file.ProjectFileMapper;
import com.qfc.issue.ProjectIssueMapper;
import com.qfc.feedback.SiteFeedbackMapper;
import com.qfc.user.SiteUserMapper;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(properties = {
    "spring.autoconfigure.exclude=org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration",
    "spring.session.store-type=none",
    "qfc.auth.token.secret=test-secret-key-for-app-context-32-chars"
})
class QfcApplicationTests {

    @MockBean
    private SiteUserMapper siteUserMapper;

    @MockBean
    private AdminUserMapper adminUserMapper;

    @MockBean
    private ProjectMapper projectMapper;

    @MockBean
    private ProjectFileMapper projectFileMapper;

    @MockBean
    private ProjectIssueMapper projectIssueMapper;

    @MockBean
    private SiteFeedbackMapper siteFeedbackMapper;

    @MockBean
    private AdminRoleMapper adminRoleMapper;

    @MockBean
    private PermissionMapper permissionMapper;

    @MockBean
    private UserRoleMapper userRoleMapper;

    @MockBean
    private RolePermissionMapper rolePermissionMapper;

    @Test
    void contextLoads() {
    }
}
