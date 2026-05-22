package com.raisetimeline.backend.controller;

import com.raisetimeline.backend.service.StorageService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
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
@Tag(name = "Upload", description = "画像アップロード（S3）")
public class UploadController {

    private final StorageService storageService;

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "画像ファイルをアップロード", description = "最大 5MB。image/* のみ受け付けます。成功すると公開 URL を返します。")
    @ApiResponse(responseCode = "200", description = "アップロード成功。{\"url\": \"...\"} を返す")
    @ApiResponse(responseCode = "400", description = "ファイルサイズが 5MB 超、または画像以外のファイル")
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
