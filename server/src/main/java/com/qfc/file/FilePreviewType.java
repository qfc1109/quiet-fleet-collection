package com.qfc.file;

import java.util.Locale;

public enum FilePreviewType {
    MARKDOWN,
    PDF,
    IMAGE,
    EXCEL,
    WORD,
    DOWNLOAD_ONLY;

    public static FilePreviewType fromFileName(String fileName) {
        String ext = extensionOf(fileName);
        if ("md".equals(ext) || "markdown".equals(ext)) {
            return MARKDOWN;
        }
        if ("pdf".equals(ext)) {
            return PDF;
        }
        if ("png".equals(ext) || "jpg".equals(ext) || "jpeg".equals(ext) || "gif".equals(ext) || "webp".equals(ext)) {
            return IMAGE;
        }
        if ("xls".equals(ext) || "xlsx".equals(ext)) {
            return EXCEL;
        }
        if ("doc".equals(ext) || "docx".equals(ext)) {
            return WORD;
        }
        return DOWNLOAD_ONLY;
    }

    public static String extensionOf(String fileName) {
        if (fileName == null) {
            return "";
        }
        int index = fileName.lastIndexOf('.');
        if (index < 0 || index == fileName.length() - 1) {
            return "";
        }
        return fileName.substring(index + 1).toLowerCase(Locale.ROOT);
    }
}
