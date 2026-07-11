package com.qfc.file;

import org.springframework.core.io.Resource;

public class FileDownload {

    private String originalName;
    private String mimeType;
    private Resource resource;

    public FileDownload(String originalName, String mimeType, Resource resource) {
        this.originalName = originalName;
        this.mimeType = mimeType;
        this.resource = resource;
    }

    public String getOriginalName() {
        return originalName;
    }

    public String getMimeType() {
        return mimeType;
    }

    public Resource getResource() {
        return resource;
    }
}
