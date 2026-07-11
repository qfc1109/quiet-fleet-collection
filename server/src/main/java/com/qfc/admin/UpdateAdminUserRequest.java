package com.qfc.admin;

import java.util.ArrayList;
import java.util.List;

public class UpdateAdminUserRequest extends UpdateUserRequest {

    private List<String> roleCodes = new ArrayList<String>();

    public List<String> getRoleCodes() {
        return roleCodes;
    }

    public void setRoleCodes(List<String> roleCodes) {
        this.roleCodes = roleCodes;
    }
}
