package com.qfc.admin;

import java.util.ArrayList;
import java.util.List;

public class AdminRoleView {

    private Long id;
    private String code;
    private String name;
    private String description;
    private Boolean builtIn;
    private List<String> permissionCodes = new ArrayList<String>();

    public static AdminRoleView from(AdminRole role, List<String> permissionCodes) {
        AdminRoleView view = new AdminRoleView();
        view.setId(role.getId());
        view.setCode(role.getCode());
        view.setName(role.getName());
        view.setDescription(role.getDescription());
        view.setBuiltIn(role.getBuiltIn());
        view.setPermissionCodes(permissionCodes == null ? new ArrayList<String>() : permissionCodes);
        return view;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Boolean getBuiltIn() {
        return builtIn;
    }

    public void setBuiltIn(Boolean builtIn) {
        this.builtIn = builtIn;
    }

    public List<String> getPermissionCodes() {
        return permissionCodes;
    }

    public void setPermissionCodes(List<String> permissionCodes) {
        this.permissionCodes = permissionCodes;
    }
}
