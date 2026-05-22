package com.raisetimeline.backend.controller;

import com.raisetimeline.backend.dto.response.FollowResponse;
import com.raisetimeline.backend.dto.response.UserResponse;
import com.raisetimeline.backend.service.FollowService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@Tag(name = "Follows", description = "フォロー/フォロワー管理")
public class FollowController {

    private final FollowService followService;

    @PostMapping("/api/users/{id}/follow")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "ユーザーをフォローする")
    @ApiResponse(responseCode = "201", description = "フォロー成功")
    @ApiResponse(responseCode = "409", description = "既にフォロー済み")
    public FollowResponse follow(@PathVariable Long id,
                                 @AuthenticationPrincipal UserDetails userDetails) {
        return followService.follow(id, Long.parseLong(userDetails.getUsername()));
    }

    @DeleteMapping("/api/users/{id}/follow")
    @Operation(summary = "フォローを解除する")
    public FollowResponse unfollow(@PathVariable Long id,
                                   @AuthenticationPrincipal UserDetails userDetails) {
        return followService.unfollow(id, Long.parseLong(userDetails.getUsername()));
    }

    @GetMapping("/api/users/{id}/followers")
    @Operation(summary = "フォロワー一覧を取得")
    public List<UserResponse> getFollowers(@PathVariable Long id,
                                           @AuthenticationPrincipal UserDetails userDetails) {
        return followService.getFollowers(id, Long.parseLong(userDetails.getUsername()));
    }

    @GetMapping("/api/users/{id}/following")
    @Operation(summary = "フォロー中ユーザー一覧を取得")
    public List<UserResponse> getFollowing(@PathVariable Long id,
                                           @AuthenticationPrincipal UserDetails userDetails) {
        return followService.getFollowing(id, Long.parseLong(userDetails.getUsername()));
    }
}
