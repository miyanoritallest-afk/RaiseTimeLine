package com.raisetimeline.backend.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.raisetimeline.backend.config.CorsConfig;
import com.raisetimeline.backend.config.SecurityConfig;
import com.raisetimeline.backend.dto.response.PagedResponse;
import com.raisetimeline.backend.dto.response.PostResponse;
import com.raisetimeline.backend.dto.response.UserResponse;
import com.raisetimeline.backend.exception.ForbiddenException;
import com.raisetimeline.backend.exception.ResourceNotFoundException;
import com.raisetimeline.backend.security.JwtAuthenticationFilter;
import com.raisetimeline.backend.security.JwtUtil;
import com.raisetimeline.backend.security.UserDetailsServiceImpl;
import com.raisetimeline.backend.service.PostService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.ResultActions;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(PostController.class)
@Import({SecurityConfig.class, CorsConfig.class, JwtAuthenticationFilter.class})
class PostControllerTest {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;

    @MockBean PostService postService;
    @MockBean JwtUtil jwtUtil;
    @MockBean UserDetailsServiceImpl userDetailsService;

    private static final String TEST_TOKEN = "test-jwt-token";
    private static final Long CURRENT_USER_ID = 1L;

    private static final UserResponse DUMMY_AUTHOR =
        new UserResponse(1L, "alice", "alice@test.com", null, null, 0L, 0L, false);

    private static final PostResponse DUMMY_POST = new PostResponse(
        1L, DUMMY_AUTHOR, "content", List.of(), 0L, 0L, false,
        LocalDateTime.now(), LocalDateTime.now());

    @BeforeEach
    void setUpAuth() {
        given(jwtUtil.validateToken(TEST_TOKEN)).willReturn(true);
        given(jwtUtil.extractUserId(TEST_TOKEN)).willReturn(CURRENT_USER_ID);
        UserDetails ud = User.withUsername("1").password("").roles("USER").build();
        given(userDetailsService.loadUserById(CURRENT_USER_ID)).willReturn(ud);
    }

    private ResultActions authenticated(MockHttpServletRequestBuilder req) throws Exception {
        return mockMvc.perform(req.header("Authorization", "Bearer " + TEST_TOKEN));
    }

    // BB（仕様確認）: 認証あり → 200
    @Test
    void getTimeline_authenticated_returns200() throws Exception {
        given(postService.getTimeline(any(), any(), any()))
            .willReturn(new PagedResponse<>(List.of(), null, false));

        authenticated(get("/api/posts"))
            .andExpect(status().isOk());
    }

    // BB（セキュリティ仕様）: 認証なし → 403
    @Test
    void getTimeline_noAuth_returns403() throws Exception {
        mockMvc.perform(get("/api/posts"))
            .andExpect(status().isForbidden());
    }

    // WB（コントローラーパス）: feed=all パラメーター → サービスに "all" 渡し確認
    @Test
    void getTimeline_feedParam_all_passedToService() throws Exception {
        given(postService.getTimeline(eq("all"), isNull(), eq(CURRENT_USER_ID)))
            .willReturn(new PagedResponse<>(List.of(), null, false));

        authenticated(get("/api/posts").param("feed", "all"))
            .andExpect(status().isOk());

        verify(postService).getTimeline(eq("all"), isNull(), eq(CURRENT_USER_ID));
    }

    // WB（コントローラーパス）: feed=following パラメーター → サービスに "following" 渡し確認
    @Test
    void getTimeline_feedParam_following_passedToService() throws Exception {
        given(postService.getTimeline(eq("following"), isNull(), eq(CURRENT_USER_ID)))
            .willReturn(new PagedResponse<>(List.of(), null, false));

        authenticated(get("/api/posts").param("feed", "following"))
            .andExpect(status().isOk());

        verify(postService).getTimeline(eq("following"), isNull(), eq(CURRENT_USER_ID));
    }

    // WB（コントローラーパス）: cursor=42 → サービスに cursor=42L 渡し確認
    @Test
    void getTimeline_withCursor_passedToService() throws Exception {
        given(postService.getTimeline(eq("all"), eq(42L), eq(CURRENT_USER_ID)))
            .willReturn(new PagedResponse<>(List.of(), null, false));

        authenticated(get("/api/posts").param("feed", "all").param("cursor", "42"))
            .andExpect(status().isOk());

        verify(postService).getTimeline(eq("all"), eq(42L), eq(CURRENT_USER_ID));
    }

