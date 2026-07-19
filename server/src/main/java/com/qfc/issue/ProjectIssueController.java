package com.qfc.issue;

import com.qfc.auth.LoginUser;
import com.qfc.auth.CurrentUserResolver;
import com.qfc.common.ApiResponse;
import java.util.List;
import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class ProjectIssueController {

    private final ProjectIssueService projectIssueService;
    private final CurrentUserResolver currentUserResolver;

    public ProjectIssueController(ProjectIssueService projectIssueService, CurrentUserResolver currentUserResolver) {
        this.projectIssueService = projectIssueService;
        this.currentUserResolver = currentUserResolver;
    }

    @PostMapping("/api/public/projects/{slug}/issues")
    public ApiResponse<ProjectIssueView> createIssue(
        @PathVariable String slug,
        @Valid @RequestBody ProjectIssueCreateRequest request,
        HttpServletRequest servletRequest
    ) {
        LoginUser current = currentSiteUser(servletRequest);
        return ApiResponse.success(projectIssueService.createIssue(slug, current.getId(), request));
    }

    @GetMapping("/api/space/projects/{projectId}/issues")
    public ApiResponse<List<ProjectIssueView>> listSpaceProjectIssues(
        @PathVariable Long projectId,
        HttpServletRequest request
    ) {
        LoginUser current = currentSiteUser(request);
        return ApiResponse.success(projectIssueService.listSpaceProjectIssues(current.getId(), projectId));
    }

    private LoginUser currentSiteUser(HttpServletRequest request) {
        return currentUserResolver.resolveSite(request);
    }
}
