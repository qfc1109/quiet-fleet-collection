package com.qfc.project;

import com.qfc.common.ApiException;
import com.qfc.log.SiteOperationLogService;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class ProjectService {

    private static final String DEFAULT_VISIBILITY = "PUBLIC";
    private static final Set<String> ALLOWED_VISIBILITIES = new HashSet<String>(Arrays.asList("PUBLIC", "PRIVATE"));
    private static final Pattern SLUG_PATTERN = Pattern.compile("^[A-Za-z0-9][A-Za-z0-9_-]{0,119}$");

    private final ProjectMapper projectMapper;
    private final SiteOperationLogService siteOperationLogService;

    public ProjectService(ProjectMapper projectMapper, SiteOperationLogService siteOperationLogService) {
        this.projectMapper = projectMapper;
        this.siteOperationLogService = siteOperationLogService;
    }

    public List<ProjectView> listPublicProjects() {
        return toViews(projectMapper.selectPublicProjects());
    }

    public ProjectView getPublicProject(String slug) {
        Project project = projectMapper.selectPublicBySlug(slug);
        if (project == null) {
            throw new ApiException("PROJECT_NOT_FOUND", "项目不存在", 404);
        }
        return ProjectView.from(project);
    }

    public List<ProjectView> listAllProjects() {
        return toViews(projectMapper.selectAllProjects());
    }

    public List<ProjectView> listSpaceProjects(Long ownerUserId) {
        return toViews(projectMapper.selectByOwnerUserId(ownerUserId));
    }

    public ProjectView createProject(ProjectCreateRequest request) {
        return createProject(null, request);
    }

    public ProjectView createSpaceProject(Long ownerUserId, ProjectCreateRequest request) {
        return createProject(ownerUserId, request);
    }

    private ProjectView createProject(Long ownerUserId, ProjectCreateRequest request) {
        String slug = normalizeSlug(request.getSlug());
        ensureSlugAvailable(slug, null);
        LocalDateTime now = LocalDateTime.now();
        Project project = new Project();
        project.setOwnerUserId(ownerUserId);
        project.setName(request.getName().trim());
        project.setSlug(slug);
        project.setDescription(defaultString(request.getDescription()));
        project.setCoverUrl(defaultString(request.getCoverUrl()));
        project.setVisibility(normalizeVisibility(request.getVisibility()));
        project.setSortOrder(request.getSortOrder() == null ? 0 : request.getSortOrder());
        project.setCreatedAt(now);
        project.setUpdatedAt(now);
        try {
            projectMapper.insert(project);
        } catch (DuplicateKeyException exception) {
            throw slugExists();
        }
        return ProjectView.from(project);
    }

    public ProjectView updateProject(Long projectId, ProjectUpdateRequest request) {
        Project project = projectMapper.selectById(projectId);
        if (project == null || project.isDeleted()) {
            throw new ApiException("PROJECT_NOT_FOUND", "项目不存在", 404);
        }
        return updateProject(project, request);
    }

    public ProjectView updateSpaceProject(Long ownerUserId, Long projectId, ProjectUpdateRequest request) {
        Project project = requireOwnedActiveProject(ownerUserId, projectId);
        return updateProject(project, request);
    }

    @Transactional(transactionManager = "siteTransactionManager")
    public void softDeleteSpaceProject(Long ownerUserId, String username, Long projectId, String ipAddress) {
        Project project = requireOwnedActiveProject(ownerUserId, projectId);
        LocalDateTime now = LocalDateTime.now();
        project.setDeletedAt(now);
        project.setDeletedByUserId(ownerUserId);
        project.setUpdatedAt(now);
        projectMapper.updateById(project);
        siteOperationLogService.logProjectSoftDelete(
            ownerUserId,
            username,
            project.getId(),
            project.getName(),
            project.getSlug(),
            ipAddress
        );
    }

    private ProjectView updateProject(Project project, ProjectUpdateRequest request) {
        String slug = normalizeSlug(request.getSlug());
        ensureSlugAvailable(slug, project.getId());
        project.setName(request.getName().trim());
        project.setSlug(slug);
        project.setDescription(defaultString(request.getDescription()));
        project.setCoverUrl(defaultString(request.getCoverUrl()));
        project.setVisibility(normalizeVisibility(request.getVisibility()));
        project.setSortOrder(request.getSortOrder() == null ? 0 : request.getSortOrder());
        project.setUpdatedAt(LocalDateTime.now());
        try {
            projectMapper.updateById(project);
        } catch (DuplicateKeyException exception) {
            throw slugExists();
        }
        return ProjectView.from(project);
    }

    private List<ProjectView> toViews(List<Project> projects) {
        List<ProjectView> views = new ArrayList<ProjectView>();
        for (Project project : projects) {
            views.add(ProjectView.from(project));
        }
        return views;
    }

    private String defaultString(String value) {
        return value == null ? "" : value;
    }

    private String normalizeSlug(String slug) {
        String normalized = slug == null ? "" : slug.trim();
        if (!SLUG_PATTERN.matcher(normalized).matches()) {
            throw new ApiException("INVALID_PROJECT_SLUG", "访问路径只能包含字母、数字、下划线和短横线，并且必须以字母或数字开头", 400);
        }
        return normalized;
    }

    private String normalizeVisibility(String visibility) {
        String normalized = StringUtils.hasText(visibility)
            ? visibility.trim().toUpperCase()
            : DEFAULT_VISIBILITY;
        if (!ALLOWED_VISIBILITIES.contains(normalized)) {
            throw new ApiException("INVALID_PROJECT_VISIBILITY", "项目可见性不正确", 400);
        }
        return normalized;
    }

    private void ensureSlugAvailable(String slug, Long currentProjectId) {
        Project existing = projectMapper.selectActiveBySlug(slug);
        if (existing != null && (currentProjectId == null || !currentProjectId.equals(existing.getId()))) {
            throw slugExists();
        }
    }

    private ApiException slugExists() {
        return new ApiException("PROJECT_SLUG_EXISTS", "访问路径已存在", 400);
    }

    private Project requireOwnedActiveProject(Long ownerUserId, Long projectId) {
        Project project = projectMapper.selectById(projectId);
        if (
            project == null
                || project.isDeleted()
                || project.getOwnerUserId() == null
                || !project.getOwnerUserId().equals(ownerUserId)
        ) {
            throw new ApiException("PROJECT_NOT_FOUND", "项目不存在", 404);
        }
        return project;
    }
}
