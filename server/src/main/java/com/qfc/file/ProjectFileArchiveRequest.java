package com.qfc.file;

import java.util.List;

public class ProjectFileArchiveRequest {

    private List<Long> fileIds;

    public List<Long> getFileIds() {
        return fileIds;
    }

    public void setFileIds(List<Long> fileIds) {
        this.fileIds = fileIds;
    }
}
