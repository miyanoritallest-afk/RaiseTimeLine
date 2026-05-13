package com.raisetimeline.backend.service;

import com.raisetimeline.backend.dto.request.CreatePostRequest;
import com.raisetimeline.backend.dto.request.UpdatePostRequest;
import com.raisetimeline.backend.dto.response.PostResponse;
import com.raisetimeline.backend.entity.Post;
import com.raisetimeline.backend.entity.User;
import com.raisetimeline.backend.exception.ForbiddenException;
import com.raisetimeline.backend.exception.ResourceNotFoundException;
import com.raisetimeline.backend.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;

@ExtendWith(MockitoExtension.class)
class PostServiceTest {

    @Mock PostRepository postRepository;
    @Mock LikeRepository likeRepository;
    @Mock CommentRepository commentRepository;
    @Mock FollowRepository followRepository;
    @Mock UserRepository userRepository;

    @InjectMocks PostService postService;

    private User user;
    private Post post;

    @BeforeEach
    void setUp() {
        user = User.builder().id(1L).username("alice").email("alice@example.com").build();
        post = Post.builder().id(10L).user(user).content("hello").images(new ArrayList<>()).build();
    }

    @Test
    void createPost_success() {
        given(userRepository.getReferenceById(1L)).willReturn(user);
        given(postRepository.save(any(Post.class))).willReturn(post);
        given(likeRepository.countByPostId(10L)).willReturn(0L);
        given(commentRepository.countByPostId(10L)).willReturn(0L);
        given(followRepository.countByFollowingId(1L)).willReturn(0L);
        given(followRepository.countByFollowerId(1L)).willReturn(0L);

        PostResponse res = postService.createPost(
            new CreatePostRequest("hello", List.of()), 1L);

        assertThat(res.content()).isEqualTo("hello");
        assertThat(res.likedByMe()).isFalse();
    }

    @Test
    void updatePost_notOwner_throwsForbidden() {
        given(postRepository.findById(10L)).willReturn(Optional.of(post));

        assertThatThrownBy(() ->
            postService.updatePost(10L, new UpdatePostRequest("edit", List.of()), 99L)
        ).isInstanceOf(ForbiddenException.class);
    }

    @Test
    void deletePost_notFound_throwsResourceNotFound() {
        given(postRepository.findById(999L)).willReturn(Optional.empty());

        assertThatThrownBy(() ->
            postService.deletePost(999L, 1L)
        ).isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void deletePost_notOwner_throwsForbidden() {
        given(postRepository.findById(10L)).willReturn(Optional.of(post));

        assertThatThrownBy(() ->
            postService.deletePost(10L, 99L)
        ).isInstanceOf(ForbiddenException.class);
    }
}
