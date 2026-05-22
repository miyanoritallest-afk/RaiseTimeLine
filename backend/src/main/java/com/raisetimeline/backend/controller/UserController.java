package com.raisetimeline.backend.controller;

import com.raisetimeline.backend.dto.request.UpdateProfileRequest;
import com.raisetimeline.backend.dto.response.CommentResponse;
import com.raisetimeline.backend.dto.response.PostResponse;
import com.raisetimeline.backend.dto.response.UserResponse;
import com.raisetimeline.backend.service.CommentService;
import com.raisetimeline.backend.service.PostService;
import com.raisetimeline.backend.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
@Tag(name = "Users", description = "ユーザー検索・プロフィール取得・更新")
public class UserController {

    private final UserService userService;
    private final PostService postService;
    private final CommentService commentService;

    @GetMapping
    @Operation(summary = "ユーザー検索", description = "ユーザー名に検索クエリを含むユーザーを返す。")
    public List<UserResponse> searchUsers(
            @Parameter(description = "検索クエリ（最低1文字）", example = "alice")
            @RequestParam String q,
            @AuthenticationPrincipal UserDetails userDetails) {
        return userService.searchUsers(q, Long.parseLong(userDetails.getUsername()));
    }

    @GetMapping("/{id}")
    @Operation(summary = "ユーザープロフィールを取得")
    @ApiResponse(responseCode = "404", description = "ユーザーが存在しない")
    public UserResponse getUser(@PathVariable Long id,
                                @AuthenticationPrincipal UserDetails userDetails) {
        return userService.getUserProfile(id, Long.parseLong(userDetails.getUsername()));
    }

    @GetMapping("/{id}/posts")
    @Operation(summary = "ユーザーが作成した投稿一覧を取得")
    public List<PostResponse> getUserPosts(@PathVariable Long id,
                                           @AuthenticationPrincipal UserDetails userDetails) {
        return postService.getPostsByUser(id, Long.parseLong(userDetails.getUsername()));
    }

    @GetMapping("/{id}/liked-posts")
    @Operation(summary = "ユーザーがいいねした投稿一覧を取得")
    public List<PostResponse> getLikedPosts(@PathVariable Long id,
                                            @AuthenticationPrincipal UserDetails userDetails) {
        return postService.getLikedPostsByUser(id, Long.parseLong(userDetails.getUsername()));
    }

    @GetMapping("/{id}/comments")
    @Operation(summary = "ユーザーが投稿したコメント一覧を取得")
    public List<CommentResponse> getUserComments(@PathVariable Long id,
                                                 @AuthenticationPrincipal UserDetails userDetails) {
        return commentService.getCommentsByUser(id);
    }

    @PatchMapping("/{id}")
    @Operation(summary = "プロフィールを更新（本人のみ）")
    @ApiResponse(responseCode = "403", description = "他のユーザーのプロフィールは更新不可")
    public UserResponse updateProfile(@PathVariable Long id,
                                      @Valid @RequestBody UpdateProfileRequest req,
                                      @AuthenticationPrincipal UserDetails userDetails) {
        return userService.updateProfile(id, req, Long.parseLong(userDetails.getUsername()));
    }
}
