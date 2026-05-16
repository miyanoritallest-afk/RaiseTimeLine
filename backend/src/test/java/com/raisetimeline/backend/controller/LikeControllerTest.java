package com.raisetimeline.backend.controller;

import com.raisetimeline.backend.config.CorsConfig;
import com.raisetimeline.backend.config.SecurityConfig;
import com.raisetimeline.backend.dto.response.LikeResponse;
import com.raisetimeline.backend.exception.ResourceNotFoundException;
import com.raisetimeline.backend.security.JwtAuthenticationFilter;
import com.raisetimeline.backend.security.JwtUtil;
import com.raisetimeline.backend.security.UserDetailsServiceImpl;
import com.raisetimeline.backend.service.LikeService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.ResultActions;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(LikeController.class)
@Import({SecurityConfig.class, CorsConfig.class, JwtAuthenticationFilter.class})
class LikeControllerTest {

    @Autowired MockMvc mockMvc;

    @MockBean LikeService likeService;
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

    private ResultActions authenticated(MockHttpServletRequestBuilder req) throws Exception {
        return mockMvc.perform(req.header("Authorization", "Bearer " + TEST_TOKEN));
    }

    // BB同値分割（有効）: いいね → 200
    @Test
    void like_authenticated_returns200() throws Exception {
        given(likeService.likePost(eq(1L), eq(CURRENT_USER_ID)))
            .willReturn(new LikeResponse(1L, 1L, true));

        authenticated(post("/api/posts/1/likes"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.likedByMe").value(true))
            .andExpect(jsonPath("$.likeCount").value(1L));
    }

    // BB（セキュリティ仕様）: 認証なし → 403
    @Test
    void like_noAuth_returns403() throws Exception {
        mockMvc.perform(post("/api/posts/1/likes"))
            .andExpect(status().isForbidden());
    }

    // BB同値分割（有効）: いいね取り消し → 200
    @Test
    void unlike_authenticated_returns200() throws Exception {
        given(likeService.unlikePost(eq(1L), eq(CURRENT_USER_ID)))
            .willReturn(new LikeResponse(1L, 0L, false));

        authenticated(delete("/api/posts/1/likes"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.likedByMe").value(false));
    }

    // BB同値分割（無効）: 存在しない投稿にいいね → 404
    @Test
    void like_postNotFound_returns404() throws Exception {
        given(likeService.likePost(eq(999L), eq(CURRENT_USER_ID)))
            .willThrow(new ResourceNotFoundException("Post not found"));

        authenticated(post("/api/posts/999/likes"))
            .andExpect(status().isNotFound());
    }

    // BB同値分割（無効）: 存在しない投稿のいいね取り消し → 404
    @Test
    void unlike_postNotFound_returns404() throws Exception {
        given(likeService.unlikePost(eq(999L), eq(CURRENT_USER_ID)))
            .willThrow(new ResourceNotFoundException("Post not found"));

        authenticated(delete("/api/posts/999/likes"))
            .andExpect(status().isNotFound());
    }
}
