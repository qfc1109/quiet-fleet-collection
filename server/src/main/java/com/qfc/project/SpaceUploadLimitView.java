package com.qfc.project;

public class SpaceUploadLimitView {

    private long maxFileSizeBytes;

    public SpaceUploadLimitView() {
    }

    public SpaceUploadLimitView(long maxFileSizeBytes) {
        this.maxFileSizeBytes = maxFileSizeBytes;
    }

    public long getMaxFileSizeBytes() {
        return maxFileSizeBytes;
    }

    public void setMaxFileSizeBytes(long maxFileSizeBytes) {
        this.maxFileSizeBytes = maxFileSizeBytes;
    }
}
