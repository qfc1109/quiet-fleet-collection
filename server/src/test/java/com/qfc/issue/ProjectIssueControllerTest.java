package com.qfc.issue;

import com.qfc.auth.LoginUser;
import com.qfc.auth.LoginSessionProperties;
import com.qfc.auth.LoginSessionService;
import com.qfc.common.ApiException;
import com.qfc.common.ApiResponse;
import com.qfc.config.SessionKeys;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpSession;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProjectIssueControllerTest {

    @Mock
    private ProjectIssueService projectIssueService;

    private ProjectIssueController controller;

    @BeforeEach
    void setUp() {
        LoginSessionProperties properties = new LoginSessionProperties();
        properties.setSessionLifetimeSeconds(86400);
        controller = new ProjectIssueController(projectIssueService, new LoginSessionService(properties));
    }

    @Test
    void createIssueRequiresSiteLogin() {
        ProjectIssueCreateRequest request = new ProjectIssueCreateRequest();
        request.setTitle("缺少配置说明");
        request.setContent("希望补充完整配置说明。");

        ApiException exception = assertThrows(
            ApiException.class,
            () -> controller.createIssue("battle-log", request, requestWithSession(new MockHttpSession()))
        );

        assertEquals("UNAUTHORIZED", exception.getCode());
    }

    @Test
    void createIssueUsesSiteSessionUserId() {
        MockHttpSession session = new MockHttpSession();
        session.setAttribute(
            SessionKeys.SITE_LOGIN_USER,
            new LoginUser(5L, "player01", "项目访客", "SITE_USER", "ENABLED", Arrays.asList(), Arrays.asList())
        );
        ProjectIssueCreateRequest request = new ProjectIssueCreateRequest();
        request.setTitle("缺少配置说明");
        request.setContent("希望补充完整配置说明。");
        ProjectIssueView view = new ProjectIssueView();
        view.setId(51L);
        when(projectIssueService.createIssue("battle-log", 5L, request)).thenReturn(view);

        ApiResponse<ProjectIssueView> response = controller.createIssue("battle-log", request, requestWithSession(session));

        assertEquals(51L, response.getData().getId());
        verify(projectIssueService).createIssue("battle-log", 5L, request);
    }

    @Test
    void listSpaceProjectIssuesUsesSiteSessionUserId() {
        MockHttpSession session = new MockHttpSession();
        session.setAttribute(
            SessionKeys.SITE_LOGIN_USER,
            new LoginUser(5L, "owner01", "项目作者", "SITE_USER", "ENABLED", Arrays.asList(), Arrays.asList())
        );
        ProjectIssueView view = new ProjectIssueView();
        view.setId(61L);
        when(projectIssueService.listSpaceProjectIssues(5L, 7L)).thenReturn(Arrays.asList(view));

        ApiResponse<List<ProjectIssueView>> response = controller.listSpaceProjectIssues(7L, requestWithSession(session));

        assertEquals(1, response.getData().size());
        assertEquals(61L, response.getData().get(0).getId());
        verify(projectIssueService).listSpaceProjectIssues(5L, 7L);
    }

    private MockHttpServletRequest requestWithSession(MockHttpSession session) {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setSession(session);
        return request;
    }
}
