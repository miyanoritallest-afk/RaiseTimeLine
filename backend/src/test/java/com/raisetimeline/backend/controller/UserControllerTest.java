package com.raisetimeline.backend.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.raisetimeline.backend.config.CorsConfig;
import com.raisetimeline.backend.config.SecurityConfig;
import com.raisetimeline.backend.dto.response.UserResponse;
import com.raisetimeline.backend.exception.ForbiddenException;
import com.raisetimeline.backend.exception.ResourceNotFoundException;
import com.raisetimeline.backend.security.JwtAuthenticationFilter;
import com.raisetimeline.backend.security.JwtUtil;
import com.raisetimeline.backend.security.UserDetailsServiceImpl;
import com.raisetimeline.backend.service.CommentService;
import com.raisetimeline.backend.service.PostService;
import com.raisetimeline.backend.service.UserService;
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

import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(UserController.class)
@Import({SecurityConfig.class, CorsConfig.class, JwtAuthenticationFilter.class})
class UserControllerTest {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;

    @MockBean UserService userService;
    @MockBean PostService postService;
    @MockBean CommentService commentService;
    @MockBean JwtUtil jwtUtil;
    @MockBean UserDetailsServiceImpl userDetailsService;

    private static final String TEST_TOKEN = "test-jwt-token";
    private static final Long CURRENT_USER_ID = 1L;

    private static final UserResponse DUMMY_USER =
        new UserResponse(2L, "bob", "bob@test.com", null, null, 0L, 0L, false);

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

    // BB（仕様確認）: ユーザー検索 → 200
    @Test
    void searchUsers_returns200() throws Exception {
        given(userService.searchUsers(eq("bob"), eq(CURRENT_USER_ID))).willReturn(List.of(DUMMY_USER));

        authenticated(get("/api/users").param("q", "bob"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$").isArray())
            .andExpect(jsonPath("$[0].username").value("bob"));
    }

    // BB同値分割（有効）: ユーザー取得 → 200
    @Test
    void getUser_found_returns200() throws Exception {
        given(userService.getUserProfile(eq(2L), eq(CURRENT_USER_ID))).willReturn(DUMMY_USER);

        authenticated(get("/api/users/2"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(2L));
    }

    // BB同値分割（無効）: 存在しないユーザー → 404
    @Test
    void getUser_notFound_returns404() throws Exception {
        given(userService.getUserProfile(eq(999L), eq(CURRENT_USER_ID)))
            .willThrow(new ResourceNotFoundException("User not found"));

        authenticated(get("/api/users/999"))
            .andExpect(status().isNotFound());
    }

    // BB（仕様確認）: ユーザー投稿一覧 → 200
    @Test
    void getUserPosts_returns200() throws Exception {
        given(postService.getPostsByUser(eq(2L), eq(CURRENT_USER_ID))).willReturn(List.of());

        authenticated(get("/api/users/2/posts"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$").isArray());
    }

    // BB（仕様確認）: いいね投稿一覧 → 200
    @Test
    void getLikedPosts_returns200() throws Exception {
        given(postService.getLikedPostsByUser(eq(2L), eq(CURRENT_USER_ID))).willReturn(List.of());

        authenticated(get("/api/users/2/liked-posts"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$").isArray());
    }

    // BB（仕様確認）: プロフィール更新（本人）→ 200
    @Test
    void updateProfile_self_returns200() throws Exception {
        UserResponse updated = new UserResponse(1L, "alice_new", "alice@test.com", "bio", null, 0L, 0L, false);
        given(userService.updateProfile(eq(CURRENT_USER_ID), any(), eq(CURRENT_USER_ID))).willReturn(updated);

        authenticated(patch("/api/users/1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of("username", "alice_new", "bio", "bio"))))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.username").value("alice_new"));
    }

    // BB同値分割（無効）: プロフィール更新（他ユーザー）→ 403
    @Test
    void updateProfile_otherUser_returns403() throws Exception {
        given(userService.updateProfile(eq(2L), any(), eq(CURRENT_USER_ID)))
            .willThrow(new ForbiddenException("Cannot update other's profile"));

        authenticated(patch("/api/users/2")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of("username", "hacker"))))
            .andExpect(status().isForbidden());
    }

    // BB（仕様確認）: ユーザーコメント一覧 → 200
    @Test
    void getUserComments_returns200() throws Exception {
        given(commentService.getCommentsByUser(eq(2L))).willReturn(List.of());

        authenticated(get("/api/users/2/comments"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$").isArray());
    }
}
