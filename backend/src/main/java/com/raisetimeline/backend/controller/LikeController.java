package com.raisetimeline.backend.controller;

import com.raisetimeline.backend.dto.response.LikeResponse;
import com.raisetimeline.backend.service.LikeService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/posts/{postId}/likes")
@RequiredArgsConstructor
public class LikeController {

    private final LikeService likeService;

    @PostMapping
    public LikeResponse like(@PathVariable Long postId,
                             @AuthenticationPrincipal UserDetails userDetails) {
        return likeService.likePost(postId, Long.parseLong(userDetails.getUsername()));
    }

    @DeleteMapping
    public LikeResponse unlike(@PathVariable Long postId,
                               @AuthenticationPrincipal UserDetails userDetails) {
        return likeService.unlikePost(postId, Long.parseLong(userDetails.getUsername()));
    }
}
