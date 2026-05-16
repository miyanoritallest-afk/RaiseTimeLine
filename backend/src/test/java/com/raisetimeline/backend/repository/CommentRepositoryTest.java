package com.raisetimeline.backend.repository;

import com.raisetimeline.backend.entity.Comment;
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
class CommentRepositoryTest {

    @Autowired TestEntityManager em;
    @Autowired CommentRepository commentRepository;

    private User alice;
    private User bob;
    private Post post1;
    private Post post2;
    private Post post3;  // コメントなし
    private Comment c1, c2, c3, c4;

    @BeforeEach
    void setUp() {
        alice = em.persistAndFlush(User.builder()
            .email("alice@test.com").username("alice").passwordHash("h").build());
        bob = em.persistAndFlush(User.builder()
            .email("bob@test.com").username("bob").passwordHash("h").build());

        post1 = em.persistAndFlush(Post.builder().user(alice).content("post1").build());
        post2 = em.persistAndFlush(Post.builder().user(alice).content("post2").build());
        post3 = em.persistAndFlush(Post.builder().user(alice).content("post3 no comments").build());

        c1 = em.persistAndFlush(Comment.builder().post(post1).user(alice).content("c1").build());
        c2 = em.persistAndFlush(Comment.builder().post(post1).user(alice).content("c2").build());
        c3 = em.persistAndFlush(Comment.builder().post(post1).user(bob).content("c3").build());
        c4 = em.persistAndFlush(Comment.builder().post(post2).user(alice).content("c4").build());

        em.clear();
    }

    // BB（仕様確認）: post1 の 3 件が ASC 順、user 非null（JOIN FETCH）
    @Test
    void findByPostIdOrderByCreatedAtAsc_correctOrder() {
        List<Comment> comments = commentRepository.findByPostIdOrderByCreatedAtAsc(post1.getId());

        assertThat(comments).hasSize(3);
        // createdAt の昇順
        for (int i = 0; i < comments.size() - 1; i++) {
            assertThat(comments.get(i).getCreatedAt())
                .isBeforeOrEqualTo(comments.get(i + 1).getCreatedAt());
        }
        // JOIN FETCH で user が取得済み
        assertThat(comments.get(0).getUser()).isNotNull();
        assertThat(comments.get(0).getUser().getUsername()).isNotNull();
    }

    // BB同値分割: post2 → c4 のみ
    @Test
    void findByPostIdOrderByCreatedAtAsc_otherPost_returnsOwn() {
        List<Comment> comments = commentRepository.findByPostIdOrderByCreatedAtAsc(post2.getId());

        assertThat(comments).hasSize(1);
        assertThat(comments.get(0).getContent()).isEqualTo("c4");
    }

    // BB（仕様確認）: countByPostId
    @Test
    void countByPostId_correctCount() {
        assertThat(commentRepository.countByPostId(post1.getId())).isEqualTo(3L);
        assertThat(commentRepository.countByPostId(post2.getId())).isEqualTo(1L);
        assertThat(commentRepository.countByPostId(post3.getId())).isEqualTo(0L);
    }

    // BB（仕様確認）: findByUserIdOrderByCreatedAtDesc - JOIN FETCH post 済み
    @Test
    void findByUserIdOrderByCreatedAtDesc_returnsUserComments() {
        List<Comment> comments = commentRepository.findByUserIdOrderByCreatedAtDesc(alice.getId());

        assertThat(comments).hasSize(3); // c1, c2, c4
        // DESC 順
        for (int i = 0; i < comments.size() - 1; i++) {
            assertThat(comments.get(i).getCreatedAt())
                .isAfterOrEqualTo(comments.get(i + 1).getCreatedAt());
        }
        // JOIN FETCH c.post
        assertThat(comments.get(0).getPost()).isNotNull();
        assertThat(comments.get(0).getPost().getContent()).isNotNull();
    }

    // WB（クエリ確認）: countByPostIds バルク集計
    @Test
    void countByPostIds_bulkCount_allPostsPresent() {
        Set<Long> ids = Set.of(post1.getId(), post2.getId());
        List<Object[]> rows = commentRepository.countByPostIds(ids);

        Map<Long, Long> countMap = rows.stream().collect(
            Collectors.toMap(r -> (Long) r[0], r -> ((Number) r[1]).longValue()));

        assertThat(countMap.get(post1.getId())).isEqualTo(3L);
        assertThat(countMap.get(post2.getId())).isEqualTo(1L);
    }

    // BB境界値（空入力）: 空コレクション → 空リスト（例外なし）
    @Test
    void countByPostIds_emptyInput_returnsEmptyList() {
        List<Object[]> rows = commentRepository.countByPostIds(Set.of());

        assertThat(rows).isEmpty();
    }

    // BB同値分割: コメントなし投稿は結果に含まれない
    @Test
    void countByPostIds_postWithNoComments_notInResult() {
        Set<Long> ids = Set.of(post3.getId());
        List<Object[]> rows = commentRepository.countByPostIds(ids);

        assertThat(rows).isEmpty();
    }
}
