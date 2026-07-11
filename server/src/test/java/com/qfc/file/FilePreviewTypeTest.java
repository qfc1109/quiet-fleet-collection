package com.qfc.file;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class FilePreviewTypeTest {

    @Test
    void detectsPreviewTypeFromFileName() {
        assertEquals(FilePreviewType.MARKDOWN, FilePreviewType.fromFileName("readme.md"));
        assertEquals(FilePreviewType.PDF, FilePreviewType.fromFileName("manual.pdf"));
        assertEquals(FilePreviewType.IMAGE, FilePreviewType.fromFileName("cover.PNG"));
        assertEquals(FilePreviewType.EXCEL, FilePreviewType.fromFileName("plan.xlsx"));
        assertEquals(FilePreviewType.WORD, FilePreviewType.fromFileName("deploy.docx"));
        assertEquals(FilePreviewType.WORD, FilePreviewType.fromFileName("guide.doc"));
        assertEquals(FilePreviewType.DOWNLOAD_ONLY, FilePreviewType.fromFileName("archive.zip"));
    }
}
