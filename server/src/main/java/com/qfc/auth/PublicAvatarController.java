package com.qfc.auth;

import com.qfc.file.FileDownload;
import java.nio.charset.StandardCharsets;
import org.springframework.core.io.Resource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/public/avatars")
public class PublicAvatarController {

    private final AuthService authService;

    public PublicAvatarController(AuthService authService) {
        this.authService = authService;
    }

    @GetMapping("/{userId}/{filename:.+}")
    public ResponseEntity<Resource> avatar(@PathVariable Long userId, @PathVariable String filename) {
        FileDownload download = authService.openAvatar(userId, filename);
        return avatarResponse(download);
    }

    @GetMapping("/{accountType}/{userId}/{filename:.+}")
    public ResponseEntity<Resource> avatar(
        @PathVariable String accountType,
        @PathVariable Long userId,
        @PathVariable String filename
    ) {
        FileDownload download = authService.openAvatar(accountType, userId, filename);
        return avatarResponse(download);
    }

    private ResponseEntity<Resource> avatarResponse(FileDownload download) {
        String mediaType = download.getMimeType() == null || download.getMimeType().isEmpty()
            ? MediaType.APPLICATION_OCTET_STREAM_VALUE
            : download.getMimeType();
        return ResponseEntity.ok()
            .contentType(MediaType.parseMediaType(mediaType))
            .header("X-Content-Type-Options", "nosniff")
            .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.inline()
                .filename(download.getOriginalName(), StandardCharsets.UTF_8)
                .build()
                .toString())
            .body(download.getResource());
    }
}
