package com.qfc.admin;

public class PermissionView {

    private Long id;
    private String code;
    private String name;
    private String module;
    private String description;

    public static PermissionView from(Permission permission) {
        PermissionView view = new PermissionView();
        view.setId(permission.getId());
        view.setCode(permission.getCode());
        view.setName(permission.getName());
        view.setModule(permission.getModule());
        view.setDescription(permission.getDescription());
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

    public String getModule() {
        return module;
    }

    public void setModule(String module) {
        this.module = module;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }
}
