package com.raisetimeline.backend.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/upload")
public class UploadController {

    @Value("${app.upload.base-dir}")
    private String uploadBaseDir;

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Map<String, String> upload(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "type", defaultValue = "posts") String type,
            @AuthenticationPrincipal UserDetails userDetails) {
        validateImageFile(file);
        try {
            Path subDir = Paths.get(uploadBaseDir, type);
            Files.createDirectories(subDir);
            String safeOriginal = Paths.get(file.getOriginalFilename() != null ? file.getOriginalFilename() : "upload").getFileName().toString();
            String filename = UUID.randomUUID() + "_" + safeOriginal;
            Path filePath = subDir.resolve(filename);
            Files.write(filePath, file.getBytes());
            return Map.of("url", "/images/" + type + "/" + filename);
        } catch (IOException e) {
            throw new RuntimeException("ファイルの保存に失敗しました", e);
        }
    }

    private void validateImageFile(MultipartFile file) {
        if (file.getSize() > 5L * 1024 * 1024) {
            throw new IllegalArgumentException("ファイルサイズは5MB以下にしてください");
        }
        String contentType = file.getContentType();
        if (contentType == null || !contentType.startsWith("image/")) {
            throw new IllegalArgumentException("画像ファイルのみアップロード可能です");
        }
    }
}
