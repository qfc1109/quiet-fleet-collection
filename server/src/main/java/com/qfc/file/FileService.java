package com.qfc.file;

import com.qfc.common.ApiException;
import com.qfc.config.QfcStorageProperties;
import com.qfc.project.Project;
import com.qfc.project.ProjectMapper;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.poi.hwpf.HWPFDocument;
import org.apache.poi.hwpf.extractor.WordExtractor;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.apache.poi.xwpf.extractor.XWPFWordExtractor;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

@Service
public class FileService {

    private static final int MAX_EXCEL_ROWS = 50;
    private static final int MAX_EXCEL_COLUMNS = 30;
    private static final Pattern MARKDOWN_IMAGE_LINK = Pattern.compile("!\\[([^\\]]*)\\]\\(([^\\s)]+)(\\s+\"[^\"]*\")?\\)");
    private static final Pattern HTML_IMAGE_SRC = Pattern.compile("(?i)(<img\\b[^>]*?\\bsrc\\s*=\\s*)([\"'])([^\"']*)(\\2)");
    private static final Pattern URI_SCHEME = Pattern.compile("^[a-zA-Z][a-zA-Z0-9+.-]*:.*");

    private final ProjectMapper projectMapper;
    private final ProjectFileMapper projectFileMapper;
    private final QfcStorageProperties storageProperties;

    public FileService(ProjectMapper projectMapper, ProjectFileMapper projectFileMapper, QfcStorageProperties storageProperties) {
        this.projectMapper = projectMapper;
        this.projectFileMapper = projectFileMapper;
        this.storageProperties = storageProperties;
    }

    public FileView uploadFile(Long projectId, MultipartFile multipartFile) {
        return uploadFile(projectId, multipartFile, null);
    }

    public FileView uploadFile(Long projectId, MultipartFile multipartFile, String requestedRelativePath) {
        Project project = projectMapper.selectById(projectId);
        if (project == null || project.isDeleted()) {
            throw new ApiException("PROJECT_NOT_FOUND", "项目不存在", 404);
        }
        return uploadFile(project, multipartFile, requestedRelativePath);
    }

    public FileView uploadOwnedFile(Long ownerUserId, Long projectId, MultipartFile multipartFile, String requestedRelativePath) {
        Project project = requireOwnedProject(ownerUserId, projectId);
        return uploadFile(project, multipartFile, requestedRelativePath);
    }

    private FileView uploadFile(Project project, MultipartFile multipartFile, String requestedRelativePath) {
        if (multipartFile == null || multipartFile.isEmpty()) {
            throw new ApiException("EMPTY_FILE", "上传文件不能为空", 400);
        }

        Long projectId = project.getId();
        String originalName = safeOriginalName(multipartFile.getOriginalFilename());
        String relativePath = normalizeUploadRelativePath(StringUtils.hasText(requestedRelativePath) ? requestedRelativePath : originalName);
        String storagePath = "projects/" + projectId + "/" + relativePath;
        ProjectFile existingFile = projectFileMapper.selectByProjectIdAndRelativePath(projectId, relativePath);
        Path target = resolveStoragePath(storagePath);
        try {
            Files.createDirectories(target.getParent());
            multipartFile.transferTo(target.toFile());
        } catch (IOException exception) {
            throw new ApiException("FILE_SAVE_FAILED", "文件保存失败", 500);
        }

        LocalDateTime now = LocalDateTime.now();
        ProjectFile file = new ProjectFile();
        if (existingFile != null) {
            file.setId(existingFile.getId());
        }
        file.setProjectId(projectId);
        file.setOriginalName(originalName);
        file.setStoredName(originalName);
        file.setFileExt(FilePreviewType.extensionOf(originalName));
        file.setMimeType(serverMimeType(originalName));
        file.setFileSize(multipartFile.getSize());
        file.setStoragePath(storagePath);
        file.setRelativePath(relativePath);
        file.setPreviewType(FilePreviewType.fromFileName(originalName).name());
        file.setCreatedAt(existingFile == null || existingFile.getCreatedAt() == null ? now : existingFile.getCreatedAt());
        file.setUpdatedAt(now);
        if (existingFile == null) {
            projectFileMapper.insert(file);
        } else {
            projectFileMapper.updateById(file);
        }
        return FileView.from(file);
    }

