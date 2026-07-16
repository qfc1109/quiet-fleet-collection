package com.qfc.file;

import com.qfc.common.ApiResponse;
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.springframework.core.io.Resource;
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
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

@RestController
@RequestMapping("/api")
public class FileController {

    private final FileService fileService;

    public FileController(FileService fileService) {
        this.fileService = fileService;
    }

    @GetMapping("/public/projects/{slug}/files")
    public ApiResponse<List<FileView>> listPublicProjectFiles(@PathVariable String slug) {
        return ApiResponse.success(fileService.listPublicProjectFiles(slug));
    }

    @GetMapping("/public/projects/{slug}/download")
    public ResponseEntity<StreamingResponseBody> downloadPublicProjectArchive(@PathVariable String slug) {
        return archiveResponse(fileService.openPublicProjectArchive(slug));
    }

    @PostMapping("/public/projects/{slug}/files/archive")
    public ResponseEntity<StreamingResponseBody> downloadPublicSelectedProjectFilesArchive(
        @PathVariable String slug,
        @RequestBody(required = false) ProjectFileArchiveRequest archiveRequest
    ) {
        List<Long> fileIds = archiveRequest == null ? null : archiveRequest.getFileIds();
        return archiveResponse(fileService.openPublicSelectedProjectFilesArchive(slug, fileIds));
    }

    @GetMapping("/public/files/{fileId}/preview")
    public ApiResponse<FilePreview> previewFile(@PathVariable Long fileId) {
        return ApiResponse.success(fileService.previewFile(fileId));
    }

    @GetMapping("/public/files/{fileId}/download")
    public ResponseEntity<Resource> downloadFile(@PathVariable Long fileId) {
        FileDownload download = fileService.downloadFile(fileId);
        String mediaType = download.getMimeType() == null || download.getMimeType().isEmpty()
            ? MediaType.APPLICATION_OCTET_STREAM_VALUE
            : download.getMimeType();
        return ResponseEntity.ok()
            .contentType(MediaType.parseMediaType(mediaType))
            .header("X-Content-Type-Options", "nosniff")
            .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment()
                .filename(download.getOriginalName(), StandardCharsets.UTF_8)
                .build()
                .toString())
            .body(download.getResource());
    }

    @GetMapping("/public/files/{fileId}/content")
    public ResponseEntity<Resource> inlineFile(@PathVariable Long fileId) {
        FileDownload download = fileService.downloadFile(fileId);
        return inlineResource(download);
    }

    @GetMapping("/public/files/{fileId}/assets")
    public ResponseEntity<Resource> assetFile(@PathVariable Long fileId, @RequestParam("path") String path) {
        FileDownload download = fileService.openAsset(fileId, path);
        return inlineResource(download);
    }

    private ResponseEntity<Resource> inlineResource(FileDownload download) {
        String mediaType = download.getMimeType() == null || download.getMimeType().isEmpty()
            ? MediaType.APPLICATION_OCTET_STREAM_VALUE
            : download.getMimeType();
        return ResponseEntity.ok()
            .contentType(MediaType.parseMediaType(mediaType))
            .header("X-Content-Type-Options", "nosniff")
            .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.inline()
                .filename(download.getOriginalName(), StandardCharsets.UTF_8)
                .build()
                .toString())
            .body(download.getResource());
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

    @GetMapping("/admin/projects/{projectId}/files")
    public ApiResponse<List<FileView>> listAdminProjectFiles(@PathVariable Long projectId) {
        return ApiResponse.success(fileService.listProjectFiles(projectId));
    }

    @PostMapping(value = "/admin/projects/{projectId}/files", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ApiResponse<FileView> uploadFile(
        @PathVariable Long projectId,
        @RequestParam("file") MultipartFile file,
        @RequestParam(value = "relativePath", required = false) String relativePath
    ) {
        return ApiResponse.success(fileService.uploadFile(projectId, file, relativePath));
    }

    @DeleteMapping("/admin/files/{fileId}")
    public ApiResponse<Boolean> deleteFile(@PathVariable Long fileId) {
        fileService.deleteFile(fileId);
        return ApiResponse.success(Boolean.TRUE);
    }

    @PutMapping("/admin/files/{fileId}/path")
    public ApiResponse<FileView> moveFile(
        @PathVariable Long fileId,
        @RequestBody(required = false) MoveProjectFileRequest request
    ) {
        String targetDirectory = request == null ? "" : request.getTargetDirectory();
        return ApiResponse.success(fileService.moveFile(fileId, targetDirectory));
    }
}
