package com.qfc.issue;

import com.qfc.common.ApiException;
import com.qfc.project.Project;
import com.qfc.project.ProjectMapper;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProjectIssueServiceTest {

    @Mock
    private ProjectMapper projectMapper;

    @Mock
    private ProjectIssueMapper projectIssueMapper;

    private ProjectIssueService projectIssueService;

    @BeforeEach
    void setUp() {
        projectIssueService = new ProjectIssueService(projectMapper, projectIssueMapper);
    }

    @Test
    void createIssueUsesPublicProjectAndCurrentSiteUser() {
        Project project = new Project();
        project.setId(7L);
        project.setSlug("battle-log");
        when(projectMapper.selectPublicBySlug("battle-log")).thenReturn(project);
        when(projectIssueMapper.insert(any(ProjectIssue.class))).thenAnswer(invocation -> {
            ProjectIssue issue = invocation.getArgument(0);
            issue.setId(51L);
            return 1;
        });
        ProjectIssueView view = new ProjectIssueView();
        view.setId(51L);
        view.setProjectId(7L);
        view.setAuthorUserId(5L);
        view.setTitle("登录后白屏");
        view.setContent("打开战斗日志页面时白屏，需要排查。");
        view.setStatus("OPEN");
        when(projectIssueMapper.selectViewById(51L)).thenReturn(view);
        ProjectIssueCreateRequest request = new ProjectIssueCreateRequest();
        request.setTitle("登录后白屏");
        request.setContent("打开战斗日志页面时白屏，需要排查。");

        ProjectIssueView created = projectIssueService.createIssue("battle-log", 5L, request);

        ArgumentCaptor<ProjectIssue> captor = ArgumentCaptor.forClass(ProjectIssue.class);
        verify(projectIssueMapper).insert(captor.capture());
        ProjectIssue saved = captor.getValue();
        assertEquals(7L, saved.getProjectId());
        assertEquals(5L, saved.getAuthorUserId());
        assertEquals("登录后白屏", saved.getTitle());
        assertEquals("打开战斗日志页面时白屏，需要排查。", saved.getContent());
        assertEquals("OPEN", saved.getStatus());
        assertNotNull(saved.getCreatedAt());
        assertNotNull(saved.getUpdatedAt());
        assertEquals(51L, created.getId());
        assertEquals("OPEN", created.getStatus());
    }

    @Test
    void createIssueRejectsMissingPublicProject() {
        ProjectIssueCreateRequest request = new ProjectIssueCreateRequest();
        request.setTitle("找不到项目");
        request.setContent("这个 slug 不存在。");
        when(projectMapper.selectPublicBySlug("missing")).thenReturn(null);

        ApiException exception = assertThrows(
            ApiException.class,
            () -> projectIssueService.createIssue("missing", 5L, request)
        );

        assertEquals("PROJECT_NOT_FOUND", exception.getCode());
        verify(projectIssueMapper, never()).insert(any(ProjectIssue.class));
    }

    @Test
    void listSpaceProjectIssuesRequiresProjectOwner() {
        Project project = new Project();
        project.setId(7L);
        project.setOwnerUserId(5L);
        when(projectMapper.selectById(7L)).thenReturn(project);
        ProjectIssueView issue = new ProjectIssueView();
        issue.setId(51L);
        issue.setProjectId(7L);
        issue.setAuthorDisplayName("项目访客");
        issue.setTitle("配置说明缺失");
        when(projectIssueMapper.selectViewsByProjectId(7L)).thenReturn(Arrays.asList(issue));

        List<ProjectIssueView> issues = projectIssueService.listSpaceProjectIssues(5L, 7L);

        assertEquals(1, issues.size());
        assertEquals("项目访客", issues.get(0).getAuthorDisplayName());
        assertEquals("配置说明缺失", issues.get(0).getTitle());
    }

    @Test
    void listSpaceProjectIssuesRejectsAnotherOwnersProject() {
        Project project = new Project();
        project.setId(7L);
        project.setOwnerUserId(9L);
        when(projectMapper.selectById(7L)).thenReturn(project);

        ApiException exception = assertThrows(
            ApiException.class,
            () -> projectIssueService.listSpaceProjectIssues(5L, 7L)
        );

        assertEquals("PROJECT_NOT_FOUND", exception.getCode());
        verify(projectIssueMapper, never()).selectViewsByProjectId(7L);
    }
}
