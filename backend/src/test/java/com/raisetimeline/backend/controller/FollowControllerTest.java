package com.raisetimeline.backend.controller;

import com.raisetimeline.backend.config.CorsConfig;
import com.raisetimeline.backend.config.SecurityConfig;
import com.raisetimeline.backend.dto.response.FollowResponse;
import com.raisetimeline.backend.dto.response.UserResponse;
import com.raisetimeline.backend.security.JwtAuthenticationFilter;
import com.raisetimeline.backend.security.JwtUtil;
import com.raisetimeline.backend.security.UserDetailsServiceImpl;
import com.raisetimeline.backend.service.FollowService;
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

import java.util.List;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(FollowController.class)
@Import({SecurityConfig.class, CorsConfig.class, JwtAuthenticationFilter.class})
class FollowControllerTest {

    @Autowired MockMvc mockMvc;

    @MockBean FollowService followService;
    @MockBean JwtUtil jwtUtil;
    @MockBean UserDetailsServiceImpl userDetailsService;

    private static final String TEST_TOKEN = "test-jwt-token";
    private static final Long CURRENT_USER_ID = 1L;

    private static final FollowResponse FOLLOWING_RESPONSE = new FollowResponse(2L, 1L, 0L, true);
    private static final FollowResponse UNFOLLOWED_RESPONSE = new FollowResponse(2L, 0L, 0L, false);

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

    // BB（仕様確認）: フォロー → 201
    @Test
    void follow_success_returns201() throws Exception {
        given(followService.follow(eq(2L), eq(CURRENT_USER_ID))).willReturn(FOLLOWING_RESPONSE);

        authenticated(post("/api/users/2/follow"))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.isFollowing").value(true));
    }

    // BB同値分割（無効）: 自己フォロー → 400（IllegalArgumentException → 400）
    @Test
    void follow_selfFollow_returns400() throws Exception {
        given(followService.follow(eq(CURRENT_USER_ID), eq(CURRENT_USER_ID)))
            .willThrow(new IllegalArgumentException("自分自身をフォローできません"));

        authenticated(post("/api/users/1/follow"))
            .andExpect(status().isBadRequest());
    }

    // BB（仕様確認）: フォロー解除 → 200
    @Test
    void unfollow_success_returns200() throws Exception {
        given(followService.unfollow(eq(2L), eq(CURRENT_USER_ID))).willReturn(UNFOLLOWED_RESPONSE);

        authenticated(delete("/api/users/2/follow"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.isFollowing").value(false));
    }

    // BB（仕様確認）: フォロワー一覧取得 → 200
    @Test
    void getFollowers_returns200() throws Exception {
        UserResponse follower = new UserResponse(3L, "carol", "carol@test.com", null, null, 0L, 0L, false);
        given(followService.getFollowers(eq(2L), eq(CURRENT_USER_ID))).willReturn(List.of(follower));

        authenticated(get("/api/users/2/followers"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$").isArray())
            .andExpect(jsonPath("$[0].username").value("carol"));
    }

    // BB（仕様確認）: フォロー中ユーザー一覧取得 → 200
    @Test
    void getFollowing_returns200() throws Exception {
        given(followService.getFollowing(eq(2L), eq(CURRENT_USER_ID))).willReturn(List.of());

        authenticated(get("/api/users/2/following"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$").isArray());
    }
}
