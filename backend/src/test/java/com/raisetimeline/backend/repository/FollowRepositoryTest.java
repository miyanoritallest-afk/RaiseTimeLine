package com.raisetimeline.backend.repository;

import com.raisetimeline.backend.entity.Follow;
import com.raisetimeline.backend.entity.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.*;

@DataJpaTest
@ActiveProfiles("test")
class FollowRepositoryTest {

    @Autowired TestEntityManager em;
    @Autowired FollowRepository followRepository;

    private User alice;
    private User bob;
    private User carol;

    @BeforeEach
    void setUp() {
        alice = em.persistAndFlush(User.builder()
            .email("alice@test.com").username("alice").passwordHash("h").build());
        bob = em.persistAndFlush(User.builder()
            .email("bob@test.com").username("bob").passwordHash("h").build());
        carol = em.persistAndFlush(User.builder()
            .email("carol@test.com").username("carol").passwordHash("h").build());

        // alice→bob・carol をフォロー、bob→carol をフォロー
        em.persistAndFlush(Follow.builder().follower(alice).following(bob).build());
        em.persistAndFlush(Follow.builder().follower(alice).following(carol).build());
        em.persistAndFlush(Follow.builder().follower(bob).following(carol).build());

        em.clear();
    }

    // BB同値分割（有効）: alice→bob → true
    @Test
    void existsByFollowerIdAndFollowingId_exists_true() {
        assertThat(followRepository.existsByFollowerIdAndFollowingId(alice.getId(), bob.getId())).isTrue();
    }

    // BB同値分割（無効）: bob→alice（逆方向）→ false
    @Test
    void existsByFollowerIdAndFollowingId_notExists_false() {
        assertThat(followRepository.existsByFollowerIdAndFollowingId(bob.getId(), alice.getId())).isFalse();
    }

    // BB（仕様確認）: carol のフォロワー数 → 2（alice・bob）
    @Test
    void countByFollowingId_correctFollowerCount() {
        assertThat(followRepository.countByFollowingId(carol.getId())).isEqualTo(2L);
    }

    // BB（仕様確認）: alice のフォロー数 → 2（bob・carol）
    @Test
    void countByFollowerId_correctFollowingCount() {
        assertThat(followRepository.countByFollowerId(alice.getId())).isEqualTo(2L);
    }

    // WB（JOIN FETCH確認）: carol のフォロワー一覧 → [alice, bob]、follow.getFollower() 非null
    @Test
    void findFollowersByFollowingId_fetchesFollower() {
        List<Follow> follows = followRepository.findFollowersByFollowingId(carol.getId());

        assertThat(follows).hasSize(2);
        assertThat(follows.get(0).getFollower()).isNotNull();
        assertThat(follows.get(0).getFollower().getUsername()).isNotNull();
        Set<Long> followerIds = follows.stream()
            .map(f -> f.getFollower().getId())
            .collect(Collectors.toSet());
        assertThat(followerIds).containsExactlyInAnyOrder(alice.getId(), bob.getId());
    }

    // WB（JOIN FETCH確認）: alice のフォロー一覧 → [bob, carol]、follow.getFollowing() 非null
    @Test
    void findFollowingByFollowerId_fetchesFollowing() {
        List<Follow> follows = followRepository.findFollowingByFollowerId(alice.getId());

        assertThat(follows).hasSize(2);
        assertThat(follows.get(0).getFollowing()).isNotNull();
        assertThat(follows.get(0).getFollowing().getUsername()).isNotNull();
        Set<Long> followingIds = follows.stream()
            .map(f -> f.getFollowing().getId())
            .collect(Collectors.toSet());
        assertThat(followingIds).containsExactlyInAnyOrder(bob.getId(), carol.getId());
    }

    // WB（クエリ確認）: alice, {bob,carol,alice} → {bob,carol}（自分は除く、フォロー中のみ）
    @Test
    void findFollowingIdsByFollowerIdAndUserIds_returnsFollowingSubset() {
        Set<Long> result = followRepository.findFollowingIdsByFollowerIdAndUserIds(
            alice.getId(), Set.of(bob.getId(), carol.getId(), alice.getId()));

        assertThat(result).containsExactlyInAnyOrder(bob.getId(), carol.getId());
        assertThat(result).doesNotContain(alice.getId());
    }

    // WB（クエリ確認）: countFollowersByUserIds バルク集計 → bob=1, carol=2
    @Test
    void countFollowersByUserIds_bulkCount() {
        List<Object[]> rows = followRepository.countFollowersByUserIds(
            Set.of(bob.getId(), carol.getId()));

        Map<Long, Long> countMap = rows.stream().collect(
            Collectors.toMap(r -> (Long) r[0], r -> ((Number) r[1]).longValue()));

        assertThat(countMap.get(bob.getId())).isEqualTo(1L);
        assertThat(countMap.get(carol.getId())).isEqualTo(2L);
    }

    // WB（クエリ確認）: countFollowingByUserIds バルク集計 → alice=2, bob=1
    @Test
    void countFollowingByUserIds_bulkCount() {
        List<Object[]> rows = followRepository.countFollowingByUserIds(
            Set.of(alice.getId(), bob.getId()));

        Map<Long, Long> countMap = rows.stream().collect(
            Collectors.toMap(r -> (Long) r[0], r -> ((Number) r[1]).longValue()));

        assertThat(countMap.get(alice.getId())).isEqualTo(2L);
        assertThat(countMap.get(bob.getId())).isEqualTo(1L);
    }

    // BB境界値（空入力）: 空コレクション → 空リスト（例外なし）
    @Test
    void countFollowersByUserIds_emptyInput_returnsEmpty() {
        List<Object[]> rows = followRepository.countFollowersByUserIds(Set.of());

        assertThat(rows).isEmpty();
    }
}
