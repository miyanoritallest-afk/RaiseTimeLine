package com.raisetimeline.backend.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/upload")
@RequiredArgsConstructor
public class UploadController {

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Map<String, String> upload(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "type", defaultValue = "posts") String type,
            @AuthenticationPrincipal UserDetails userDetails) {
        validateImageFile(file);
        // S3統合は後続PRで実装。現時点はダミーURLを返す
        String filename = UUID.randomUUID() + "_" + file.getOriginalFilename();
        String dummyUrl = "/images/" + type + "/" + filename;
        return Map.of("url", dummyUrl);
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
