package com.raisetimeline.backend.controller;

import com.raisetimeline.backend.dto.request.UpdateProfileRequest;
import com.raisetimeline.backend.dto.response.CommentResponse;
import com.raisetimeline.backend.dto.response.PostResponse;
import com.raisetimeline.backend.dto.response.UserResponse;
import com.raisetimeline.backend.service.CommentService;
import com.raisetimeline.backend.service.PostService;
import com.raisetimeline.backend.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;
    private final PostService postService;
    private final CommentService commentService;

    @GetMapping
    public List<UserResponse> searchUsers(@RequestParam String q,
                                          @AuthenticationPrincipal UserDetails userDetails) {
        return userService.searchUsers(q, Long.parseLong(userDetails.getUsername()));
    }

    @GetMapping("/{id}")
    public UserResponse getUser(@PathVariable Long id,
                                @AuthenticationPrincipal UserDetails userDetails) {
        return userService.getUserProfile(id, Long.parseLong(userDetails.getUsername()));
    }

    @GetMapping("/{id}/posts")
    public List<PostResponse> getUserPosts(@PathVariable Long id,
                                           @AuthenticationPrincipal UserDetails userDetails) {
        return postService.getPostsByUser(id, Long.parseLong(userDetails.getUsername()));
    }

    @GetMapping("/{id}/liked-posts")
    public List<PostResponse> getLikedPosts(@PathVariable Long id,
                                            @AuthenticationPrincipal UserDetails userDetails) {
        return postService.getLikedPostsByUser(id, Long.parseLong(userDetails.getUsername()));
    }

    @GetMapping("/{id}/comments")
    public List<CommentResponse> getUserComments(@PathVariable Long id,
                                                 @AuthenticationPrincipal UserDetails userDetails) {
        return commentService.getCommentsByUser(id);
    }

    @PatchMapping("/{id}")
    public UserResponse updateProfile(@PathVariable Long id,
                                      @Valid @RequestBody UpdateProfileRequest req,
                                      @AuthenticationPrincipal UserDetails userDetails) {
        return userService.updateProfile(id, req, Long.parseLong(userDetails.getUsername()));
    }
}
