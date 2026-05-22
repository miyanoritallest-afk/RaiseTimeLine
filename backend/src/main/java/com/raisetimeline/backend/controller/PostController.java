package com.raisetimeline.backend.controller;

import com.raisetimeline.backend.dto.request.CreatePostRequest;
import com.raisetimeline.backend.dto.request.UpdatePostRequest;
import com.raisetimeline.backend.dto.response.PagedResponse;
import com.raisetimeline.backend.dto.response.PostResponse;
import com.raisetimeline.backend.service.PostService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/posts")
@RequiredArgsConstructor
@Tag(name = "Posts", description = "投稿の CRUD。タイムラインはカーソルベースページネーション。")
public class PostController {

    private final PostService postService;

    @GetMapping
    @Operation(summary = "タイムライン取得", description = "feed は 'all'（全投稿）または 'following'（フォロー中ユーザーの投稿）。cursor に前回レスポンスの nextCursor を渡してページネーション。")
    public PagedResponse<PostResponse> getTimeline(
            @Parameter(description = "フィルター: 'all' または 'following'", example = "all")
            @RequestParam(defaultValue = "all") String feed,
            @Parameter(description = "次ページ取得用カーソル（前回レスポンスの nextCursor）")
            @RequestParam(required = false) Long cursor,
            @AuthenticationPrincipal UserDetails userDetails) {
        Long userId = Long.parseLong(userDetails.getUsername());
        return postService.getTimeline(feed, cursor, userId);
    }

    @GetMapping("/{id}")
    @Operation(summary = "投稿を1件取得")
    @ApiResponse(responseCode = "404", description = "投稿が存在しない")
    public PostResponse getPost(@PathVariable Long id,
                                @AuthenticationPrincipal UserDetails userDetails) {
        Long userId = Long.parseLong(userDetails.getUsername());
        return postService.getPost(id, userId);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "投稿を作成")
    @ApiResponse(responseCode = "201", description = "投稿作成成功")
    public PostResponse createPost(@Valid @RequestBody CreatePostRequest req,
                                   @AuthenticationPrincipal UserDetails userDetails) {
        return postService.createPost(req, Long.parseLong(userDetails.getUsername()));
    }

    @PatchMapping("/{id}")
    @Operation(summary = "投稿を更新（投稿者のみ）")
    @ApiResponse(responseCode = "403", description = "投稿者以外は更新不可")
    @ApiResponse(responseCode = "404", description = "投稿が存在しない")
    public PostResponse updatePost(@PathVariable Long id,
                                   @Valid @RequestBody UpdatePostRequest req,
                                   @AuthenticationPrincipal UserDetails userDetails) {
        return postService.updatePost(id, req, Long.parseLong(userDetails.getUsername()));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "投稿を削除（投稿者のみ）")
    @ApiResponse(responseCode = "204", description = "削除成功")
    @ApiResponse(responseCode = "403", description = "投稿者以外は削除不可")
    public void deletePost(@PathVariable Long id,
                           @AuthenticationPrincipal UserDetails userDetails) {
        postService.deletePost(id, Long.parseLong(userDetails.getUsername()));
    }
}