    public List<FileView> listPublicProjectFiles(String slug) {
        Project project = projectMapper.selectPublicBySlug(slug);
        if (project == null) {
            throw new ApiException("PROJECT_NOT_FOUND", "项目不存在", 404);
        }
        return listProjectFiles(project.getId());
    }

    public List<FileView> listProjectFiles(Long projectId) {
        List<ProjectFile> files = projectFileMapper.selectByProjectId(projectId);
        List<FileView> views = new ArrayList<FileView>();
        for (ProjectFile file : files) {
            views.add(FileView.from(file));
        }
        return views;
    }

    public List<FileView> listOwnedProjectFiles(Long ownerUserId, Long projectId) {
        requireOwnedProject(ownerUserId, projectId);
        return listProjectFiles(projectId);
    }

    public FilePreview previewFile(Long fileId) {
        ProjectFile file = findFile(fileId);
        FilePreview preview = basePreview(file);
        FilePreviewType previewType = FilePreviewType.valueOf(file.getPreviewType());
        if (previewType == FilePreviewType.MARKDOWN) {
            preview.setContent(rewriteMarkdownAssetLinks(readText(file), file));
            return preview;
        }
        if (previewType == FilePreviewType.EXCEL) {
            preview.setExcel(readExcel(file));
            return preview;
        }
        if (previewType == FilePreviewType.WORD) {
            preview.setContent(readWord(file));
            return preview;
        }
        return preview;
    }

    public FileDownload downloadFile(Long fileId) {
        ProjectFile file = findFile(fileId);
        return openStoredFile(file);
    }

    public FileDownload openAsset(Long fileId, String relativePath) {
        ProjectFile baseFile = findFile(fileId);
        String normalizedPath = normalizeUploadRelativePath(relativePath);
        ProjectFile assetFile = projectFileMapper.selectByProjectIdAndRelativePath(baseFile.getProjectId(), normalizedPath);
        if (assetFile == null) {
            throw new ApiException("FILE_NOT_FOUND", "文件不存在", 404);
        }
        return openStoredFile(assetFile);
    }

    private FileDownload openStoredFile(ProjectFile file) {
        Path path = resolveStoragePath(file.getStoragePath());
        if (!Files.exists(path)) {
            throw new ApiException("FILE_NOT_FOUND", "文件不存在", 404);
        }
        try {
            Resource resource = new UrlResource(path.toUri());
            return new FileDownload(file.getOriginalName(), file.getMimeType(), resource);
        } catch (MalformedURLException exception) {
            throw new ApiException("FILE_READ_FAILED", "文件读取失败", 500);
        }
    }

    public void deleteFile(Long fileId) {
        ProjectFile file = findFile(fileId);
        deleteFile(file);
    }

    public void deleteOwnedFile(Long ownerUserId, Long fileId) {
        ProjectFile file = findFile(fileId);
        requireOwnedProject(ownerUserId, file.getProjectId());
        deleteFile(file);
    }

    private void deleteFile(ProjectFile file) {
        Path path = resolveStoragePath(file.getStoragePath());
        try {
            Files.deleteIfExists(path);
        } catch (IOException exception) {
            throw new ApiException("FILE_DELETE_FAILED", "文件删除失败", 500);
        }
        projectFileMapper.deleteById(file.getId());
    }

    @Transactional(transactionManager = "siteTransactionManager")
    public FileView moveFile(Long fileId, String targetDirectory) {
        ProjectFile file = findFile(fileId);
        return moveFile(file, targetDirectory);
    }

    @Transactional(transactionManager = "siteTransactionManager")
    public FileView moveOwnedFile(Long ownerUserId, Long fileId, String targetDirectory) {
        ProjectFile file = findFile(fileId);
        requireOwnedProject(ownerUserId, file.getProjectId());
        return moveFile(file, targetDirectory);
    }

