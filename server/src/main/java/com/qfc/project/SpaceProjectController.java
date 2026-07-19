package com.qfc.project;

import com.qfc.auth.LoginUser;
import com.qfc.auth.CurrentUserResolver;
import com.qfc.common.ApiResponse;
import com.qfc.file.FileArchive;
import com.qfc.file.FileService;
import com.qfc.file.FileView;
import com.qfc.file.MoveProjectFileRequest;
import com.qfc.file.ProjectFileArchiveRequest;
import java.nio.charset.StandardCharsets;
import java.util.List;
import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;
import org.springframework.boot.autoconfigure.web.servlet.MultipartProperties;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

@RestController
public class SpaceProjectController {

    private final ProjectService projectService;
    private final FileService fileService;
    private final CurrentUserResolver currentUserResolver;
    private final MultipartProperties multipartProperties;

    public SpaceProjectController(
        ProjectService projectService,
        FileService fileService,
        CurrentUserResolver currentUserResolver,
        MultipartProperties multipartProperties
    ) {
        this.projectService = projectService;
        this.fileService = fileService;
        this.currentUserResolver = currentUserResolver;
        this.multipartProperties = multipartProperties;
    }

    @GetMapping("/api/space/projects")
    public ApiResponse<List<ProjectView>> listProjects(HttpServletRequest request) {
        return ApiResponse.success(projectService.listSpaceProjects(currentSiteUser(request).getId()));
    }

    @GetMapping("/api/space/upload-limits")
    public ApiResponse<SpaceUploadLimitView> uploadLimits(HttpServletRequest request) {
        currentSiteUser(request);
        return ApiResponse.success(new SpaceUploadLimitView(multipartProperties.getMaxFileSize().toBytes()));
    }

    @PostMapping("/api/space/projects")
    public ApiResponse<ProjectView> createProject(@Valid @RequestBody ProjectCreateRequest form, HttpServletRequest request) {
        return ApiResponse.success(projectService.createSpaceProject(currentSiteUser(request).getId(), form));
    }

    @PutMapping("/api/space/projects/{projectId}")
    public ApiResponse<ProjectView> updateProject(
        @PathVariable Long projectId,
        @Valid @RequestBody ProjectUpdateRequest form,
        HttpServletRequest request
    ) {
        return ApiResponse.success(projectService.updateSpaceProject(currentSiteUser(request).getId(), projectId, form));
    }

    @DeleteMapping("/api/space/projects/{projectId}")
    public ApiResponse<Boolean> deleteProject(
        @PathVariable Long projectId,
        HttpServletRequest request
    ) {
        LoginUser current = currentSiteUser(request);
        projectService.softDeleteSpaceProject(current.getId(), current.getUsername(), projectId, clientIp(request));
        return ApiResponse.success(Boolean.TRUE);
    }

    @GetMapping("/api/space/projects/{projectId}/files")
    public ApiResponse<List<FileView>> listProjectFiles(@PathVariable Long projectId, HttpServletRequest request) {
        return ApiResponse.success(fileService.listOwnedProjectFiles(currentSiteUser(request).getId(), projectId));
    }

    @GetMapping("/api/space/projects/{projectId}/download")
    public ResponseEntity<StreamingResponseBody> downloadProjectArchive(
        @PathVariable Long projectId,
        HttpServletRequest request
    ) {
        return archiveResponse(fileService.openOwnedProjectArchive(currentSiteUser(request).getId(), projectId));
    }

    @PostMapping("/api/space/projects/{projectId}/files/archive")
    public ResponseEntity<StreamingResponseBody> downloadSelectedProjectFilesArchive(
        @PathVariable Long projectId,
        @RequestBody(required = false) ProjectFileArchiveRequest archiveRequest,
        HttpServletRequest request
    ) {
        List<Long> fileIds = archiveRequest == null ? null : archiveRequest.getFileIds();
        return archiveResponse(fileService.openOwnedSelectedProjectFilesArchive(currentSiteUser(request).getId(), projectId, fileIds));
    }

    @PostMapping(value = "/api/space/projects/{projectId}/files", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ApiResponse<FileView> uploadFile(
        @PathVariable Long projectId,
        @RequestParam("file") MultipartFile file,
        @RequestParam(value = "relativePath", required = false) String relativePath,
        HttpServletRequest request
    ) {
        return ApiResponse.success(fileService.uploadOwnedFile(currentSiteUser(request).getId(), projectId, file, relativePath));
    }

    @PutMapping("/api/space/files/{fileId}/path")
    public ApiResponse<FileView> moveFile(
        @PathVariable Long fileId,
        @RequestBody(required = false) MoveProjectFileRequest request,
        HttpServletRequest servletRequest
    ) {
        String targetDirectory = request == null ? "" : request.getTargetDirectory();
        return ApiResponse.success(fileService.moveOwnedFile(currentSiteUser(servletRequest).getId(), fileId, targetDirectory));
    }

    @DeleteMapping("/api/space/files/{fileId}")
    public ApiResponse<Boolean> deleteFile(@PathVariable Long fileId, HttpServletRequest request) {
        fileService.deleteOwnedFile(currentSiteUser(request).getId(), fileId);
        return ApiResponse.success(Boolean.TRUE);
    }

    private LoginUser currentSiteUser(HttpServletRequest request) {
        return currentUserResolver.resolveSite(request);
    }

    private String clientIp(HttpServletRequest request) {
        return request == null ? "" : request.getRemoteAddr();
    }

    private ResponseEntity<StreamingResponseBody> archiveResponse(FileArchive archive) {
        StreamingResponseBody body = outputStream -> fileService.writeArchive(archive, outputStream);
        return ResponseEntity.ok()
            .contentType(MediaType.parseMediaType(archive.getMimeType()))
            .header("X-Content-Type-Options", "nosniff")
            .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment()
                .filename(archive.getOriginalName(), StandardCharsets.UTF_8)
                .build()
                .toString())
            .body(body);
    }
}
