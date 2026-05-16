package com.raisetimeline.backend.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.raisetimeline.backend.config.CorsConfig;
import com.raisetimeline.backend.config.SecurityConfig;
import com.raisetimeline.backend.dto.response.AuthResponse;
import com.raisetimeline.backend.dto.response.UserResponse;
import com.raisetimeline.backend.exception.DuplicateResourceException;
import com.raisetimeline.backend.exception.InvalidTokenException;
import com.raisetimeline.backend.security.JwtAuthenticationFilter;
import com.raisetimeline.backend.security.JwtUtil;
import com.raisetimeline.backend.security.UserDetailsServiceImpl;
import com.raisetimeline.backend.service.AuthService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AuthController.class)
@Import({SecurityConfig.class, CorsConfig.class, JwtAuthenticationFilter.class})
class AuthControllerTest {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;

    @MockBean AuthService authService;
    @MockBean JwtUtil jwtUtil;
    @MockBean UserDetailsServiceImpl userDetailsService;

    private static final UserResponse DUMMY_USER =
        new UserResponse(1L, "alice", "alice@test.com", null, null, 0L, 0L, false);

    private static final AuthResponse DUMMY_AUTH =
        new AuthResponse("access-token", "refresh-token", 900L, DUMMY_USER);

    // BB同値分割（有効）: 全フィールド有効 → 201
    @Test
    void register_validRequest_returns201() throws Exception {
        given(authService.register(any())).willReturn(DUMMY_AUTH);

        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(
                    Map.of("email", "alice@test.com", "username", "alice", "password", "password1"))))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.accessToken").value("access-token"));
    }

    // BB同値分割（無効Email）: メール形式エラー → 400
    @Test
    void register_invalidEmailFormat_returns400() throws Exception {
        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(
                    Map.of("email", "not-an-email", "username", "alice", "password", "password1"))))
            .andExpect(status().isBadRequest());
    }

    // BB境界値（8-1=7文字）: パスワード7文字 → 400
    @Test
    void register_passwordTooShort_7chars_returns400() throws Exception {
        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(
                    Map.of("email", "alice@test.com", "username", "alice", "password", "1234567"))))
            .andExpect(status().isBadRequest());
    }

    // BB境界値（最小有効=8文字）: パスワード8文字 → 201
    @Test
    void register_passwordExactly8chars_returns201() throws Exception {
        given(authService.register(any())).willReturn(DUMMY_AUTH);

        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(
                    Map.of("email", "alice@test.com", "username", "alice", "password", "12345678"))))
            .andExpect(status().isCreated());
    }

    // BB同値分割（無効）: ユーザー名空文字 → 400
    @Test
    void register_blankUsername_returns400() throws Exception {
        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(
                    Map.of("email", "alice@test.com", "username", "", "password", "password1"))))
            .andExpect(status().isBadRequest());
    }

    // BB同値分割（サービス例外）: 重複メール → 409
    @Test
    void register_duplicateEmail_returns409() throws Exception {
        given(authService.register(any())).willThrow(new DuplicateResourceException("Email already exists"));

        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(
                    Map.of("email", "alice@test.com", "username", "alice", "password", "password1"))))
            .andExpect(status().isConflict());
    }

    // BB同値分割（有効）: 正しい認証情報 → 200
    @Test
    void login_validCredentials_returns200() throws Exception {
        given(authService.login(any())).willReturn(DUMMY_AUTH);

        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(
                    Map.of("email", "alice@test.com", "password", "password1"))))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.accessToken").exists());
    }

    // BB同値分割（無効）: 不正認証情報 → 401
    @Test
    void login_badCredentials_returns401() throws Exception {
        given(authService.login(any())).willThrow(new BadCredentialsException("Bad credentials"));

        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(
                    Map.of("email", "alice@test.com", "password", "wrongpass"))))
            .andExpect(status().isUnauthorized());
    }

    // BB同値分割（有効）: 有効リフレッシュトークン → 200
    @Test
    void refresh_validToken_returns200() throws Exception {
        given(authService.refresh(any())).willReturn(DUMMY_AUTH);

        mockMvc.perform(post("/api/auth/refresh")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(
                    Map.of("refreshToken", "valid-refresh-token"))))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.accessToken").exists());
    }

    // BB同値分割（無効）: 無効リフレッシュトークン → 401
    @Test
    void refresh_invalidToken_returns401() throws Exception {
        given(authService.refresh(any())).willThrow(new InvalidTokenException("Invalid token"));

        mockMvc.perform(post("/api/auth/refresh")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(
                    Map.of("refreshToken", "invalid-token"))))
            .andExpect(status().isUnauthorized());
    }

    // BB（仕様確認）: ログアウト → 204、body なし
    @Test
    void logout_returns204() throws Exception {
        mockMvc.perform(post("/api/auth/logout")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(
                    Map.of("refreshToken", "some-refresh-token"))))
            .andExpect(status().isNoContent());
    }
}
