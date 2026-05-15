package com.raisetimeline.backend.controller;

import com.raisetimeline.backend.dto.request.CreatePostRequest;
import com.raisetimeline.backend.dto.request.UpdatePostRequest;
import com.raisetimeline.backend.dto.response.PagedResponse;
import com.raisetimeline.backend.dto.response.PostResponse;
import com.raisetimeline.backend.service.PostService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/posts")
@RequiredArgsConstructor
public class PostController {

    private final PostService postService;

    @GetMapping
    public PagedResponse<PostResponse> getTimeline(
            @RequestParam(defaultValue = "all") String feed,
            @RequestParam(required = false) Long cursor,
            @AuthenticationPrincipal UserDetails userDetails) {
        Long userId = Long.parseLong(userDetails.getUsername());
        return postService.getTimeline(feed, cursor, userId);
    }

    @GetMapping("/{id}")
    public PostResponse getPost(@PathVariable Long id,
                                @AuthenticationPrincipal UserDetails userDetails) {
        Long userId = Long.parseLong(userDetails.getUsername());
        return postService.getPost(id, userId);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public PostResponse createPost(@Valid @RequestBody CreatePostRequest req,
                                   @AuthenticationPrincipal UserDetails userDetails) {
        return postService.createPost(req, Long.parseLong(userDetails.getUsername()));
    }

    @PatchMapping("/{id}")
    public PostResponse updatePost(@PathVariable Long id,
                                   @Valid @RequestBody UpdatePostRequest req,
                                   @AuthenticationPrincipal UserDetails userDetails) {
        return postService.updatePost(id, req, Long.parseLong(userDetails.getUsername()));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deletePost(@PathVariable Long id,
                           @AuthenticationPrincipal UserDetails userDetails) {
        postService.deletePost(id, Long.parseLong(userDetails.getUsername()));
    }
}
