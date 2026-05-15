package com.raisetimeline.backend.controller;

import com.raisetimeline.backend.service.StorageService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Map;

@RestController
@RequestMapping("/api/upload")
@RequiredArgsConstructor
public class UploadController {

    private final StorageService storageService;

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Map<String, String> upload(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "type", defaultValue = "posts") String type,
            @AuthenticationPrincipal UserDetails userDetails) {
        validateImageFile(file);
        try {
            String url = storageService.upload(file, type);
            return Map.of("url", url);
        } catch (IOException e) {
            throw new RuntimeException("ファイルのアップロードに失敗しました", e);
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
