package com.qfc.auth;

import com.qfc.common.ApiException;
import com.qfc.admin.AdminUser;
import com.qfc.admin.AdminUserMapper;
import com.qfc.admin.RbacUserSessionService;
import com.qfc.config.QfcStorageProperties;
import com.qfc.file.FileDownload;
import com.qfc.user.SiteUser;
import com.qfc.user.SiteUserMapper;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

@Service
public class AuthService {

    private final SiteUserMapper siteUserMapper;
    private final AdminUserMapper adminUserMapper;
    private final PasswordEncoder passwordEncoder;
    private final RbacUserSessionService rbacUserSessionService;
    private final QfcStorageProperties storageProperties;

    public AuthService(
        SiteUserMapper siteUserMapper,
        AdminUserMapper adminUserMapper,
        PasswordEncoder passwordEncoder,
        RbacUserSessionService rbacUserSessionService,
        QfcStorageProperties storageProperties
    ) {
        this.siteUserMapper = siteUserMapper;
        this.adminUserMapper = adminUserMapper;
        this.passwordEncoder = passwordEncoder;
        this.rbacUserSessionService = rbacUserSessionService;
        this.storageProperties = storageProperties;
    }

    public LoginUser loginSite(String username, String password) {
        if (!StringUtils.hasText(username) || !StringUtils.hasText(password)) {
            throw invalidCredentials();
        }
        String normalizedUsername = username.trim();
        SiteUser siteUser = siteUserMapper.findByUsername(normalizedUsername);
        if (siteUser == null || !passwordEncoder.matches(password, siteUser.getPasswordHash())) {
            throw invalidCredentials();
        }
        ensureEnabled(siteUser.getStatus());
        return rbacUserSessionService.toLoginUser(siteUser);
    }

    public LoginUser loginAdmin(String username, String password) {
        if (!StringUtils.hasText(username) || !StringUtils.hasText(password)) {
            throw invalidCredentials();
        }
        String normalizedUsername = username.trim();
        AdminUser adminUser = adminUserMapper.findByUsername(normalizedUsername);
        if (adminUser == null || !passwordEncoder.matches(password, adminUser.getPasswordHash())) {
            throw invalidCredentials();
        }
        ensureEnabled(adminUser.getStatus());
        return rbacUserSessionService.toLoginUser(adminUser);
    }

    public LoginUser register(RegisterRequest request) {
        if (!StringUtils.hasText(request.getUsername()) || !StringUtils.hasText(request.getPassword())) {
            throw new ApiException("VALIDATION_ERROR", "账号和密码不能为空", 400);
        }
        String username = request.getUsername().trim();
        if (siteUserMapper.findByUsername(username) != null) {
            throw new ApiException("USERNAME_EXISTS", "账号已存在", 400);
        }

        LocalDateTime now = LocalDateTime.now();
        SiteUser account = new SiteUser();
        account.setUsername(username);
        account.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        account.setDisplayName(request.getDisplayName());
        account.setBio("");
        account.setAvatarUrl("");
        account.setStatus(RbacUserSessionService.STATUS_ENABLED);
        account.setCreatedAt(now);
        account.setUpdatedAt(now);
        siteUserMapper.insert(account);
        return rbacUserSessionService.toLoginUser(account);
    }

    public LoginUser currentUser(Long userId, String accountType) {
        if (RbacUserSessionService.ACCOUNT_TYPE_ADMIN.equals(accountType)) {
            return rbacUserSessionService.toLoginUser(enabledAdminAccount(userId));
        }
        return rbacUserSessionService.toLoginUser(enabledSiteAccount(userId));
    }

    public LoginUser updateCurrentUserProfile(Long userId, String accountType, UpdateCurrentUserRequest request) {
        if (request == null || !StringUtils.hasText(request.getDisplayName())) {
            throw new ApiException("VALIDATION_ERROR", "昵称不能为空", 400);
        }
        if (RbacUserSessionService.ACCOUNT_TYPE_ADMIN.equals(accountType)) {
            AdminUser account = enabledAdminAccount(userId);
            account.setDisplayName(request.getDisplayName().trim());
            account.setBio(request.getBio() == null ? "" : request.getBio().trim());
            account.setUpdatedAt(LocalDateTime.now());
            adminUserMapper.updateById(account);
            return rbacUserSessionService.toLoginUser(account);
        }
        SiteUser account = enabledSiteAccount(userId);
        account.setDisplayName(request.getDisplayName().trim());
        account.setBio(request.getBio() == null ? "" : request.getBio().trim());
        account.setUpdatedAt(LocalDateTime.now());
        siteUserMapper.updateById(account);
        return rbacUserSessionService.toLoginUser(account);
    }

