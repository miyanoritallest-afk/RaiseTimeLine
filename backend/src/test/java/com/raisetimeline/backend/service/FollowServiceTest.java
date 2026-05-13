package com.raisetimeline.backend.service;

import com.raisetimeline.backend.dto.response.FollowResponse;
import com.raisetimeline.backend.entity.User;
import com.raisetimeline.backend.repository.FollowRepository;
import com.raisetimeline.backend.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.BDDMockito.*;

@ExtendWith(MockitoExtension.class)
class FollowServiceTest {

    @Mock FollowRepository followRepository;
    @Mock UserRepository userRepository;

    @InjectMocks FollowService followService;

    @Test
    void follow_selfFollow_throwsIllegalArgument() {
        assertThatThrownBy(() ->
            followService.follow(1L, 1L)
        ).isInstanceOf(IllegalArgumentException.class)
         .hasMessageContaining("自分自身");
    }

    @Test
    void follow_success() {
        given(userRepository.existsById(2L)).willReturn(true);
        given(followRepository.existsByFollowerIdAndFollowingId(1L, 2L)).willReturn(false);
        given(userRepository.getReferenceById(1L)).willReturn(User.builder().id(1L).build());
        given(userRepository.getReferenceById(2L)).willReturn(User.builder().id(2L).build());
        given(followRepository.countByFollowingId(2L)).willReturn(1L);
        given(followRepository.countByFollowerId(2L)).willReturn(0L);

        FollowResponse res = followService.follow(2L, 1L);

        assertThat(res.isFollowing()).isTrue();
        assertThat(res.followersCount()).isEqualTo(1L);
    }

    @Test
    void follow_alreadyFollowing_idempotent() {
        given(userRepository.existsById(2L)).willReturn(true);
        given(followRepository.existsByFollowerIdAndFollowingId(1L, 2L)).willReturn(true);
        given(followRepository.countByFollowingId(2L)).willReturn(1L);
        given(followRepository.countByFollowerId(2L)).willReturn(0L);

        FollowResponse res = followService.follow(2L, 1L);

        assertThat(res.isFollowing()).isTrue();
        verify(followRepository, never()).save(any());
    }

    @Test
    void unfollow_success() {
        given(userRepository.existsById(2L)).willReturn(true);
        given(followRepository.findByFollowerIdAndFollowingId(1L, 2L))
            .willReturn(java.util.Optional.of(
                com.raisetimeline.backend.entity.Follow.builder().build()));
        given(followRepository.countByFollowingId(2L)).willReturn(0L);
        given(followRepository.countByFollowerId(2L)).willReturn(0L);

        FollowResponse res = followService.unfollow(2L, 1L);

        assertThat(res.isFollowing()).isFalse();
    }
}
