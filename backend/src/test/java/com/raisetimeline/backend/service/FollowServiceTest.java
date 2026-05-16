package com.raisetimeline.backend.service;

import com.raisetimeline.backend.dto.response.FollowResponse;
import com.raisetimeline.backend.dto.response.UserResponse;
import com.raisetimeline.backend.entity.Follow;
import com.raisetimeline.backend.entity.User;
import com.raisetimeline.backend.exception.ResourceNotFoundException;
import com.raisetimeline.backend.repository.FollowRepository;
import com.raisetimeline.backend.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;

@ExtendWith(MockitoExtension.class)
class FollowServiceTest {

    @Mock FollowRepository followRepository;
    @Mock UserRepository userRepository;

    @InjectMocks FollowService followService;

    private User alice;
    private User bob;

    @BeforeEach
    void setUp() {
        alice = User.builder().id(1L).username("alice").email("alice@example.com").build();
        bob   = User.builder().id(2L).username("bob").email("bob@example.com").build();
    }

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
            .willReturn(Optional.of(Follow.builder().build()));
        given(followRepository.countByFollowingId(2L)).willReturn(0L);
        given(followRepository.countByFollowerId(2L)).willReturn(0L);

        FollowResponse res = followService.unfollow(2L, 1L);

        assertThat(res.isFollowing()).isFalse();
    }

    // WB分岐: target 未発見 → ResourceNotFoundException（follow）
    @Test
    void follow_targetNotFound_throwsResourceNotFound() {
        given(userRepository.existsById(999L)).willReturn(false);

        assertThatThrownBy(() -> followService.follow(999L, 1L))
            .isInstanceOf(ResourceNotFoundException.class);
    }

    // WB分岐: target 未発見 → ResourceNotFoundException（unfollow）
    @Test
    void unfollow_targetNotFound_throwsResourceNotFound() {
        given(userRepository.existsById(999L)).willReturn(false);

        assertThatThrownBy(() -> followService.unfollow(999L, 1L))
            .isInstanceOf(ResourceNotFoundException.class);
    }

    // WB分岐（ifPresent で delete が呼ばれない）: フォローしていない → delete 未呼び出し
    @Test
    void unfollow_notFollowing_noDelete() {
        given(userRepository.existsById(2L)).willReturn(true);
        given(followRepository.findByFollowerIdAndFollowingId(1L, 2L)).willReturn(Optional.empty());
        given(followRepository.countByFollowingId(2L)).willReturn(0L);
        given(followRepository.countByFollowerId(2L)).willReturn(0L);

        followService.unfollow(2L, 1L);

        then(followRepository).should(never()).delete(any());
    }

    // WB分岐（toUserResponses 内の users.isEmpty()）: フォロワーなし → 空リスト
    @Test
    void getFollowers_noFollowers_returnsEmpty() {
        given(followRepository.findFollowersByFollowingId(1L)).willReturn(List.of());

        List<UserResponse> res = followService.getFollowers(1L, 2L);

        assertThat(res).isEmpty();
        then(followRepository).should(never()).findFollowingIdsByFollowerIdAndUserIds(any(), any());
    }

    // WB分岐（toUserResponses 内の users.isEmpty()）: フォロー中なし → 空リスト
    @Test
    void getFollowing_noFollowing_returnsEmpty() {
        given(followRepository.findFollowingByFollowerId(1L)).willReturn(List.of());

        List<UserResponse> res = followService.getFollowing(1L, 2L);

        assertThat(res).isEmpty();
    }
}