    private FileView moveFile(ProjectFile file, String targetDirectory) {
        String targetRelativePath = moveTargetRelativePath(file, targetDirectory);
        ProjectFile existingFile = projectFileMapper.selectByProjectIdAndRelativePath(file.getProjectId(), targetRelativePath);
        if (existingFile != null && !existingFile.getId().equals(file.getId())) {
            throw new ApiException("FILE_PATH_EXISTS", "目标目录下已存在同名文件", 400);
        }
        if (targetRelativePath.equals(file.getRelativePath())) {
            return FileView.from(file);
        }

        String targetStoragePath = "projects/" + file.getProjectId() + "/" + targetRelativePath;
        Path source = resolveStoragePath(file.getStoragePath());
        Path target = resolveStoragePath(targetStoragePath);
        if (!Files.exists(source)) {
            throw new ApiException("FILE_NOT_FOUND", "文件不存在", 404);
        }
        if (!source.equals(target) && Files.exists(target)) {
            throw new ApiException("FILE_PATH_EXISTS", "目标目录下已存在同名文件", 400);
        }
        try {
            Files.createDirectories(target.getParent());
            Files.move(source, target, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException exception) {
            throw new ApiException("FILE_MOVE_FAILED", "文件移动失败", 500);
        }

        file.setStoragePath(targetStoragePath);
        file.setRelativePath(targetRelativePath);
        file.setUpdatedAt(LocalDateTime.now());
        projectFileMapper.updateById(file);
        return FileView.from(file);
    }

    private Project requireOwnedProject(Long ownerUserId, Long projectId) {
        Project project = projectMapper.selectById(projectId);
        if (project == null || project.isDeleted() || project.getOwnerUserId() == null || !project.getOwnerUserId().equals(ownerUserId)) {
            throw new ApiException("PROJECT_NOT_FOUND", "项目不存在", 404);
        }
        return project;
    }

    private FilePreview basePreview(ProjectFile file) {
        FilePreview preview = new FilePreview();
        preview.setFileId(file.getId());
        preview.setOriginalName(file.getOriginalName());
        preview.setRelativePath(file.getRelativePath());
        preview.setPreviewType(file.getPreviewType());
        preview.setDownloadUrl("/api/public/files/" + file.getId() + "/download");
        preview.setStreamUrl("/api/public/files/" + file.getId() + "/content");
        return preview;
    }

    private String readText(ProjectFile file) {
        Path path = resolveStoragePath(file.getStoragePath());
        try {
            byte[] bytes = Files.readAllBytes(path);
            return new String(bytes, StandardCharsets.UTF_8);
        } catch (IOException exception) {
            throw new ApiException("FILE_READ_FAILED", "文件读取失败", 500);
        }
    }

    private ExcelPreview readExcel(ProjectFile file) {
        Path path = resolveStoragePath(file.getStoragePath());
        try (InputStream inputStream = Files.newInputStream(path);
             Workbook workbook = WorkbookFactory.create(inputStream)) {
            ExcelPreview preview = new ExcelPreview();
            if (workbook.getNumberOfSheets() == 0) {
                return preview;
            }
            Sheet sheet = workbook.getSheetAt(0);
            DataFormatter formatter = new DataFormatter();
            List<List<String>> rows = new ArrayList<List<String>>();
            int lastRow = Math.min(sheet.getLastRowNum(), MAX_EXCEL_ROWS - 1);
            for (int rowIndex = 0; rowIndex <= lastRow; rowIndex++) {
                Row row = sheet.getRow(rowIndex);
                List<String> values = new ArrayList<String>();
                if (row != null) {
                    short lastCellNum = row.getLastCellNum();
                    int lastColumn = Math.min(lastCellNum < 0 ? 0 : lastCellNum, MAX_EXCEL_COLUMNS);
                    for (int cellIndex = 0; cellIndex < lastColumn; cellIndex++) {
                        Cell cell = row.getCell(cellIndex);
                        values.add(cell == null ? "" : formatter.formatCellValue(cell));
                    }
                }
                rows.add(values);
            }
            preview.setRows(rows);
            return preview;
        } catch (Exception exception) {
            throw new ApiException("FILE_PREVIEW_FAILED", "Excel 预览失败", 500);
        }
    }

    private String readWord(ProjectFile file) {
        String ext = StringUtils.hasText(file.getFileExt())
            ? file.getFileExt().toLowerCase(Locale.ROOT)
            : FilePreviewType.extensionOf(file.getOriginalName());
        if ("doc".equals(ext)) {
            return readLegacyWord(file);
        }
        return readOpenXmlWord(file);
    }

    private String readOpenXmlWord(ProjectFile file) {
        Path path = resolveStoragePath(file.getStoragePath());
        try (InputStream inputStream = Files.newInputStream(path);
             XWPFDocument document = new XWPFDocument(inputStream);
             XWPFWordExtractor extractor = new XWPFWordExtractor(document)) {
            return extractor.getText();
        } catch (Exception exception) {
            throw new ApiException("FILE_PREVIEW_FAILED", "Word 预览失败", 500);
        }
    }

    private String readLegacyWord(ProjectFile file) {
        Path path = resolveStoragePath(file.getStoragePath());
        try (InputStream inputStream = Files.newInputStream(path);
             HWPFDocument document = new HWPFDocument(inputStream);
             WordExtractor extractor = new WordExtractor(document)) {
            return extractor.getText();
        } catch (Exception exception) {
            throw new ApiException("FILE_PREVIEW_FAILED", "Word 预览失败", 500);
        }
    }

    private ProjectFile findFile(Long fileId) {
        ProjectFile file = projectFileMapper.selectById(fileId);
        if (file == null) {
            throw new ApiException("FILE_NOT_FOUND", "文件不存在", 404);
        }
        return file;
    }

    private Path resolveStoragePath(String relativePath) {
        Path root = Paths.get(storageProperties.getRoot()).toAbsolutePath().normalize();
        Path path = root.resolve(relativePath).normalize();
        if (!path.startsWith(root)) {
            throw new ApiException("INVALID_FILE_PATH", "文件路径不正确", 400);
        }
        return path;
    }

    private String rewriteMarkdownAssetLinks(String content, ProjectFile file) {
        if (!StringUtils.hasText(content)) {
            return content;
        }
        Matcher matcher = MARKDOWN_IMAGE_LINK.matcher(content);
        StringBuffer buffer = new StringBuffer();
        while (matcher.find()) {
            String title = matcher.group(3) == null ? "" : matcher.group(3);
            String target = matcher.group(2);
            String rewrittenTarget = rewriteMarkdownAssetTarget(file, target);
            String replacement = "![" + matcher.group(1) + "](" + rewrittenTarget + title + ")";
            matcher.appendReplacement(buffer, Matcher.quoteReplacement(replacement));
        }
        matcher.appendTail(buffer);
        return rewriteHtmlImageSrc(buffer.toString(), file);
    }

    private String rewriteHtmlImageSrc(String content, ProjectFile file) {
        Matcher matcher = HTML_IMAGE_SRC.matcher(content);
        StringBuffer buffer = new StringBuffer();
        while (matcher.find()) {
            String target = matcher.group(3);
            String rewrittenTarget = rewriteMarkdownAssetTarget(file, target);
            String replacement = matcher.group(1) + matcher.group(2) + rewrittenTarget + matcher.group(2);
            matcher.appendReplacement(buffer, Matcher.quoteReplacement(replacement));
        }
        matcher.appendTail(buffer);
        return buffer.toString();
    }

    private String rewriteMarkdownAssetTarget(ProjectFile file, String target) {
        if (!StringUtils.hasText(target) || isExternalAssetLink(target)) {
            return target;
        }
        String relativePath = StringUtils.hasText(file.getRelativePath()) ? file.getRelativePath() : file.getOriginalName();
        String parentPath = parentPathOf(relativePath);
        String candidate = StringUtils.hasText(parentPath) ? parentPath + "/" + target : target;
        try {
            String normalizedPath = normalizeLinkedRelativePath(candidate);
            return "/api/public/files/" + file.getId() + "/assets?path=" + encodeQueryParam(normalizedPath);
        } catch (ApiException exception) {
            if ("INVALID_FILE_PATH".equals(exception.getCode())) {
                return target;
            }
            throw exception;
        }
    }

    private boolean isExternalAssetLink(String target) {
        String lowerTarget = target.toLowerCase();
        return target.startsWith("/")
            || target.startsWith("#")
            || lowerTarget.startsWith("http://")
            || lowerTarget.startsWith("https://")
            || lowerTarget.startsWith("data:")
            || URI_SCHEME.matcher(target).matches();
    }

    private String parentPathOf(String relativePath) {
        if (!StringUtils.hasText(relativePath)) {
            return "";
        }
        int slashIndex = relativePath.lastIndexOf('/');
        return slashIndex < 0 ? "" : relativePath.substring(0, slashIndex);
    }

    private String moveTargetRelativePath(ProjectFile file, String targetDirectory) {
        String baseName = baseNameOf(StringUtils.hasText(file.getRelativePath()) ? file.getRelativePath() : file.getOriginalName());
        if (!StringUtils.hasText(targetDirectory)) {
            return baseName;
        }
        String directory = normalizeUploadRelativePath(targetDirectory);
        return directory + "/" + baseName;
    }

    private String baseNameOf(String relativePath) {
        String normalizedPath = normalizeUploadRelativePath(relativePath);
        int slashIndex = normalizedPath.lastIndexOf('/');
        return slashIndex < 0 ? normalizedPath : normalizedPath.substring(slashIndex + 1);
    }

    private String normalizeUploadRelativePath(String relativePath) {
        return normalizeProjectRelativePath(relativePath, false);
    }

    private String normalizeLinkedRelativePath(String relativePath) {
        return normalizeProjectRelativePath(relativePath, true);
    }

    private String normalizeProjectRelativePath(String relativePath, boolean allowDotSegments) {
        if (!StringUtils.hasText(relativePath)) {
            throw new ApiException("INVALID_FILE_PATH", "文件路径不正确", 400);
        }
        String path = relativePath.replace('\\', '/').trim();
        if (!StringUtils.hasText(path) || path.startsWith("/") || path.contains("\0")) {
            throw new ApiException("INVALID_FILE_PATH", "文件路径不正确", 400);
        }
        List<String> segments = new ArrayList<String>();
        for (String segment : path.split("/")) {
            if (!StringUtils.hasText(segment) || ".".equals(segment)) {
                if (allowDotSegments) {
                    continue;
                }
                throw new ApiException("INVALID_FILE_PATH", "文件路径不正确", 400);
            }
            if ("..".equals(segment)) {
                if (allowDotSegments && !segments.isEmpty()) {
                    segments.remove(segments.size() - 1);
                    continue;
                }
                throw new ApiException("INVALID_FILE_PATH", "文件路径不正确", 400);
            }
            if (segment.contains(":") || segment.matches(".*[\\\\*?\"<>|].*")) {
                throw new ApiException("INVALID_FILE_PATH", "文件路径不正确", 400);
            }
            segments.add(segment);
        }
        if (segments.isEmpty()) {
            throw new ApiException("INVALID_FILE_PATH", "文件路径不正确", 400);
        }
        return String.join("/", segments);
    }

    private String encodeQueryParam(String value) {
        try {
            return URLEncoder.encode(value, StandardCharsets.UTF_8.name());
        } catch (UnsupportedEncodingException exception) {
            throw new ApiException("FILE_PATH_ENCODE_FAILED", "文件路径编码失败", 500);
        }
    }

    private String safeOriginalName(String originalFilename) {
        String filename = StringUtils.hasText(originalFilename) ? originalFilename : "file";
        String onlyName = Paths.get(filename).getFileName().toString();
        return onlyName.replaceAll("[\\\\/:*?\"<>|]", "_");
    }

    private String serverMimeType(String fileName) {
        String extension = FilePreviewType.extensionOf(fileName);
        if ("md".equals(extension) || "markdown".equals(extension)) {
            return "text/markdown";
        }
        if ("pdf".equals(extension)) {
            return "application/pdf";
        }
        if ("png".equals(extension)) {
            return "image/png";
        }
        if ("jpg".equals(extension) || "jpeg".equals(extension)) {
            return "image/jpeg";
        }
        if ("gif".equals(extension)) {
            return "image/gif";
        }
        if ("webp".equals(extension)) {
            return "image/webp";
        }
        if ("xls".equals(extension)) {
            return "application/vnd.ms-excel";
        }
        if ("xlsx".equals(extension)) {
            return "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
        }
        if ("doc".equals(extension)) {
            return "application/msword";
        }
        if ("docx".equals(extension)) {
            return "application/vnd.openxmlformats-officedocument.wordprocessingml.document";
        }
        if ("txt".equals(extension)) {
            return "text/plain";
        }
        if ("csv".equals(extension)) {
            return "text/csv";
        }
        if ("json".equals(extension)) {
            return "application/json";
        }
        return "application/octet-stream";
    }
}
