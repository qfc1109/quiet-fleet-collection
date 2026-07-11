package com.qfc.issue;

import com.qfc.common.ApiException;
import com.qfc.project.Project;
import com.qfc.project.ProjectMapper;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class ProjectIssueService {

    private static final String DEFAULT_STATUS = "OPEN";

    private final ProjectMapper projectMapper;
    private final ProjectIssueMapper projectIssueMapper;

    public ProjectIssueService(ProjectMapper projectMapper, ProjectIssueMapper projectIssueMapper) {
        this.projectMapper = projectMapper;
        this.projectIssueMapper = projectIssueMapper;
    }

    public ProjectIssueView createIssue(String projectSlug, Long authorUserId, ProjectIssueCreateRequest request) {
        Project project = projectMapper.selectPublicBySlug(projectSlug);
        if (project == null) {
            throw new ApiException("PROJECT_NOT_FOUND", "项目不存在", 404);
        }

        LocalDateTime now = LocalDateTime.now();
        ProjectIssue issue = new ProjectIssue();
        issue.setProjectId(project.getId());
        issue.setAuthorUserId(authorUserId);
        issue.setTitle(request.getTitle());
        issue.setContent(request.getContent());
        issue.setStatus(DEFAULT_STATUS);
        issue.setCreatedAt(now);
        issue.setUpdatedAt(now);
        projectIssueMapper.insert(issue);
        return projectIssueMapper.selectViewById(issue.getId());
    }

    public List<ProjectIssueView> listSpaceProjectIssues(Long ownerUserId, Long projectId) {
        requireOwnedProject(ownerUserId, projectId);
        return projectIssueMapper.selectViewsByProjectId(projectId);
    }

    private Project requireOwnedProject(Long ownerUserId, Long projectId) {
        Project project = projectMapper.selectById(projectId);
        if (project == null || project.isDeleted() || project.getOwnerUserId() == null || !project.getOwnerUserId().equals(ownerUserId)) {
            throw new ApiException("PROJECT_NOT_FOUND", "项目不存在", 404);
        }
        return project;
    }
}
