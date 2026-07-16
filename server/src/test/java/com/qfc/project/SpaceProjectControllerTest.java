package com.qfc.project;

import com.qfc.auth.LoginUser;
import com.qfc.auth.LoginSessionProperties;
import com.qfc.auth.LoginSessionService;
import com.qfc.common.ApiResponse;
import com.qfc.config.SessionKeys;
import com.qfc.file.FileArchive;
import com.qfc.file.FileService;
import com.qfc.file.ProjectFileArchiveRequest;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.autoconfigure.web.servlet.MultipartProperties;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.util.unit.DataSize;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

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
        MultipartProperties multipartProperties = new MultipartProperties();
        multipartProperties.setMaxFileSize(DataSize.ofMegabytes(2));
        controller = new SpaceProjectController(projectService, fileService, new LoginSessionService(properties), multipartProperties);
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

    @Test
    void uploadLimitsUsesConfiguredMultipartMaxFileSize() {
        MockHttpSession session = new MockHttpSession();
        session.setAttribute(
            SessionKeys.SITE_LOGIN_USER,
            new LoginUser(5L, "player01", "网站用户", "SITE_USER", "ENABLED", Arrays.asList(), Arrays.asList())
        );

        ApiResponse<SpaceUploadLimitView> response = controller.uploadLimits(requestWithSession(session));

        assertEquals(2L * 1024L * 1024L, response.getData().getMaxFileSizeBytes());
    }

    @Test
    void downloadProjectArchiveUsesSiteUserSession() {
        MockHttpSession session = new MockHttpSession();
        session.setAttribute(
            SessionKeys.SITE_LOGIN_USER,
            new LoginUser(5L, "player01", "网站用户", "SITE_USER", "ENABLED", Arrays.asList(), Arrays.asList())
        );
        FileArchive archive = new FileArchive("docs.zip", "application/zip", Collections.emptyList());
        when(fileService.openOwnedProjectArchive(5L, 31L)).thenReturn(archive);

        ResponseEntity<StreamingResponseBody> response = controller.downloadProjectArchive(31L, requestWithSession(session));

        assertEquals("application/zip", response.getHeaders().getContentType().toString());
        verify(fileService).openOwnedProjectArchive(5L, 31L);
    }

    @Test
    void downloadSelectedProjectFilesArchiveUsesSiteUserSession() {
        MockHttpSession session = new MockHttpSession();
        session.setAttribute(
            SessionKeys.SITE_LOGIN_USER,
            new LoginUser(5L, "player01", "网站用户", "SITE_USER", "ENABLED", Arrays.asList(), Arrays.asList())
        );
        ProjectFileArchiveRequest archiveRequest = new ProjectFileArchiveRequest();
        archiveRequest.setFileIds(Arrays.asList(3L, 4L));
        FileArchive archive = new FileArchive("docs-selected.zip", "application/zip", Collections.emptyList());
        when(fileService.openOwnedSelectedProjectFilesArchive(5L, 31L, Arrays.asList(3L, 4L))).thenReturn(archive);

        ResponseEntity<StreamingResponseBody> response =
            controller.downloadSelectedProjectFilesArchive(31L, archiveRequest, requestWithSession(session));

        assertEquals("application/zip", response.getHeaders().getContentType().toString());
        verify(fileService).openOwnedSelectedProjectFilesArchive(5L, 31L, Arrays.asList(3L, 4L));
    }

    private MockHttpServletRequest requestWithSession(MockHttpSession session) {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setSession(session);
        return request;
    }
}
