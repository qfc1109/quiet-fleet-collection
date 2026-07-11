package com.qfc.file;

import com.qfc.common.ApiException;
import com.qfc.config.QfcStorageProperties;
import com.qfc.project.Project;
import com.qfc.project.ProjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.springframework.mock.web.MockMultipartFile;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FileServiceTest {

    @TempDir
    private Path tempDir;

    @Mock
    private ProjectMapper projectMapper;

    @Mock
    private ProjectFileMapper projectFileMapper;

    private FileService fileService;

    @BeforeEach
    void setUp() {
        QfcStorageProperties properties = new QfcStorageProperties();
        properties.setRoot(tempDir.toString());
        fileService = new FileService(projectMapper, projectFileMapper, properties);
    }

    @Test
    void uploadFileStoresFileAndPersistsMetadata() throws Exception {
        Project project = new Project();
        project.setId(7L);
        when(projectMapper.selectById(7L)).thenReturn(project);
        when(projectFileMapper.insert(any(ProjectFile.class))).thenAnswer(invocation -> {
            ProjectFile file = invocation.getArgument(0);
            file.setId(99L);
            return 1;
        });
        MockMultipartFile upload = new MockMultipartFile(
            "file",
            "README.md",
            "text/markdown",
            "# QFC".getBytes(StandardCharsets.UTF_8)
        );

        FileView view = fileService.uploadFile(7L, upload);

        ArgumentCaptor<ProjectFile> captor = ArgumentCaptor.forClass(ProjectFile.class);
        verify(projectFileMapper).insert(captor.capture());
        ProjectFile saved = captor.getValue();
        assertEquals(7L, saved.getProjectId());
        assertEquals("README.md", saved.getOriginalName());
        assertEquals("README.md", saved.getRelativePath());
        assertEquals("md", saved.getFileExt());
        assertEquals("MARKDOWN", saved.getPreviewType());
        assertNotNull(saved.getStoragePath());
        assertTrue(Files.exists(tempDir.resolve(saved.getStoragePath())));
        assertEquals(99L, view.getId());
        assertEquals("README.md", view.getOriginalName());
        assertEquals("README.md", view.getRelativePath());
    }

    @Test
    void uploadFileUsesServerMimeTypeFromExtensionInsteadOfClientHeader() throws Exception {
        Project project = new Project();
        project.setId(7L);
        when(projectMapper.selectById(7L)).thenReturn(project);
        when(projectFileMapper.insert(any(ProjectFile.class))).thenAnswer(invocation -> {
            ProjectFile file = invocation.getArgument(0);
            file.setId(100L);
            return 1;
        });
        MockMultipartFile upload = new MockMultipartFile(
            "file",
            "README.md",
            "text/html",
            "# QFC".getBytes(StandardCharsets.UTF_8)
        );

        fileService.uploadFile(7L, upload);

        ArgumentCaptor<ProjectFile> captor = ArgumentCaptor.forClass(ProjectFile.class);
        verify(projectFileMapper).insert(captor.capture());
        assertEquals("text/markdown", captor.getValue().getMimeType());
    }

    @Test
    void uploadWordFilesMarksThemAsWordPreview() throws Exception {
        Project project = new Project();
        project.setId(7L);
        when(projectMapper.selectById(7L)).thenReturn(project);
        when(projectFileMapper.insert(any(ProjectFile.class))).thenAnswer(invocation -> {
            ProjectFile file = invocation.getArgument(0);
            file.setId(99L);
            return 1;
        });
        MockMultipartFile docxUpload = new MockMultipartFile(
            "file",
            "部署说明.docx",
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
            "docx".getBytes(StandardCharsets.UTF_8)
        );
        MockMultipartFile docUpload = new MockMultipartFile(
            "file",
            "部署说明.doc",
            "application/msword",
            "doc".getBytes(StandardCharsets.UTF_8)
        );

        FileView docxView = fileService.uploadFile(7L, docxUpload);
        FileView docView = fileService.uploadFile(7L, docUpload);

        assertEquals("WORD", docxView.getPreviewType());
        assertEquals("WORD", docView.getPreviewType());
    }

    @Test
    void uploadFilePreservesFolderRelativePath() throws Exception {
        Project project = new Project();
        project.setId(7L);
        when(projectMapper.selectById(7L)).thenReturn(project);
        when(projectFileMapper.insert(any(ProjectFile.class))).thenAnswer(invocation -> {
            ProjectFile file = invocation.getArgument(0);
            file.setId(100L);
            return 1;
        });
        MockMultipartFile upload = new MockMultipartFile(
            "file",
            "logo.png",
            "image/png",
            "png".getBytes(StandardCharsets.UTF_8)
        );

        FileView view = fileService.uploadFile(7L, upload, "docs/assets/logo.png");

        ArgumentCaptor<ProjectFile> captor = ArgumentCaptor.forClass(ProjectFile.class);
        verify(projectFileMapper).insert(captor.capture());
        ProjectFile saved = captor.getValue();
        assertEquals("logo.png", saved.getOriginalName());
        assertEquals("docs/assets/logo.png", saved.getRelativePath());
        assertEquals("projects/7/docs/assets/logo.png", saved.getStoragePath());
        assertTrue(Files.exists(tempDir.resolve("projects/7/docs/assets/logo.png")));
        assertEquals("docs/assets/logo.png", view.getRelativePath());
    }

    @Test
    void uploadFileRejectsUnsafeRelativePath() {
        Project project = new Project();
        project.setId(7L);
        when(projectMapper.selectById(7L)).thenReturn(project);
        MockMultipartFile upload = new MockMultipartFile(
            "file",
            "README.md",
            "text/markdown",
            "# QFC".getBytes(StandardCharsets.UTF_8)
        );

        ApiException exception = assertThrows(ApiException.class, () -> fileService.uploadFile(7L, upload, "../README.md"));

        assertEquals("INVALID_FILE_PATH", exception.getCode());
    }

    @Test
    void uploadOwnedFileRejectsProjectOwnedByAnotherSiteUser() {
        Project project = new Project();
        project.setId(7L);
        project.setOwnerUserId(9L);
        when(projectMapper.selectById(7L)).thenReturn(project);
        MockMultipartFile upload = new MockMultipartFile(
            "file",
            "README.md",
            "text/markdown",
            "# QFC".getBytes(StandardCharsets.UTF_8)
        );

        ApiException exception = assertThrows(
            ApiException.class,
            () -> fileService.uploadOwnedFile(5L, 7L, upload, "README.md")
        );

        assertEquals("PROJECT_NOT_FOUND", exception.getCode());
        verify(projectFileMapper, never()).insert(any(ProjectFile.class));
    }

    @Test
    void uploadFileUpdatesExistingFileWithSameRelativePath() throws Exception {
        Project project = new Project();
        project.setId(7L);
        when(projectMapper.selectById(7L)).thenReturn(project);
        ProjectFile existing = new ProjectFile();
        existing.setId(88L);
        existing.setProjectId(7L);
        existing.setOriginalName("README.md");
        existing.setStoragePath("projects/7/docs/README.md");
        existing.setRelativePath("docs/README.md");
        existing.setCreatedAt(LocalDateTime.of(2026, 7, 1, 12, 0));
        when(projectFileMapper.selectByProjectIdAndRelativePath(7L, "docs/README.md")).thenReturn(existing);
        MockMultipartFile upload = new MockMultipartFile(
            "file",
            "README.md",
            "text/markdown",
            "# Updated".getBytes(StandardCharsets.UTF_8)
        );

        FileView view = fileService.uploadFile(7L, upload, "docs/README.md");

        ArgumentCaptor<ProjectFile> captor = ArgumentCaptor.forClass(ProjectFile.class);
        verify(projectFileMapper).updateById(captor.capture());
        verify(projectFileMapper, never()).insert(any(ProjectFile.class));
        ProjectFile saved = captor.getValue();
        assertEquals(88L, saved.getId());
        assertEquals(LocalDateTime.of(2026, 7, 1, 12, 0), saved.getCreatedAt());
        assertEquals("docs/README.md", saved.getRelativePath());
        assertEquals(88L, view.getId());
        assertTrue(Files.exists(tempDir.resolve("projects/7/docs/README.md")));
    }

    @Test
    void moveFileToDirectoryUpdatesPathAndMovesStoredFile() throws Exception {
        Path stored = tempDir.resolve("projects/7/README.md");
        Files.createDirectories(stored.getParent());
        Files.write(stored, "# QFC".getBytes(StandardCharsets.UTF_8));
        ProjectFile file = new ProjectFile();
        file.setId(3L);
        file.setProjectId(7L);
        file.setOriginalName("README.md");
        file.setStoredName("README.md");
        file.setFileExt("md");
        file.setMimeType("text/markdown");
        file.setFileSize(5L);
        file.setStoragePath("projects/7/README.md");
        file.setRelativePath("README.md");
        file.setPreviewType("MARKDOWN");
        file.setCreatedAt(LocalDateTime.of(2026, 7, 1, 12, 0));
        file.setUpdatedAt(LocalDateTime.of(2026, 7, 1, 12, 0));
        when(projectFileMapper.selectById(3L)).thenReturn(file);
        when(projectFileMapper.selectByProjectIdAndRelativePath(7L, "docs/README.md")).thenReturn(null);

        FileView view = fileService.moveFile(3L, "docs");

        ArgumentCaptor<ProjectFile> captor = ArgumentCaptor.forClass(ProjectFile.class);
        verify(projectFileMapper).updateById(captor.capture());
        ProjectFile moved = captor.getValue();
        assertEquals(3L, moved.getId());
        assertEquals("docs/README.md", moved.getRelativePath());
        assertEquals("projects/7/docs/README.md", moved.getStoragePath());
        assertTrue(Files.exists(tempDir.resolve("projects/7/docs/README.md")));
        assertTrue(Files.notExists(stored));
        assertEquals("docs/README.md", view.getRelativePath());
    }

    @Test
    void moveFileToRootDirectoryKeepsBaseName() throws Exception {
        Path stored = tempDir.resolve("projects/7/docs/README.md");
        Files.createDirectories(stored.getParent());
        Files.write(stored, "# QFC".getBytes(StandardCharsets.UTF_8));
        ProjectFile file = new ProjectFile();
        file.setId(3L);
        file.setProjectId(7L);
        file.setOriginalName("README.md");
        file.setStoredName("README.md");
        file.setFileExt("md");
        file.setMimeType("text/markdown");
        file.setFileSize(5L);
        file.setStoragePath("projects/7/docs/README.md");
        file.setRelativePath("docs/README.md");
        file.setPreviewType("MARKDOWN");
        when(projectFileMapper.selectById(3L)).thenReturn(file);
        when(projectFileMapper.selectByProjectIdAndRelativePath(7L, "README.md")).thenReturn(null);

        FileView view = fileService.moveFile(3L, "");

        ArgumentCaptor<ProjectFile> captor = ArgumentCaptor.forClass(ProjectFile.class);
        verify(projectFileMapper).updateById(captor.capture());
        ProjectFile moved = captor.getValue();
        assertEquals("README.md", moved.getRelativePath());
        assertEquals("projects/7/README.md", moved.getStoragePath());
        assertTrue(Files.exists(tempDir.resolve("projects/7/README.md")));
        assertTrue(Files.notExists(stored));
        assertEquals("README.md", view.getRelativePath());
    }

    @Test
    void moveFileRejectsExistingTargetPath() throws Exception {
        ProjectFile file = new ProjectFile();
        file.setId(3L);
        file.setProjectId(7L);
        file.setOriginalName("README.md");
        file.setStoragePath("projects/7/README.md");
        file.setRelativePath("README.md");
        ProjectFile existing = new ProjectFile();
        existing.setId(4L);
        existing.setProjectId(7L);
        existing.setRelativePath("docs/README.md");
        when(projectFileMapper.selectById(3L)).thenReturn(file);
        when(projectFileMapper.selectByProjectIdAndRelativePath(7L, "docs/README.md")).thenReturn(existing);

        ApiException exception = assertThrows(ApiException.class, () -> fileService.moveFile(3L, "docs"));

        assertEquals("FILE_PATH_EXISTS", exception.getCode());
        verify(projectFileMapper, never()).updateById(any(ProjectFile.class));
    }

    @Test
    void listPublicProjectFilesUsesProjectSlug() {
        Project project = new Project();
        project.setId(7L);
        when(projectMapper.selectPublicBySlug("docs")).thenReturn(project);
        ProjectFile file = new ProjectFile();
        file.setId(3L);
        file.setProjectId(7L);
        file.setOriginalName("README.md");
        file.setStoredName("stored.md");
        file.setFileExt("md");
        file.setMimeType("text/markdown");
        file.setFileSize(12L);
        file.setStoragePath("projects/7/stored.md");
        file.setRelativePath("README.md");
        file.setPreviewType("MARKDOWN");
        file.setCreatedAt(LocalDateTime.of(2026, 7, 1, 12, 0));
        file.setUpdatedAt(LocalDateTime.of(2026, 7, 1, 12, 0));
        when(projectFileMapper.selectByProjectId(7L)).thenReturn(Arrays.asList(file));

        List<FileView> files = fileService.listPublicProjectFiles("docs");

        assertEquals(1, files.size());
        assertEquals(3L, files.get(0).getId());
        assertEquals("README.md", files.get(0).getOriginalName());
        assertEquals("MARKDOWN", files.get(0).getPreviewType());
    }

    @Test
    void previewMarkdownReadsStoredContent() throws Exception {
        Path stored = tempDir.resolve("projects/7/readme.md");
        Files.createDirectories(stored.getParent());
        Files.write(stored, "# Hello".getBytes(StandardCharsets.UTF_8));
        ProjectFile file = new ProjectFile();
        file.setId(3L);
        file.setProjectId(7L);
        file.setOriginalName("README.md");
        file.setStoragePath("projects/7/readme.md");
        file.setRelativePath("README.md");
        file.setPreviewType("MARKDOWN");
        when(projectFileMapper.selectById(3L)).thenReturn(file);

        FilePreview preview = fileService.previewFile(3L);

        assertEquals("MARKDOWN", preview.getPreviewType());
        assertEquals("# Hello", preview.getContent());
    }

    @Test
    void previewDocxReadsStoredTextContent() throws Exception {
        Path stored = tempDir.resolve("projects/7/deploy.docx");
        Files.createDirectories(stored.getParent());
        try (XWPFDocument document = new XWPFDocument()) {
            XWPFParagraph title = document.createParagraph();
            title.createRun().setText("本地部署后台和配置");
            XWPFParagraph body = document.createParagraph();
            body.createRun().setText("先启动后端，再启动前端。");
            document.write(Files.newOutputStream(stored));
        }
        ProjectFile file = new ProjectFile();
        file.setId(3L);
        file.setProjectId(7L);
        file.setOriginalName("deploy.docx");
        file.setFileExt("docx");
        file.setStoragePath("projects/7/deploy.docx");
        file.setRelativePath("deploy.docx");
        file.setPreviewType("WORD");
        when(projectFileMapper.selectById(3L)).thenReturn(file);

        FilePreview preview = fileService.previewFile(3L);

        assertEquals("WORD", preview.getPreviewType());
        assertTrue(preview.getContent().contains("本地部署后台和配置"));
        assertTrue(preview.getContent().contains("先启动后端，再启动前端。"));
    }

    @Test
    void previewMarkdownRewritesRelativeImageLinksFromMarkdownDirectory() throws Exception {
        Path stored = tempDir.resolve("projects/7/docs/README.md");
        Files.createDirectories(stored.getParent());
        Files.write(stored, "![Logo](images/logo.png)\n![Remote](https://example.com/logo.png)".getBytes(StandardCharsets.UTF_8));
        ProjectFile file = new ProjectFile();
        file.setId(3L);
        file.setProjectId(7L);
        file.setOriginalName("README.md");
        file.setStoragePath("projects/7/docs/README.md");
        file.setRelativePath("docs/README.md");
        file.setPreviewType("MARKDOWN");
        when(projectFileMapper.selectById(3L)).thenReturn(file);

        FilePreview preview = fileService.previewFile(3L);

        assertTrue(preview.getContent().contains("![Logo](/api/public/files/3/assets?path=docs%2Fimages%2Flogo.png)"));
        assertTrue(preview.getContent().contains("![Remote](https://example.com/logo.png)"));
    }

    @Test
    void previewMarkdownRewritesHtmlImageSrcFromMarkdownDirectory() throws Exception {
        Path stored = tempDir.resolve("projects/7/docs/README.md");
        Files.createDirectories(stored.getParent());
        Files.write(
            stored,
            "<img src=\"images/logo.png\" alt=\"Logo\" style=\"zoom:50%;\" />\n<img src=\"https://example.com/logo.png\" alt=\"Remote\" />"
                .getBytes(StandardCharsets.UTF_8)
        );
        ProjectFile file = new ProjectFile();
        file.setId(3L);
        file.setProjectId(7L);
        file.setOriginalName("README.md");
        file.setStoragePath("projects/7/docs/README.md");
        file.setRelativePath("docs/README.md");
        file.setPreviewType("MARKDOWN");
        when(projectFileMapper.selectById(3L)).thenReturn(file);

        FilePreview preview = fileService.previewFile(3L);

        assertTrue(preview.getContent().contains("<img src=\"/api/public/files/3/assets?path=docs%2Fimages%2Flogo.png\" alt=\"Logo\" style=\"zoom:50%;\" />"));
        assertTrue(preview.getContent().contains("<img src=\"https://example.com/logo.png\" alt=\"Remote\" />"));
    }

    @Test
    void openAssetUsesFileRelativePathInsideSameProject() throws Exception {
        Path stored = tempDir.resolve("projects/7/docs/images/logo.png");
        Files.createDirectories(stored.getParent());
        Files.write(stored, "png".getBytes(StandardCharsets.UTF_8));
        ProjectFile markdown = new ProjectFile();
        markdown.setId(3L);
        markdown.setProjectId(7L);
        markdown.setOriginalName("README.md");
        markdown.setStoragePath("projects/7/docs/README.md");
        markdown.setRelativePath("docs/README.md");
        markdown.setPreviewType("MARKDOWN");
        ProjectFile asset = new ProjectFile();
        asset.setId(4L);
        asset.setProjectId(7L);
        asset.setOriginalName("logo.png");
        asset.setMimeType("image/png");
        asset.setStoragePath("projects/7/docs/images/logo.png");
        asset.setRelativePath("docs/images/logo.png");
        when(projectFileMapper.selectById(3L)).thenReturn(markdown);
        when(projectFileMapper.selectByProjectIdAndRelativePath(7L, "docs/images/logo.png")).thenReturn(asset);

        FileDownload download = fileService.openAsset(3L, "docs/images/logo.png");

        assertEquals("logo.png", download.getOriginalName());
        assertTrue(download.getResource().exists());
    }

    @Test
    void previewRejectsMissingFile() {
        when(projectFileMapper.selectById(404L)).thenReturn(null);

        ApiException exception = assertThrows(ApiException.class, () -> fileService.previewFile(404L));

        assertEquals("FILE_NOT_FOUND", exception.getCode());
    }
}