    public LoginUser uploadCurrentUserAvatar(Long userId, String accountType, MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new ApiException("EMPTY_FILE", "头像文件不能为空", 400);
        }
        String extension = avatarExtension(file);
        String filename = "avatar." + extension;
        String normalizedAccountType = normalizeAccountType(accountType);
        String storagePath = "avatars/" + normalizedAccountType.toLowerCase() + "/" + userId + "/" + filename;
        Path target = resolveStoragePath(storagePath);
        try {
            Files.createDirectories(target.getParent());
            file.transferTo(target.toFile());
        } catch (IOException exception) {
            throw new ApiException("FILE_SAVE_FAILED", "头像保存失败", 500);
        }
        String avatarUrl = "/api/public/avatars/" + normalizedAccountType + "/" + userId + "/" + filename;
        if (RbacUserSessionService.ACCOUNT_TYPE_ADMIN.equals(normalizedAccountType)) {
            AdminUser account = enabledAdminAccount(userId);
            account.setAvatarUrl(avatarUrl);
            account.setUpdatedAt(LocalDateTime.now());
            adminUserMapper.updateById(account);
            return rbacUserSessionService.toLoginUser(account);
        }
        SiteUser account = enabledSiteAccount(userId);
        account.setAvatarUrl(avatarUrl);
        account.setUpdatedAt(LocalDateTime.now());
        siteUserMapper.updateById(account);
        return rbacUserSessionService.toLoginUser(account);
    }

    public FileDownload openAvatar(Long userId, String filename) {
        return openAvatar(RbacUserSessionService.ACCOUNT_TYPE_SITE_USER, userId, filename);
    }

    public FileDownload openAvatar(String accountType, Long userId, String filename) {
        if (userId == null || userId <= 0 || !StringUtils.hasText(filename)) {
            throw new ApiException("INVALID_FILE_PATH", "头像路径不正确", 400);
        }
        String normalizedAccountType = normalizeAccountType(accountType);
        String safeFilename = Paths.get(filename).getFileName().toString();
        if (!filename.equals(safeFilename) || safeFilename.contains("\0")) {
            throw new ApiException("INVALID_FILE_PATH", "头像路径不正确", 400);
        }
        Path path = resolveStoragePath("avatars/" + normalizedAccountType.toLowerCase() + "/" + userId + "/" + safeFilename);
        if (!Files.exists(path)) {
            throw new ApiException("FILE_NOT_FOUND", "头像不存在", 404);
        }
        try {
            Resource resource = new UrlResource(path.toUri());
            return new FileDownload(safeFilename, avatarMimeType(safeFilename), resource);
        } catch (MalformedURLException exception) {
            throw new ApiException("FILE_READ_FAILED", "头像读取失败", 500);
        }
    }

    private SiteUser enabledSiteAccount(Long userId) {
        SiteUser account = siteUserMapper.selectById(userId);
        if (account == null) {
            throw new ApiException("UNAUTHORIZED", "请先登录", 401);
        }
        ensureEnabled(account.getStatus());
        return account;
    }

    private AdminUser enabledAdminAccount(Long userId) {
        AdminUser account = adminUserMapper.selectById(userId);
        if (account == null) {
            throw new ApiException("UNAUTHORIZED", "请先登录", 401);
        }
        ensureEnabled(account.getStatus());
        return account;
    }

    private Path resolveStoragePath(String relativePath) {
        Path root = Paths.get(storageProperties.getRoot()).toAbsolutePath().normalize();
        Path path = root.resolve(relativePath).normalize();
        if (!path.startsWith(root)) {
            throw new ApiException("INVALID_FILE_PATH", "文件路径不正确", 400);
        }
        return path;
    }

    private String avatarExtension(MultipartFile file) {
        String originalName = file.getOriginalFilename();
        String lowerName = originalName == null ? "" : originalName.toLowerCase();
        String extension = "";
        int dotIndex = lowerName.lastIndexOf('.');
        if (dotIndex >= 0 && dotIndex + 1 < lowerName.length()) {
            extension = lowerName.substring(dotIndex + 1);
        }
        if (!isAllowedAvatarExtension(extension)) {
            String contentType = file.getContentType() == null ? "" : file.getContentType().toLowerCase();
            if ("image/png".equals(contentType)) {
                extension = "png";
            } else if ("image/jpeg".equals(contentType)) {
                extension = "jpg";
            } else if ("image/gif".equals(contentType)) {
                extension = "gif";
            } else if ("image/webp".equals(contentType)) {
                extension = "webp";
            }
        }
        if (!isAllowedAvatarExtension(extension)) {
            throw new ApiException("INVALID_AVATAR_TYPE", "头像只支持 png、jpg、jpeg、gif 或 webp", 400);
        }
        String normalizedExtension = "jpeg".equals(extension) ? "jpg" : extension;
        validateAvatarContent(file, normalizedExtension);
        return normalizedExtension;
    }

    private boolean isAllowedAvatarExtension(String extension) {
        return "png".equals(extension)
            || "jpg".equals(extension)
            || "jpeg".equals(extension)
            || "gif".equals(extension)
            || "webp".equals(extension);
    }

    private String avatarMimeType(String filename) {
        String lowerName = filename.toLowerCase();
        if (lowerName.endsWith(".png")) {
            return "image/png";
        }
        if (lowerName.endsWith(".jpg") || lowerName.endsWith(".jpeg")) {
            return "image/jpeg";
        }
        if (lowerName.endsWith(".gif")) {
            return "image/gif";
        }
        if (lowerName.endsWith(".webp")) {
            return "image/webp";
        }
        return "application/octet-stream";
    }

    private void validateAvatarContent(MultipartFile file, String extension) {
        byte[] header = readHeader(file, 12);
        boolean valid;
        if ("png".equals(extension)) {
            valid = header.length >= 8
                && header[0] == (byte) 0x89
                && header[1] == 0x50
                && header[2] == 0x4E
                && header[3] == 0x47
                && header[4] == 0x0D
                && header[5] == 0x0A
                && header[6] == 0x1A
                && header[7] == 0x0A;
        } else if ("jpg".equals(extension)) {
            valid = header.length >= 3
                && header[0] == (byte) 0xFF
                && header[1] == (byte) 0xD8
                && header[2] == (byte) 0xFF;
        } else if ("gif".equals(extension)) {
            valid = header.length >= 6
                && header[0] == 0x47
                && header[1] == 0x49
                && header[2] == 0x46
                && header[3] == 0x38
                && (header[4] == 0x37 || header[4] == 0x39)
                && header[5] == 0x61;
        } else if ("webp".equals(extension)) {
            valid = header.length >= 12
                && header[0] == 0x52
                && header[1] == 0x49
                && header[2] == 0x46
                && header[3] == 0x46
                && header[8] == 0x57
                && header[9] == 0x45
                && header[10] == 0x42
                && header[11] == 0x50;
        } else {
            valid = false;
        }
        if (!valid) {
            throw new ApiException("INVALID_AVATAR_TYPE", "头像文件内容与图片类型不匹配", 400);
        }
    }

    private byte[] readHeader(MultipartFile file, int size) {
        byte[] buffer = new byte[size];
        try (InputStream inputStream = file.getInputStream()) {
            int length = inputStream.read(buffer);
            if (length <= 0) {
                return new byte[0];
            }
            if (length == size) {
                return buffer;
            }
            byte[] actual = new byte[length];
            System.arraycopy(buffer, 0, actual, 0, length);
            return actual;
        } catch (IOException exception) {
            throw new ApiException("INVALID_AVATAR_TYPE", "头像文件读取失败", 400);
        }
    }

    private void ensureEnabled(String status) {
        if (RbacUserSessionService.STATUS_DISABLED.equals(status)) {
            throw new ApiException("ACCOUNT_DISABLED", "账号已停用", 403);
        }
    }

    private String normalizeAccountType(String accountType) {
        if (RbacUserSessionService.ACCOUNT_TYPE_ADMIN.equals(accountType)) {
            return RbacUserSessionService.ACCOUNT_TYPE_ADMIN;
        }
        if (RbacUserSessionService.ACCOUNT_TYPE_SITE_USER.equals(accountType)) {
            return RbacUserSessionService.ACCOUNT_TYPE_SITE_USER;
        }
        throw new ApiException("INVALID_ACCOUNT_TYPE", "账号类型不正确", 400);
    }

    private ApiException invalidCredentials() {
        return new ApiException("INVALID_CREDENTIALS", "账号或密码错误", 401);
    }
}