    // BB同値分割（有効）: 投稿取得 → 200、body に id・content
    @Test
    void getPost_found_returns200() throws Exception {
        given(postService.getPost(eq(1L), eq(CURRENT_USER_ID))).willReturn(DUMMY_POST);

        authenticated(get("/api/posts/1"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(1L))
            .andExpect(jsonPath("$.content").value("content"));
    }

    // BB同値分割（無効）: 存在しない投稿 → 404
    @Test
    void getPost_notFound_returns404() throws Exception {
        given(postService.getPost(eq(999L), eq(CURRENT_USER_ID)))
            .willThrow(new ResourceNotFoundException("Post not found"));

        authenticated(get("/api/posts/999"))
            .andExpect(status().isNotFound());
    }

    // BB境界値（0文字）: content="" → 400
    @Test
    void createPost_blankContent_returns400() throws Exception {
        authenticated(post("/api/posts")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of("content", ""))))
            .andExpect(status().isBadRequest());
    }

    // BB境界値（最小有効=1文字）: content="a" → 201
    @Test
    void createPost_1charContent_returns201() throws Exception {
        given(postService.createPost(any(), eq(CURRENT_USER_ID))).willReturn(DUMMY_POST);

        authenticated(post("/api/posts")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of("content", "a"))))
            .andExpect(status().isCreated());
    }

    // BB境界値（最大有効=280文字）: content=280文字 → 201
    @Test
    void createPost_280charContent_returns201() throws Exception {
        given(postService.createPost(any(), eq(CURRENT_USER_ID))).willReturn(DUMMY_POST);

        String content280 = "a".repeat(280);
        authenticated(post("/api/posts")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of("content", content280))))
            .andExpect(status().isCreated());
    }

    // BB境界値（max+1=281文字）: content=281文字 → 400
    @Test
    void createPost_281charContent_returns400() throws Exception {
        String content281 = "a".repeat(281);
        authenticated(post("/api/posts")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of("content", content281))))
            .andExpect(status().isBadRequest());
    }

    // BB（セキュリティ仕様）: 認証なし投稿 → 403
    @Test
    void createPost_noAuth_returns403() throws Exception {
        mockMvc.perform(post("/api/posts")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of("content", "hello"))))
            .andExpect(status().isForbidden());
    }

    // BB同値分割（有効）: 投稿更新（投稿者）→ 200
    @Test
    void updatePost_owner_returns200() throws Exception {
        given(postService.updatePost(eq(1L), any(), eq(CURRENT_USER_ID))).willReturn(DUMMY_POST);

        authenticated(patch("/api/posts/1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of("content", "updated"))))
            .andExpect(status().isOk());
    }

    // BB同値分割（無効）: 投稿更新（非投稿者）→ 403
    @Test
    void updatePost_forbidden_returns403() throws Exception {
        given(postService.updatePost(eq(1L), any(), eq(CURRENT_USER_ID)))
            .willThrow(new ForbiddenException("Not authorized"));

        authenticated(patch("/api/posts/1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of("content", "updated"))))
            .andExpect(status().isForbidden());
    }

    // BB同値分割（無効）: 投稿更新（存在しない）→ 404
    @Test
    void updatePost_notFound_returns404() throws Exception {
        given(postService.updatePost(eq(1L), any(), eq(CURRENT_USER_ID)))
            .willThrow(new ResourceNotFoundException("Post not found"));

        authenticated(patch("/api/posts/1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of("content", "updated"))))
            .andExpect(status().isNotFound());
    }

    // BB（仕様確認）: 投稿削除（投稿者）→ 204
    @Test
    void deletePost_owner_returns204() throws Exception {
        authenticated(delete("/api/posts/1"))
            .andExpect(status().isNoContent());
    }

    // BB同値分割（無効）: 投稿削除（存在しない）→ 404
    @Test
    void deletePost_notFound_returns404() throws Exception {
        org.mockito.Mockito.doThrow(new ResourceNotFoundException("Post not found"))
            .when(postService).deletePost(eq(999L), eq(CURRENT_USER_ID));

        authenticated(delete("/api/posts/999"))
            .andExpect(status().isNotFound());
    }
}
