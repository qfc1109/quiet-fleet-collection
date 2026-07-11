package com.qfc.admin;

import com.qfc.user.SiteUser;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class UserAccountView {

    private Long id;
    private String username;
    private String displayName;
    private String bio;
    private String avatarUrl;
    private String accountType;
    private String status;
    private List<String> roleCodes = new ArrayList<String>();
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static UserAccountView from(SiteUser account, List<String> roleCodes) {
        UserAccountView view = fromCommon(
            account.getId(),
            account.getUsername(),
            account.getDisplayName(),
            account.getBio(),
            account.getAvatarUrl(),
            RbacUserSessionService.ACCOUNT_TYPE_SITE_USER,
            account.getStatus(),
            account.getCreatedAt(),
            account.getUpdatedAt(),
            roleCodes
        );
        return view;
    }

    public static UserAccountView from(AdminUser account, List<String> roleCodes) {
        return fromCommon(
            account.getId(),
            account.getUsername(),
            account.getDisplayName(),
            account.getBio(),
            account.getAvatarUrl(),
            RbacUserSessionService.ACCOUNT_TYPE_ADMIN,
            account.getStatus(),
            account.getCreatedAt(),
            account.getUpdatedAt(),
            roleCodes
        );
    }

    private static UserAccountView fromCommon(
        Long id,
        String username,
        String displayName,
        String bio,
        String avatarUrl,
        String accountType,
        String status,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        List<String> roleCodes
    ) {
        UserAccountView view = new UserAccountView();
        view.setId(id);
        view.setUsername(username);
        view.setDisplayName(displayName);
        view.setBio(bio);
        view.setAvatarUrl(avatarUrl);
        view.setAccountType(accountType);
        view.setStatus(status);
        view.setRoleCodes(roleCodes == null ? new ArrayList<String>() : roleCodes);
        view.setCreatedAt(createdAt);
        view.setUpdatedAt(updatedAt);
        return view;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public String getBio() {
        return bio;
    }

    public void setBio(String bio) {
        this.bio = bio;
    }

    public String getAvatarUrl() {
        return avatarUrl;
    }

    public void setAvatarUrl(String avatarUrl) {
        this.avatarUrl = avatarUrl;
    }

    public String getAccountType() {
        return accountType;
    }

    public void setAccountType(String accountType) {
        this.accountType = accountType;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public List<String> getRoleCodes() {
        return roleCodes;
    }

    public void setRoleCodes(List<String> roleCodes) {
        this.roleCodes = roleCodes;
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
