package com.raisetimeline.backend.controller;

import com.raisetimeline.backend.config.CorsConfig;
import com.raisetimeline.backend.config.SecurityConfig;
import com.raisetimeline.backend.security.JwtAuthenticationFilter;
import com.raisetimeline.backend.security.JwtUtil;
import com.raisetimeline.backend.security.UserDetailsServiceImpl;
import com.raisetimeline.backend.service.StorageService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(UploadController.class)
@Import({SecurityConfig.class, CorsConfig.class, JwtAuthenticationFilter.class})
class UploadControllerTest {

    @Autowired MockMvc mockMvc;

    @MockBean StorageService storageService;
    @MockBean JwtUtil jwtUtil;
    @MockBean UserDetailsServiceImpl userDetailsService;

    private static final String TEST_TOKEN = "test-jwt-token";
    private static final Long CURRENT_USER_ID = 1L;

    @BeforeEach
    void setUpAuth() {
        given(jwtUtil.validateToken(TEST_TOKEN)).willReturn(true);
        given(jwtUtil.extractUserId(TEST_TOKEN)).willReturn(CURRENT_USER_ID);
        UserDetails ud = User.withUsername("1").password("").roles("USER").build();
        given(userDetailsService.loadUserById(CURRENT_USER_ID)).willReturn(ud);
    }

    // BB同値分割（有効）: 有効な画像ファイル → 200 + URL
    @Test
    void upload_validImage_returns200WithUrl() throws Exception {
        given(storageService.upload(any(), any())).willReturn("http://localhost/test/posts/image.jpg");

        MockMultipartFile file = new MockMultipartFile(
            "file", "image.jpg", MediaType.IMAGE_JPEG_VALUE, new byte[1024]);

        mockMvc.perform(multipart("/api/upload")
                .file(file)
                .header("Authorization", "Bearer " + TEST_TOKEN))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.url").value("http://localhost/test/posts/image.jpg"));
    }

    // BB（セキュリティ仕様）: 認証なし → 403
    @Test
    void upload_noAuth_returns403() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
            "file", "image.jpg", MediaType.IMAGE_JPEG_VALUE, new byte[1024]);

        mockMvc.perform(multipart("/api/upload").file(file))
            .andExpect(status().isForbidden());
    }

    // BB同値分割（無効）: 画像以外のファイル → 400
    @Test
    void upload_nonImageFile_returns400() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
            "file", "document.pdf", MediaType.APPLICATION_PDF_VALUE, new byte[1024]);

        mockMvc.perform(multipart("/api/upload")
                .file(file)
                .header("Authorization", "Bearer " + TEST_TOKEN))
            .andExpect(status().isBadRequest());
    }

    // BB境界値（5MB超）: ファイルサイズ超過 → 400
    @Test
    void upload_fileTooLarge_returns400() throws Exception {
        // 5MB + 1 byte
        byte[] largeContent = new byte[5 * 1024 * 1024 + 1];
        MockMultipartFile file = new MockMultipartFile(
            "file", "large.jpg", MediaType.IMAGE_JPEG_VALUE, largeContent);

        mockMvc.perform(multipart("/api/upload")
                .file(file)
                .header("Authorization", "Bearer " + TEST_TOKEN))
            .andExpect(status().isBadRequest());
    }
}
