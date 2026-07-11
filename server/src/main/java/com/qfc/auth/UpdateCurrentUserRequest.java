package com.qfc.auth;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;

public class UpdateCurrentUserRequest {

    @NotBlank
    @Size(max = 100)
    private String displayName;

    @Size(max = 500)
    private String bio;

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
}
