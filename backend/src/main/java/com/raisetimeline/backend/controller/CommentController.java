package com.raisetimeline.backend.controller;

import com.raisetimeline.backend.dto.request.CreateCommentRequest;
import com.raisetimeline.backend.dto.request.UpdateCommentRequest;
import com.raisetimeline.backend.dto.response.CommentResponse;
import com.raisetimeline.backend.service.CommentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@Tag(name = "Comments", description = "コメントの CRUD")
public class CommentController {

    private final CommentService commentService;

    @GetMapping("/api/posts/{postId}/comments")
    @Operation(summary = "投稿のコメント一覧を取得")
    public List<CommentResponse> getComments(@PathVariable Long postId) {
        return commentService.getCommentsByPost(postId);
    }

    @PostMapping("/api/posts/{postId}/comments")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "コメントを追加")
    @ApiResponse(responseCode = "201", description = "コメント作成成功")
    @ApiResponse(responseCode = "404", description = "投稿が存在しない")
    public CommentResponse createComment(@PathVariable Long postId,
                                         @Valid @RequestBody CreateCommentRequest req,
                                         @AuthenticationPrincipal UserDetails userDetails) {
        return commentService.createComment(postId, req, Long.parseLong(userDetails.getUsername()));
    }

    @PatchMapping("/api/comments/{id}")
    @Operation(summary = "コメントを編集（投稿者のみ）")
    @ApiResponse(responseCode = "403", description = "投稿者以外は編集不可")
    @ApiResponse(responseCode = "404", description = "コメントが存在しない")
    public CommentResponse updateComment(@PathVariable Long id,
                                         @Valid @RequestBody UpdateCommentRequest req,
                                         @AuthenticationPrincipal UserDetails userDetails) {
        return commentService.updateComment(id, req, Long.parseLong(userDetails.getUsername()));
    }

    @DeleteMapping("/api/comments/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "コメントを削除（投稿者のみ）")
    @ApiResponse(responseCode = "204", description = "削除成功")
    @ApiResponse(responseCode = "403", description = "投稿者以外は削除不可")
    public void deleteComment(@PathVariable Long id,
                              @AuthenticationPrincipal UserDetails userDetails) {
        commentService.deleteComment(id, Long.parseLong(userDetails.getUsername()));
    }
}
