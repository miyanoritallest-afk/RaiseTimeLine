package com.raisetimeline.backend.controller;

import com.raisetimeline.backend.dto.request.LoginRequest;
import com.raisetimeline.backend.dto.request.RefreshRequest;
import com.raisetimeline.backend.dto.request.RegisterRequest;
import com.raisetimeline.backend.dto.response.AuthResponse;
import com.raisetimeline.backend.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Tag(name = "Auth", description = "認証系エンドポイント（全てパブリック）")
public class AuthController {

    private final AuthService authService;

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "ユーザー登録")
    @ApiResponse(responseCode = "201", description = "登録成功")
    @ApiResponse(responseCode = "409", description = "メールまたはユーザー名が既存")
    @SecurityRequirements
    public AuthResponse register(@Valid @RequestBody RegisterRequest req) {
        return authService.register(req);
    }

    @PostMapping("/login")
    @Operation(summary = "ログイン")
    @ApiResponse(responseCode = "200", description = "認証成功")
    @ApiResponse(responseCode = "401", description = "認証情報が不正")
    @SecurityRequirements
    public AuthResponse login(@Valid @RequestBody LoginRequest req) {
        return authService.login(req);
    }

    @PostMapping("/refresh")
    @Operation(summary = "アクセストークンの更新")
    @ApiResponse(responseCode = "200", description = "新しいトークンを発行")
    @ApiResponse(responseCode = "401", description = "リフレッシュトークンが無効または期限切れ")
    @SecurityRequirements
    public AuthResponse refresh(@Valid @RequestBody RefreshRequest req) {
        return authService.refresh(req.refreshToken());
    }

    @PostMapping("/logout")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "ログアウト（リフレッシュトークンを無効化）")
    @ApiResponse(responseCode = "204", description = "成功")
    @SecurityRequirements
    public void logout(@Valid @RequestBody RefreshRequest req) {
        authService.logout(req.refreshToken());
    }
}
