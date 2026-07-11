package com.qfc.auth;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class LoginUser implements Serializable {

    private Long id;
    private String username;
    private String displayName;
    private String bio;
    private String avatarUrl;
    private String accountType;
    private String status;
    private List<String> roleCodes = new ArrayList<String>();
    private List<String> permissionCodes = new ArrayList<String>();

    public LoginUser() {
    }

    public LoginUser(Long id, String username, String displayName) {
        this.id = id;
        this.username = username;
        this.displayName = displayName;
        this.bio = "";
        this.avatarUrl = "";
    }

    public LoginUser(
        Long id,
        String username,
        String displayName,
        String accountType,
        String status,
        List<String> roleCodes,
        List<String> permissionCodes
    ) {
        this.id = id;
        this.username = username;
        this.displayName = displayName;
        this.bio = "";
        this.avatarUrl = "";
        this.accountType = accountType;
        this.status = status;
        this.roleCodes = roleCodes == null ? new ArrayList<String>() : roleCodes;
        this.permissionCodes = permissionCodes == null ? new ArrayList<String>() : permissionCodes;
    }

    public LoginUser(
        Long id,
        String username,
        String displayName,
        String bio,
        String avatarUrl,
        String accountType,
        String status,
        List<String> roleCodes,
        List<String> permissionCodes
    ) {
        this.id = id;
        this.username = username;
        this.displayName = displayName;
        this.bio = bio == null ? "" : bio;
        this.avatarUrl = avatarUrl == null ? "" : avatarUrl;
        this.accountType = accountType;
        this.status = status;
        this.roleCodes = roleCodes == null ? new ArrayList<String>() : roleCodes;
        this.permissionCodes = permissionCodes == null ? new ArrayList<String>() : permissionCodes;
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

    public List<String> getPermissionCodes() {
        return permissionCodes;
    }

    public void setPermissionCodes(List<String> permissionCodes) {
        this.permissionCodes = permissionCodes;
    }
}
