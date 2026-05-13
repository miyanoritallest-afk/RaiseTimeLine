package com.raisetimeline.backend.controller;

import com.raisetimeline.backend.dto.response.FollowResponse;
import com.raisetimeline.backend.dto.response.UserResponse;
import com.raisetimeline.backend.service.FollowService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class FollowController {

    private final FollowService followService;

    @PostMapping("/api/users/{id}/follow")
    @ResponseStatus(HttpStatus.CREATED)
    public FollowResponse follow(@PathVariable Long id,
                                 @AuthenticationPrincipal UserDetails userDetails) {
        return followService.follow(id, Long.parseLong(userDetails.getUsername()));
    }

    @DeleteMapping("/api/users/{id}/follow")
    public FollowResponse unfollow(@PathVariable Long id,
                                   @AuthenticationPrincipal UserDetails userDetails) {
        return followService.unfollow(id, Long.parseLong(userDetails.getUsername()));
    }

    @GetMapping("/api/users/{id}/followers")
    public List<UserResponse> getFollowers(@PathVariable Long id,
                                           @AuthenticationPrincipal UserDetails userDetails) {
        return followService.getFollowers(id, Long.parseLong(userDetails.getUsername()));
    }

    @GetMapping("/api/users/{id}/following")
    public List<UserResponse> getFollowing(@PathVariable Long id,
                                           @AuthenticationPrincipal UserDetails userDetails) {
        return followService.getFollowing(id, Long.parseLong(userDetails.getUsername()));
    }
}
