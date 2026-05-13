package com.raisetimeline.backend.controller;

import com.raisetimeline.backend.dto.request.CreateCommentRequest;
import com.raisetimeline.backend.dto.request.UpdateCommentRequest;
import com.raisetimeline.backend.dto.response.CommentResponse;
import com.raisetimeline.backend.service.CommentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class CommentController {

    private final CommentService commentService;

    @GetMapping("/api/posts/{postId}/comments")
    public List<CommentResponse> getComments(@PathVariable Long postId) {
        return commentService.getCommentsByPost(postId);
    }

    @PostMapping("/api/posts/{postId}/comments")
    @ResponseStatus(HttpStatus.CREATED)
    public CommentResponse createComment(@PathVariable Long postId,
                                         @Valid @RequestBody CreateCommentRequest req,
                                         @AuthenticationPrincipal UserDetails userDetails) {
        return commentService.createComment(postId, req, Long.parseLong(userDetails.getUsername()));
    }

    @PatchMapping("/api/comments/{id}")
    public CommentResponse updateComment(@PathVariable Long id,
                                         @Valid @RequestBody UpdateCommentRequest req,
                                         @AuthenticationPrincipal UserDetails userDetails) {
        return commentService.updateComment(id, req, Long.parseLong(userDetails.getUsername()));
    }

    @DeleteMapping("/api/comments/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteComment(@PathVariable Long id,
                              @AuthenticationPrincipal UserDetails userDetails) {
        commentService.deleteComment(id, Long.parseLong(userDetails.getUsername()));
    }
}
