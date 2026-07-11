package com.qfc.project;

import com.qfc.auth.LoginUser;
import com.qfc.auth.LoginSessionProperties;
import com.qfc.auth.LoginSessionService;
import com.qfc.common.ApiResponse;
import com.qfc.config.SessionKeys;
import com.qfc.file.FileService;
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
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SpaceProjectControllerTest {

    @Mock
    private ProjectService projectService;

    @Mock
    private FileService fileService;

    private SpaceProjectController controller;

    @BeforeEach
    void setUp() {
        LoginSessionProperties properties = new LoginSessionProperties();
        properties.setSessionLifetimeSeconds(86400);
        controller = new SpaceProjectController(projectService, fileService, new LoginSessionService(properties));
    }

    @Test
    void listProjectsUsesSiteUserSessionId() {
        MockHttpSession session = new MockHttpSession();
        session.setAttribute(
            SessionKeys.SITE_LOGIN_USER,
            new LoginUser(5L, "player01", "网站用户", "SITE_USER", "ENABLED", Arrays.asList(), Arrays.asList())
        );
        ProjectView project = new ProjectView();
        project.setId(11L);
        project.setOwnerUserId(5L);
        project.setName("我的项目");
        when(projectService.listSpaceProjects(5L)).thenReturn(Arrays.asList(project));

        ApiResponse<List<ProjectView>> response = controller.listProjects(requestWithSession(session));

        assertEquals(1, response.getData().size());
        assertEquals(5L, response.getData().get(0).getOwnerUserId());
    }

    @Test
    void deleteProjectUsesSiteUserSessionAndRemoteIp() {
        MockHttpSession session = new MockHttpSession();
        session.setAttribute(
            SessionKeys.SITE_LOGIN_USER,
            new LoginUser(5L, "player01", "网站用户", "SITE_USER", "ENABLED", Arrays.asList(), Arrays.asList())
        );
        MockHttpServletRequest request = requestWithSession(session);
        request.setRemoteAddr("127.0.0.1");

        ApiResponse<Boolean> response = controller.deleteProject(31L, request);

        assertEquals(Boolean.TRUE, response.getData());
        verify(projectService).softDeleteSpaceProject(5L, "player01", 31L, "127.0.0.1");
    }

    private MockHttpServletRequest requestWithSession(MockHttpSession session) {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setSession(session);
        return request;
    }
}
