package com.raisetimeline.backend.controller;

import com.raisetimeline.backend.dto.response.LikeResponse;
import com.raisetimeline.backend.service.LikeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/posts/{postId}/likes")
@RequiredArgsConstructor
@Tag(name = "Likes", description = "投稿へのいいね")
public class LikeController {

    private final LikeService likeService;

    @PostMapping
    @Operation(summary = "投稿にいいねする")
    @ApiResponse(responseCode = "200", description = "いいね成功")
    @ApiResponse(responseCode = "409", description = "既にいいね済み")
    public LikeResponse like(@PathVariable Long postId,
                             @AuthenticationPrincipal UserDetails userDetails) {
        return likeService.likePost(postId, Long.parseLong(userDetails.getUsername()));
    }

    @DeleteMapping
    @Operation(summary = "いいねを取り消す")
    @ApiResponse(responseCode = "200", description = "いいね取り消し成功")
    public LikeResponse unlike(@PathVariable Long postId,
                               @AuthenticationPrincipal UserDetails userDetails) {
        return likeService.unlikePost(postId, Long.parseLong(userDetails.getUsername()));
    }
}
