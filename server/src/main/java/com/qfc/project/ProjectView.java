package com.qfc.project;

import java.time.LocalDateTime;

public class ProjectView {

    private Long id;
    private Long ownerUserId;
    private String name;
    private String slug;
    private String description;
    private String coverUrl;
    private String visibility;
    private Integer sortOrder;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime deletedAt;
    private Long deletedByUserId;

    public static ProjectView from(Project project) {
        ProjectView view = new ProjectView();
        view.setId(project.getId());
        view.setOwnerUserId(project.getOwnerUserId());
        view.setName(project.getName());
        view.setSlug(project.getSlug());
        view.setDescription(project.getDescription());
        view.setCoverUrl(project.getCoverUrl());
        view.setVisibility(project.getVisibility());
        view.setSortOrder(project.getSortOrder());
        view.setCreatedAt(project.getCreatedAt());
        view.setUpdatedAt(project.getUpdatedAt());
        view.setDeletedAt(project.getDeletedAt());
        view.setDeletedByUserId(project.getDeletedByUserId());
        return view;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getOwnerUserId() {
        return ownerUserId;
    }

    public void setOwnerUserId(Long ownerUserId) {
        this.ownerUserId = ownerUserId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getSlug() {
        return slug;
    }

    public void setSlug(String slug) {
        this.slug = slug;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getCoverUrl() {
        return coverUrl;
    }

    public void setCoverUrl(String coverUrl) {
        this.coverUrl = coverUrl;
    }

    public String getVisibility() {
        return visibility;
    }

    public void setVisibility(String visibility) {
        this.visibility = visibility;
    }

    public Integer getSortOrder() {
        return sortOrder;
    }

    public void setSortOrder(Integer sortOrder) {
        this.sortOrder = sortOrder;
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

    public LocalDateTime getDeletedAt() {
        return deletedAt;
    }

    public void setDeletedAt(LocalDateTime deletedAt) {
        this.deletedAt = deletedAt;
    }

    public Long getDeletedByUserId() {
        return deletedByUserId;
    }

    public void setDeletedByUserId(Long deletedByUserId) {
        this.deletedByUserId = deletedByUserId;
    }
}
