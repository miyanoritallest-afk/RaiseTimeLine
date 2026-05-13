package com.raisetimeline.backend.service;

import com.raisetimeline.backend.dto.response.LikeResponse;
import com.raisetimeline.backend.entity.Like;
import com.raisetimeline.backend.entity.Post;
import com.raisetimeline.backend.entity.User;
import com.raisetimeline.backend.repository.LikeRepository;
import com.raisetimeline.backend.repository.PostRepository;
import com.raisetimeline.backend.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;

@ExtendWith(MockitoExtension.class)
class LikeServiceTest {

    @Mock LikeRepository likeRepository;
    @Mock PostRepository postRepository;
    @Mock UserRepository userRepository;

    @InjectMocks LikeService likeService;

    @Test
    void likePost_success() {
        given(postRepository.existsById(1L)).willReturn(true);
        given(likeRepository.existsByPostIdAndUserId(1L, 2L)).willReturn(false);
        given(postRepository.getReferenceById(1L)).willReturn(Post.builder().id(1L).build());
        given(userRepository.getReferenceById(2L)).willReturn(User.builder().id(2L).build());
        given(likeRepository.save(any(Like.class))).willReturn(Like.builder().build());
        given(likeRepository.countByPostId(1L)).willReturn(1L);

        LikeResponse res = likeService.likePost(1L, 2L);

        assertThat(res.likedByMe()).isTrue();
        assertThat(res.likeCount()).isEqualTo(1L);
    }

    @Test
    void likePost_alreadyLiked_idempotent() {
        given(postRepository.existsById(1L)).willReturn(true);
        given(likeRepository.existsByPostIdAndUserId(1L, 2L)).willReturn(true);
        given(likeRepository.countByPostId(1L)).willReturn(5L);

        LikeResponse res = likeService.likePost(1L, 2L);

        assertThat(res.likedByMe()).isTrue();
        assertThat(res.likeCount()).isEqualTo(5L);
        verify(likeRepository, never()).save(any());
    }

    @Test
    void unlikePost_success() {
        Like like = Like.builder().build();
        given(postRepository.existsById(1L)).willReturn(true);
        given(likeRepository.findByPostIdAndUserId(1L, 2L)).willReturn(Optional.of(like));
        given(likeRepository.countByPostId(1L)).willReturn(0L);

        LikeResponse res = likeService.unlikePost(1L, 2L);

        assertThat(res.likedByMe()).isFalse();
        verify(likeRepository).delete(like);
    }

    @Test
    void unlikePost_notLiked_idempotent() {
        given(postRepository.existsById(1L)).willReturn(true);
        given(likeRepository.findByPostIdAndUserId(1L, 2L)).willReturn(Optional.empty());
        given(likeRepository.countByPostId(1L)).willReturn(0L);

        LikeResponse res = likeService.unlikePost(1L, 2L);

        assertThat(res.likedByMe()).isFalse();
        verify(likeRepository, never()).delete(any());
    }
}
