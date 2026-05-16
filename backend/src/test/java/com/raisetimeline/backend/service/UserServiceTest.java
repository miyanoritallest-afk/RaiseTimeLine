package com.raisetimeline.backend.service;

import com.raisetimeline.backend.dto.request.UpdateProfileRequest;
import com.raisetimeline.backend.dto.response.UserResponse;
import com.raisetimeline.backend.entity.User;
import com.raisetimeline.backend.exception.ForbiddenException;
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
class UserServiceTest {

    @Mock UserRepository userRepository;
    @Mock FollowRepository followRepository;

    @InjectMocks UserService userService;

    private User alice;
    private User bob;

    @BeforeEach
    void setUp() {
        alice = User.builder().id(1L).username("alice").email("alice@example.com").bio("bio").avatarUrl("http://avatar.jpg").build();
        bob   = User.builder().id(2L).username("bob").email("bob@example.com").build();
    }

    // ─── getUserProfile ───────────────────────────────────────────────────

    // WB分岐網羅（分岐1=F, 2: 他ユーザー）: 他ユーザーのプロフィール → isFollowing はリポジトリ結果に依存
    @Test
    void getUserProfile_userExists_otherUser_isFollowingChecked() {
        given(userRepository.findById(2L)).willReturn(Optional.of(bob));
        given(followRepository.countByFollowingId(2L)).willReturn(5L);
        given(followRepository.countByFollowerId(2L)).willReturn(3L);
        given(followRepository.existsByFollowerIdAndFollowingId(1L, 2L)).willReturn(true);

        UserResponse res = userService.getUserProfile(2L, 1L);

        assertThat(res.id()).isEqualTo(2L);
        assertThat(res.followersCount()).isEqualTo(5L);
        assertThat(res.isFollowing()).isTrue();
    }

    // WB条件網羅（短絡評価）: 自分自身のプロフィール → isFollowing=false・existsByFollowerIdAndFollowingId 呼び出しなし
    @Test
    void getUserProfile_userExists_selfProfile_isFollowingFalse() {
        given(userRepository.findById(1L)).willReturn(Optional.of(alice));
        given(followRepository.countByFollowingId(1L)).willReturn(0L);
        given(followRepository.countByFollowerId(1L)).willReturn(0L);

        UserResponse res = userService.getUserProfile(1L, 1L);

        assertThat(res.isFollowing()).isFalse();
        then(followRepository).should(never()).existsByFollowerIdAndFollowingId(any(), any());
    }

    // BB同値分割（無効）/ WB分岐1=T: ユーザー未発見 → ResourceNotFoundException
    @Test
    void getUserProfile_userNotFound_throwsResourceNotFound() {
        given(userRepository.findById(999L)).willReturn(Optional.empty());

        assertThatThrownBy(() ->
            userService.getUserProfile(999L, 1L)
        ).isInstanceOf(ResourceNotFoundException.class);
    }

    // ─── searchUsers ──────────────────────────────────────────────────────

    // BB同値分割（無効）/ WB分岐3=T（blank）: 空文字クエリ → List.of()・リポジトリ呼び出しなし
    @Test
    void searchUsers_blankQuery_returnsEmpty() {
        List<UserResponse> res = userService.searchUsers("  ", 1L);

        assertThat(res).isEmpty();
        then(userRepository).should(never()).findByUsernameContainingIgnoreCase(any());
    }

    // BB同値分割（無効）/ WB条件網羅（null）: null クエリ → List.of()
    @Test
    void searchUsers_nullQuery_returnsEmpty() {
        List<UserResponse> res = userService.searchUsers(null, 1L);

        assertThat(res).isEmpty();
        then(userRepository).should(never()).findByUsernameContainingIgnoreCase(any());
    }

    // WB分岐網羅（分岐3=F, 4=T）: 検索ヒットなし → List.of()
    @Test
    void searchUsers_noMatchingUsers_returnsEmpty() {
        given(userRepository.findByUsernameContainingIgnoreCase("xyz")).willReturn(List.of());

        List<UserResponse> res = userService.searchUsers("xyz", 1L);

        assertThat(res).isEmpty();
        then(followRepository).should(never()).findFollowingIdsByFollowerIdAndUserIds(any(), any());
    }

