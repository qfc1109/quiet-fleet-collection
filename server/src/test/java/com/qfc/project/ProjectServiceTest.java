package com.qfc.project;

import com.qfc.log.SiteOperationLogService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProjectServiceTest {

    @Mock
    private ProjectMapper projectMapper;

    @Mock
    private SiteOperationLogService siteOperationLogService;

    private ProjectService projectService;

    @BeforeEach
    void setUp() {
        projectService = new ProjectService(projectMapper, siteOperationLogService);
    }

    @Test
    void listPublicProjectsReturnsOnlyPublicProjectViews() {
        Project project = new Project();
        project.setId(11L);
        project.setName("战斗日志");
        project.setSlug("battle-log");
        project.setDescription("游戏服务端排障记录");
        project.setVisibility("PUBLIC");
        project.setSortOrder(10);
        project.setCreatedAt(LocalDateTime.of(2026, 7, 1, 10, 0));
        project.setUpdatedAt(LocalDateTime.of(2026, 7, 1, 10, 0));
        when(projectMapper.selectPublicProjects()).thenReturn(Arrays.asList(project));

        List<ProjectView> views = projectService.listPublicProjects();

        assertEquals(1, views.size());
        assertEquals(11L, views.get(0).getId());
        assertEquals("战斗日志", views.get(0).getName());
        assertEquals("battle-log", views.get(0).getSlug());
        assertEquals("PUBLIC", views.get(0).getVisibility());
    }

    @Test
    void createProjectDefaultsVisibilityAndTimestamps() {
        ProjectCreateRequest request = new ProjectCreateRequest();
        request.setName("协议文档");
        request.setSlug("protocol-docs");
        request.setDescription("客户端和服务端协议说明");
        when(projectMapper.insert(any(Project.class))).thenAnswer(invocation -> {
            Project inserted = invocation.getArgument(0);
            inserted.setId(21L);
            return 1;
        });

        ProjectView view = projectService.createProject(request);

        ArgumentCaptor<Project> captor = ArgumentCaptor.forClass(Project.class);
        verify(projectMapper).insert(captor.capture());
        Project saved = captor.getValue();
        assertEquals("协议文档", saved.getName());
        assertEquals("protocol-docs", saved.getSlug());
        assertEquals("客户端和服务端协议说明", saved.getDescription());
        assertEquals("PUBLIC", saved.getVisibility());
        assertEquals(0, saved.getSortOrder());
        assertNotNull(saved.getCreatedAt());
        assertNotNull(saved.getUpdatedAt());
        assertEquals(21L, view.getId());
    }

    @Test
    void createProjectRejectsDuplicateSlugWithBusinessError() {
        ProjectCreateRequest request = new ProjectCreateRequest();
        request.setName("协议文档");
        request.setSlug("protocol-docs");
        Project existing = new Project();
        existing.setId(12L);
        existing.setSlug("protocol-docs");
        when(projectMapper.selectActiveBySlug("protocol-docs")).thenReturn(existing);

        com.qfc.common.ApiException exception = assertThrows(
            com.qfc.common.ApiException.class,
            () -> projectService.createProject(request)
        );

        assertEquals("PROJECT_SLUG_EXISTS", exception.getCode());
        verify(projectMapper, never()).insert(any(Project.class));
    }

    @Test
    void createProjectRejectsInvalidVisibility() {
        ProjectCreateRequest request = new ProjectCreateRequest();
        request.setName("协议文档");
        request.setSlug("protocol-docs");
        request.setVisibility("SECRET");

        com.qfc.common.ApiException exception = assertThrows(
            com.qfc.common.ApiException.class,
            () -> projectService.createProject(request)
        );

        assertEquals("INVALID_PROJECT_VISIBILITY", exception.getCode());
        verify(projectMapper, never()).insert(any(Project.class));
    }

    @Test
    void listSpaceProjectsReturnsOnlyCurrentSiteUsersProjects() {
        Project project = new Project();
        project.setId(31L);
        project.setOwnerUserId(5L);
        project.setName("个人排障记录");
        project.setSlug("personal-debug-notes");
        project.setDescription("只属于当前网站用户");
        project.setVisibility("PUBLIC");
        project.setSortOrder(0);
        when(projectMapper.selectByOwnerUserId(5L)).thenReturn(Arrays.asList(project));

        List<ProjectView> views = projectService.listSpaceProjects(5L);

        assertEquals(1, views.size());
        assertEquals(31L, views.get(0).getId());
        assertEquals(5L, views.get(0).getOwnerUserId());
        assertEquals("个人排障记录", views.get(0).getName());
    }

    @Test
    void createSpaceProjectSetsCurrentSiteUserAsOwner() {
        ProjectCreateRequest request = new ProjectCreateRequest();
        request.setName("个人协议文档");
        request.setSlug("my-protocol-docs");
        request.setDescription("我维护的服务端协议说明");
        when(projectMapper.insert(any(Project.class))).thenAnswer(invocation -> {
            Project inserted = invocation.getArgument(0);
            inserted.setId(41L);
            return 1;
        });

        ProjectView view = projectService.createSpaceProject(5L, request);

        ArgumentCaptor<Project> captor = ArgumentCaptor.forClass(Project.class);
        verify(projectMapper).insert(captor.capture());
        assertEquals(5L, captor.getValue().getOwnerUserId());
        assertEquals(5L, view.getOwnerUserId());
    }

    @Test
    void createSpaceProjectAllowsReusingSlugWhenNoActiveProjectUsesIt() {
        ProjectCreateRequest request = new ProjectCreateRequest();
        request.setName("新的小画文档");
        request.setSlug("xiaohua");
        request.setDescription("重新创建的小画文档");
        when(projectMapper.selectActiveBySlug("xiaohua")).thenReturn(null);

        ProjectView view = projectService.createSpaceProject(5L, request);

        ArgumentCaptor<Project> captor = ArgumentCaptor.forClass(Project.class);
        verify(projectMapper).insert(captor.capture());
        Project saved = captor.getValue();
        assertEquals(5L, saved.getOwnerUserId());
        assertEquals("新的小画文档", saved.getName());
        assertEquals("xiaohua", saved.getSlug());
        assertEquals("xiaohua", view.getSlug());
    }

    @Test
    void updateSpaceProjectRejectsProjectOwnedByAnotherSiteUser() {
        Project project = new Project();
        project.setId(31L);
        project.setOwnerUserId(9L);
        project.setName("其他人的项目");
        project.setSlug("other-user-project");
        when(projectMapper.selectById(31L)).thenReturn(project);
        ProjectUpdateRequest request = new ProjectUpdateRequest();
        request.setName("非法修改");
        request.setSlug("illegal-update");

        com.qfc.common.ApiException exception = assertThrows(
            com.qfc.common.ApiException.class,
            () -> projectService.updateSpaceProject(5L, 31L, request)
        );

        assertEquals("PROJECT_NOT_FOUND", exception.getCode());
        verify(projectMapper, never()).updateById(any(Project.class));
    }

    @Test
    void softDeleteSpaceProjectMarksProjectDeletedAndWritesSiteLog() {
        Project project = new Project();
        project.setId(31L);
        project.setOwnerUserId(5L);
        project.setName("个人排障记录");
        project.setSlug("personal-debug-notes");
        project.setVisibility("PUBLIC");
        project.setCreatedAt(LocalDateTime.of(2026, 7, 1, 10, 0));
        project.setUpdatedAt(LocalDateTime.of(2026, 7, 1, 10, 0));
        when(projectMapper.selectById(31L)).thenReturn(project);

        projectService.softDeleteSpaceProject(5L, "player01", 31L, "127.0.0.1");

        ArgumentCaptor<Project> captor = ArgumentCaptor.forClass(Project.class);
        verify(projectMapper).updateById(captor.capture());
        Project deleted = captor.getValue();
        assertEquals(5L, deleted.getDeletedByUserId());
        assertNotNull(deleted.getDeletedAt());
        assertNotNull(deleted.getUpdatedAt());
        verify(siteOperationLogService).logProjectSoftDelete(
            5L,
            "player01",
            31L,
            "个人排障记录",
            "personal-debug-notes",
            "127.0.0.1"
        );
    }

    @Test
    void softDeleteSpaceProjectRejectsAnotherOwnersProject() {
        Project project = new Project();
        project.setId(31L);
        project.setOwnerUserId(9L);
        project.setName("其他人的项目");
        project.setSlug("other-user-project");
        when(projectMapper.selectById(31L)).thenReturn(project);

        com.qfc.common.ApiException exception = assertThrows(
            com.qfc.common.ApiException.class,
            () -> projectService.softDeleteSpaceProject(5L, "player01", 31L, "127.0.0.1")
        );

        assertEquals("PROJECT_NOT_FOUND", exception.getCode());
        verify(projectMapper, never()).updateById(any(Project.class));
        verify(siteOperationLogService, never()).logProjectSoftDelete(
            any(Long.class),
            any(String.class),
            any(Long.class),
            any(String.class),
            any(String.class),
            any(String.class)
        );
    }
}
