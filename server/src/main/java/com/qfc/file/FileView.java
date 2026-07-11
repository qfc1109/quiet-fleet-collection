package com.qfc.file;

import java.time.LocalDateTime;

public class FileView {

    private Long id;
    private Long projectId;
    private String originalName;
    private String fileExt;
    private String mimeType;
    private Long fileSize;
    private String relativePath;
    private String previewType;
    private String downloadUrl;
    private String previewUrl;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static FileView from(ProjectFile file) {
        FileView view = new FileView();
        view.setId(file.getId());
        view.setProjectId(file.getProjectId());
        view.setOriginalName(file.getOriginalName());
        view.setFileExt(file.getFileExt());
        view.setMimeType(file.getMimeType());
        view.setFileSize(file.getFileSize());
        view.setRelativePath(file.getRelativePath());
        view.setPreviewType(file.getPreviewType());
        view.setDownloadUrl("/api/public/files/" + file.getId() + "/download");
        view.setPreviewUrl("/api/public/files/" + file.getId() + "/preview");
        view.setCreatedAt(file.getCreatedAt());
        view.setUpdatedAt(file.getUpdatedAt());
        return view;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getProjectId() {
        return projectId;
    }

    public void setProjectId(Long projectId) {
        this.projectId = projectId;
    }

    public String getOriginalName() {
        return originalName;
    }

    public void setOriginalName(String originalName) {
        this.originalName = originalName;
    }

    public String getFileExt() {
        return fileExt;
    }

    public void setFileExt(String fileExt) {
        this.fileExt = fileExt;
    }

    public String getMimeType() {
        return mimeType;
    }

    public void setMimeType(String mimeType) {
        this.mimeType = mimeType;
    }

    public Long getFileSize() {
        return fileSize;
    }

    public void setFileSize(Long fileSize) {
        this.fileSize = fileSize;
    }

    public String getRelativePath() {
        return relativePath;
    }

    public void setRelativePath(String relativePath) {
        this.relativePath = relativePath;
    }

    public String getPreviewType() {
        return previewType;
    }

    public void setPreviewType(String previewType) {
        this.previewType = previewType;
    }

    public String getDownloadUrl() {
        return downloadUrl;
    }

    public void setDownloadUrl(String downloadUrl) {
        this.downloadUrl = downloadUrl;
    }

    public String getPreviewUrl() {
        return previewUrl;
    }

    public void setPreviewUrl(String previewUrl) {
        this.previewUrl = previewUrl;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