    // WB分岐網羅（分岐3=F, 4=F）: マッチあり → isFollowing が followingIds.contains の結果と一致
    @Test
    void searchUsers_matchingUsers_returnsWithFollowStatus() {
        given(userRepository.findByUsernameContainingIgnoreCase("bob")).willReturn(List.of(bob));
        given(followRepository.findFollowingIdsByFollowerIdAndUserIds(eq(1L), any()))
            .willReturn(Set.of(2L));  // alice は bob をフォロー中
        given(followRepository.countByFollowingId(2L)).willReturn(1L);
        given(followRepository.countByFollowerId(2L)).willReturn(0L);

        List<UserResponse> res = userService.searchUsers("bob", 1L);

        assertThat(res).hasSize(1);
        assertThat(res.get(0).username()).isEqualTo("bob");
        assertThat(res.get(0).isFollowing()).isTrue();
    }

    // ─── updateProfile ────────────────────────────────────────────────────

    // WB分岐網羅（分岐5=F, 6=F, 7=T, 8=T）: 全フィールドあり → username/bio/avatarUrl すべて更新
    @Test
    void updateProfile_self_allFieldsPresent_success() {
        given(userRepository.findById(1L)).willReturn(Optional.of(alice));
        given(userRepository.save(any(User.class))).willReturn(alice);
        given(followRepository.countByFollowingId(1L)).willReturn(0L);
        given(followRepository.countByFollowerId(1L)).willReturn(0L);

        userService.updateProfile(1L,
            new UpdateProfileRequest("newname", "new bio", "http://new-avatar.jpg"), 1L);

        then(userRepository).should().save(argThat(u ->
            u.getUsername().equals("newname")
            && u.getBio().equals("new bio")
            && u.getAvatarUrl().equals("http://new-avatar.jpg")
        ));
    }

    // WB分岐網羅（分岐7=F）: bio が null → bio は更新されない
    @Test
    void updateProfile_self_bioNull_skippedBio() {
        given(userRepository.findById(1L)).willReturn(Optional.of(alice));
        given(userRepository.save(any(User.class))).willReturn(alice);
        given(followRepository.countByFollowingId(1L)).willReturn(0L);
        given(followRepository.countByFollowerId(1L)).willReturn(0L);

        userService.updateProfile(1L,
            new UpdateProfileRequest("newname", null, "http://new-avatar.jpg"), 1L);

        then(userRepository).should().save(argThat(u ->
            u.getBio().equals("bio")  // 元の bio が保持される
        ));
    }

    // WB分岐網羅（分岐8=F）: avatarUrl が null → avatarUrl は更新されない
    @Test
    void updateProfile_self_avatarUrlNull_skippedAvatar() {
        given(userRepository.findById(1L)).willReturn(Optional.of(alice));
        given(userRepository.save(any(User.class))).willReturn(alice);
        given(followRepository.countByFollowingId(1L)).willReturn(0L);
        given(followRepository.countByFollowerId(1L)).willReturn(0L);

        userService.updateProfile(1L,
            new UpdateProfileRequest("newname", "new bio", null), 1L);

        then(userRepository).should().save(argThat(u ->
            u.getAvatarUrl().equals("http://avatar.jpg")  // 元の avatarUrl が保持される
        ));
    }

    // BB同値分割（無効）/ WB分岐5=T: 他人のプロフィール更新 → ForbiddenException・DB アクセスなし
    @Test
    void updateProfile_otherUser_throwsForbidden() {
        assertThatThrownBy(() ->
            userService.updateProfile(2L,
                new UpdateProfileRequest("hacker", null, null), 1L)
        ).isInstanceOf(ForbiddenException.class);

        then(userRepository).should(never()).findById(any());
    }

    // WB分岐網羅（分岐5=F, 6=T）: 自分だがユーザー未発見 → ResourceNotFoundException
    @Test
    void updateProfile_self_userNotFound_throwsResourceNotFound() {
        given(userRepository.findById(1L)).willReturn(Optional.empty());

        assertThatThrownBy(() ->
            userService.updateProfile(1L,
                new UpdateProfileRequest("name", null, null), 1L)
        ).isInstanceOf(ResourceNotFoundException.class);
    }
}
