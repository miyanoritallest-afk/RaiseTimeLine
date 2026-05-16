package com.raisetimeline.backend.repository;

import com.raisetimeline.backend.entity.Like;
import com.raisetimeline.backend.entity.Post;
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
class LikeRepositoryTest {

    @Autowired TestEntityManager em;
    @Autowired LikeRepository likeRepository;

    private User alice;
    private User bob;
    private Post post1, post2, post3;

    @BeforeEach
    void setUp() {
        alice = em.persistAndFlush(User.builder()
            .email("alice@test.com").username("alice").passwordHash("h").build());
        bob = em.persistAndFlush(User.builder()
            .email("bob@test.com").username("bob").passwordHash("h").build());

        post1 = em.persistAndFlush(Post.builder().user(alice).content("p1").build());
        post2 = em.persistAndFlush(Post.builder().user(alice).content("p2").build());
        post3 = em.persistAndFlush(Post.builder().user(alice).content("p3").build());

        // alice は post1・post2 に Like、bob は post1 に Like
        em.persistAndFlush(Like.builder().post(post1).user(alice).build());
        em.persistAndFlush(Like.builder().post(post2).user(alice).build());
        em.persistAndFlush(Like.builder().post(post1).user(bob).build());

        em.clear();
    }

    // BB同値分割（有効）: alice × post1 → non-empty
    @Test
    void findByPostIdAndUserId_found_returnsOptional() {
        assertThat(likeRepository.findByPostIdAndUserId(post1.getId(), alice.getId())).isPresent();
    }

    // BB同値分割（無効）: alice × post3 → empty
    @Test
    void findByPostIdAndUserId_notFound_returnsEmpty() {
        assertThat(likeRepository.findByPostIdAndUserId(post3.getId(), alice.getId())).isEmpty();
    }

    // BB同値分割: alice × post1 → true
    @Test
    void existsByPostIdAndUserId_liked_true() {
        assertThat(likeRepository.existsByPostIdAndUserId(post1.getId(), alice.getId())).isTrue();
    }

    // BB同値分割: alice × post3 → false
    @Test
    void existsByPostIdAndUserId_notLiked_false() {
        assertThat(likeRepository.existsByPostIdAndUserId(post3.getId(), alice.getId())).isFalse();
    }

    // BB境界値: countByPostId
    @Test
    void countByPostId_correctCount() {
        assertThat(likeRepository.countByPostId(post1.getId())).isEqualTo(2L);
        assertThat(likeRepository.countByPostId(post2.getId())).isEqualTo(1L);
        assertThat(likeRepository.countByPostId(post3.getId())).isEqualTo(0L);
    }

    // WB（クエリ確認）: alice のいいねポスト集合から post1・post2 のみ返る
    @Test
    void findLikedPostIdsByUserIdAndPostIds_returnsOnlyLiked() {
        Set<Long> result = likeRepository.findLikedPostIdsByUserIdAndPostIds(
            alice.getId(), Set.of(post1.getId(), post2.getId(), post3.getId()));

        assertThat(result).containsExactlyInAnyOrder(post1.getId(), post2.getId());
        assertThat(result).doesNotContain(post3.getId());
    }

    // BB境界値（空入力）: 空セット → 空セット（例外なし）
    @Test
    void findLikedPostIdsByUserIdAndPostIds_emptyPostIds_returnsEmpty() {
        Set<Long> result = likeRepository.findLikedPostIdsByUserIdAndPostIds(alice.getId(), Set.of());

        assertThat(result).isEmpty();
    }

    // WB（JOIN FETCH確認）: alice の Like リスト、like.getPost() 非null
    @Test
    void findByUserIdOrderByCreatedAtDesc_postEagerlyFetched() {
        List<Like> likes = likeRepository.findByUserIdOrderByCreatedAtDesc(alice.getId());

        assertThat(likes).hasSize(2);
        assertThat(likes.get(0).getPost()).isNotNull();
        assertThat(likes.get(0).getPost().getContent()).isNotNull();
    }

    // WB（クエリ確認）: countByPostIds バルク
    @Test
    void countByPostIds_bulkCount_correctPairs() {
        Set<Long> ids = Set.of(post1.getId(), post2.getId(), post3.getId());
        List<Object[]> rows = likeRepository.countByPostIds(ids);

        Map<Long, Long> countMap = rows.stream().collect(
            Collectors.toMap(r -> (Long) r[0], r -> ((Number) r[1]).longValue()));

        assertThat(countMap.get(post1.getId())).isEqualTo(2L);
        assertThat(countMap.get(post2.getId())).isEqualTo(1L);
        assertThat(countMap).doesNotContainKey(post3.getId()); // 0件は含まれない
    }
}
