package com.raisetimeline.backend.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.raisetimeline.backend.config.CorsConfig;
import com.raisetimeline.backend.config.SecurityConfig;
import com.raisetimeline.backend.dto.response.CommentResponse;
import com.raisetimeline.backend.dto.response.UserResponse;
import com.raisetimeline.backend.exception.ForbiddenException;
import com.raisetimeline.backend.exception.ResourceNotFoundException;
import com.raisetimeline.backend.security.JwtAuthenticationFilter;
import com.raisetimeline.backend.security.JwtUtil;
import com.raisetimeline.backend.security.UserDetailsServiceImpl;
import com.raisetimeline.backend.service.CommentService;
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
import static org.mockito.Mockito.doThrow;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(CommentController.class)
@Import({SecurityConfig.class, CorsConfig.class, JwtAuthenticationFilter.class})
class CommentControllerTest {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;

    @MockBean CommentService commentService;
    @MockBean JwtUtil jwtUtil;
    @MockBean UserDetailsServiceImpl userDetailsService;

    private static final String TEST_TOKEN = "test-jwt-token";
    private static final Long CURRENT_USER_ID = 1L;

    private static final UserResponse DUMMY_AUTHOR =
        new UserResponse(1L, "alice", "alice@test.com", null, null, 0L, 0L, false);

    private static final CommentResponse DUMMY_COMMENT = new CommentResponse(
        1L, 1L, DUMMY_AUTHOR, "comment content", LocalDateTime.now(), LocalDateTime.now());

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

    // BB（仕様確認）: コメント一覧取得 → 200、JSON配列
    @Test
    void getComments_returns200() throws Exception {
        given(commentService.getCommentsByPost(1L)).willReturn(List.of(DUMMY_COMMENT));

        authenticated(get("/api/posts/1/comments"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$").isArray())
            .andExpect(jsonPath("$[0].content").value("comment content"));
    }

    // BB同値分割（有効）: コメント作成 → 201
    @Test
    void createComment_validContent_returns201() throws Exception {
        given(commentService.createComment(eq(1L), any(), eq(CURRENT_USER_ID))).willReturn(DUMMY_COMMENT);

        authenticated(post("/api/posts/1/comments")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of("content", "valid comment"))))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.content").value("comment content"));
    }

    // BB境界値（空文字）: content="" → 400
    @Test
    void createComment_blankContent_returns400() throws Exception {
        authenticated(post("/api/posts/1/comments")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of("content", ""))))
            .andExpect(status().isBadRequest());
    }

    // BB同値分割（空白のみ）: content=" " → 400
    @Test
    void createComment_whitespaceOnly_returns400() throws Exception {
        authenticated(post("/api/posts/1/comments")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"content\":\" \"}"))
            .andExpect(status().isBadRequest());
    }

    // BB同値分割（サービス例外）: 投稿なし → 404
    @Test
    void createComment_postNotFound_returns404() throws Exception {
        given(commentService.createComment(eq(999L), any(), eq(CURRENT_USER_ID)))
            .willThrow(new ResourceNotFoundException("Post not found"));

        authenticated(post("/api/posts/999/comments")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of("content", "comment"))))
            .andExpect(status().isNotFound());
    }

    // BB（セキュリティ仕様）: 認証なし → 403
    @Test
    void createComment_noAuth_returns403() throws Exception {
        mockMvc.perform(post("/api/posts/1/comments")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of("content", "comment"))))
            .andExpect(status().isForbidden());
    }

    // BB（仕様確認）: コメント更新（投稿者）→ 200
    @Test
    void updateComment_owner_returns200() throws Exception {
        given(commentService.updateComment(eq(1L), any(), eq(CURRENT_USER_ID))).willReturn(DUMMY_COMMENT);

        authenticated(patch("/api/comments/1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of("content", "updated"))))
            .andExpect(status().isOk());
    }

    // BB（仕様確認）: コメント更新（非投稿者）→ 403
    @Test
    void updateComment_forbidden_returns403() throws Exception {
        given(commentService.updateComment(eq(1L), any(), eq(CURRENT_USER_ID)))
            .willThrow(new ForbiddenException("Not authorized"));

        authenticated(patch("/api/comments/1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of("content", "updated"))))
            .andExpect(status().isForbidden());
    }

    // BB（仕様確認）: コメント更新（存在しない）→ 404
    @Test
    void updateComment_notFound_returns404() throws Exception {
        given(commentService.updateComment(eq(999L), any(), eq(CURRENT_USER_ID)))
            .willThrow(new ResourceNotFoundException("Comment not found"));

        authenticated(patch("/api/comments/999")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of("content", "updated"))))
            .andExpect(status().isNotFound());
    }

    // BB（仕様確認）: コメント削除（投稿者）→ 204
    @Test
    void deleteComment_owner_returns204() throws Exception {
        authenticated(delete("/api/comments/1"))
            .andExpect(status().isNoContent());
    }

    // BB（仕様確認）: コメント削除（存在しない）→ 404
    @Test
    void deleteComment_notFound_returns404() throws Exception {
        doThrow(new ResourceNotFoundException("Comment not found"))
            .when(commentService).deleteComment(eq(999L), eq(CURRENT_USER_ID));

        authenticated(delete("/api/comments/999"))
            .andExpect(status().isNotFound());
    }
}
