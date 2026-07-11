package com.qfc.project;

import com.qfc.common.ApiResponse;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import javax.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class ProjectController {

    private final ProjectService projectService;

    public ProjectController(ProjectService projectService) {
        this.projectService = projectService;
    }

    @GetMapping("/api/public/site")
    public ApiResponse<Map<String, String>> getSite() {
        Map<String, String> site = new LinkedHashMap<String, String>();
        site.put("name", "轻帆集");
        site.put("englishName", "Quiet Fleet Collection");
        site.put("shortName", "QFC");
        site.put("description", "个人文档与项目笔记展示网站");
        return ApiResponse.success(site);
    }

    @GetMapping("/api/public/projects")
    public ApiResponse<List<ProjectView>> listPublicProjects() {
        return ApiResponse.success(projectService.listPublicProjects());
    }

    @GetMapping("/api/public/projects/{slug}")
    public ApiResponse<ProjectView> getPublicProject(@PathVariable String slug) {
        return ApiResponse.success(projectService.getPublicProject(slug));
    }

    @GetMapping("/api/admin/projects")
    public ApiResponse<List<ProjectView>> listAdminProjects() {
        return ApiResponse.success(projectService.listAllProjects());
    }

    @PostMapping("/api/admin/projects")
    public ApiResponse<ProjectView> createProject(@Valid @RequestBody ProjectCreateRequest request) {
        return ApiResponse.success(projectService.createProject(request));
    }

    @PutMapping("/api/admin/projects/{projectId}")
    public ApiResponse<ProjectView> updateProject(
        @PathVariable Long projectId,
        @Valid @RequestBody ProjectUpdateRequest request
    ) {
        return ApiResponse.success(projectService.updateProject(projectId, request));
    }
}
