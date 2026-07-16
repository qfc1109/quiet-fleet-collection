package com.qfc.file;

import java.util.Collections;
import java.util.Arrays;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FileControllerTest {

    @Mock
    private FileService fileService;

    private FileController controller;

    @BeforeEach
    void setUp() {
        controller = new FileController(fileService);
    }

    @Test
    void downloadPublicProjectArchiveUsesSlugAndZipHeaders() {
        FileArchive archive = new FileArchive("docs.zip", "application/zip", Collections.emptyList());
        when(fileService.openPublicProjectArchive("docs")).thenReturn(archive);

        ResponseEntity<StreamingResponseBody> response = controller.downloadPublicProjectArchive("docs");

        assertEquals("application/zip", response.getHeaders().getContentType().toString());
        assertEquals("attachment; filename*=UTF-8''docs.zip", response.getHeaders().getFirst("Content-Disposition"));
        verify(fileService).openPublicProjectArchive("docs");
    }

    @Test
    void downloadPublicSelectedProjectFilesArchiveUsesSlugAndRequestIds() {
        ProjectFileArchiveRequest archiveRequest = new ProjectFileArchiveRequest();
        archiveRequest.setFileIds(Arrays.asList(3L, 4L));
        FileArchive archive = new FileArchive("docs-selected.zip", "application/zip", Collections.emptyList());
        when(fileService.openPublicSelectedProjectFilesArchive("docs", Arrays.asList(3L, 4L))).thenReturn(archive);

        ResponseEntity<StreamingResponseBody> response =
            controller.downloadPublicSelectedProjectFilesArchive("docs", archiveRequest);

        assertEquals("application/zip", response.getHeaders().getContentType().toString());
        assertEquals("attachment; filename*=UTF-8''docs-selected.zip", response.getHeaders().getFirst("Content-Disposition"));
        verify(fileService).openPublicSelectedProjectFilesArchive("docs", Arrays.asList(3L, 4L));
    }
}
