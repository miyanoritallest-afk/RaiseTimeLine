package com.raisetimeline.backend.repository;

import com.raisetimeline.backend.entity.Follow;
import com.raisetimeline.backend.entity.Post;
import com.raisetimeline.backend.entity.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;

import static org.assertj.core.api.Assertions.*;

@DataJpaTest
@ActiveProfiles("test")
class PostRepositoryTest {

    @Autowired TestEntityManager em;
    @Autowired PostRepository postRepository;
    @Autowired FollowRepository followRepository;

    private User alice;
    private User bob;
    private User carol;
    private Post p1, p2, p3;  // alice の投稿
    private Post p4, p5;      // bob の投稿

    @BeforeEach
    void setUp() {
        alice = em.persistAndFlush(User.builder()
            .email("alice@test.com").username("alice").passwordHash("h").build());
        bob = em.persistAndFlush(User.builder()
            .email("bob@test.com").username("bob").passwordHash("h").build());
        carol = em.persistAndFlush(User.builder()
            .email("carol@test.com").username("carol").passwordHash("h").build());

        p1 = em.persistAndFlush(Post.builder().user(alice).content("alice post 1").build());
        p2 = em.persistAndFlush(Post.builder().user(alice).content("alice post 2").build());
        p3 = em.persistAndFlush(Post.builder().user(alice).content("alice post 3").build());
        p4 = em.persistAndFlush(Post.builder().user(bob).content("bob post 1").build());
        p5 = em.persistAndFlush(Post.builder().user(bob).content("bob post 2").build());

        // alice は bob をフォロー
        em.persistAndFlush(Follow.builder().follower(alice).following(bob).build());

        em.clear();
    }

    // BB（仕様確認）: 全件取得・降順・user が null でない（JOIN FETCH 確認）
    @Test
    void findAllForFeed_returnsAllPostsDescendingById() {
        List<Post> posts = postRepository.findAllForFeed(PageRequest.of(0, 10));

        assertThat(posts).hasSize(5);
        // ID 降順
        assertThat(posts.get(0).getId()).isGreaterThan(posts.get(1).getId());
        // JOIN FETCH で user が取得済み
        assertThat(posts.get(0).getUser()).isNotNull();
        assertThat(posts.get(0).getUser().getUsername()).isNotNull();
    }

    // BB境界値: ページサイズ制限
    @Test
    void findAllForFeed_withPageable_limitsResults() {
        List<Post> posts = postRepository.findAllForFeed(PageRequest.of(0, 2));

        assertThat(posts).hasSize(2);
    }

    // WB（クエリ条件）: cursor より小さい ID のみ返却
    @Test
    void findAllForFeedBefore_cursor_excludesAtAndAbove() {
        // p3 の ID をカーソルとして使う（p3 自体と p4, p5 は除外）
        List<Post> posts = postRepository.findAllForFeedBefore(p3.getId(), PageRequest.of(0, 10));

        assertThat(posts).allMatch(p -> p.getId() < p3.getId());
        assertThat(posts).hasSize(2); // p1, p2
    }

    // BB境界値: cursor が最小値 → 空
    @Test
    void findAllForFeedBefore_cursorAtMin_returnsEmpty() {
        List<Post> posts = postRepository.findAllForFeedBefore(p1.getId(), PageRequest.of(0, 10));

        assertThat(posts).isEmpty();
    }

    // BB（仕様確認）: following feed → フォロー中ユーザーの投稿のみ
    @Test
    void findFollowingFeed_returnsOnlyFollowedUserPosts() {
        // alice の following feed → bob の投稿のみ
        List<Post> posts = postRepository.findFollowingFeed(alice.getId(), PageRequest.of(0, 10));

        assertThat(posts).hasSize(2);
        assertThat(posts).allMatch(p -> p.getUser().getId().equals(bob.getId()));
    }

    // BB同値分割（無効）: フォローなし → 空
    @Test
    void findFollowingFeed_noFollows_returnsEmpty() {
        // carol はフォロー関係なし
        List<Post> posts = postRepository.findFollowingFeed(carol.getId(), PageRequest.of(0, 10));

        assertThat(posts).isEmpty();
    }

    // WB（クエリ条件）: cursor 付き following feed
    @Test
    void findFollowingFeedBefore_appliesCursor() {
        // alice の following feed, cursor=p5 → p4 のみ
        List<Post> posts = postRepository.findFollowingFeedBefore(
            alice.getId(), p5.getId(), PageRequest.of(0, 10));

        assertThat(posts).hasSize(1);
        assertThat(posts.get(0).getId()).isEqualTo(p4.getId());
    }

    // BB（仕様確認）: ユーザー別投稿一覧（alice のみ・createdAt DESC）
    @Test
    void findByUserIdOrderByCreatedAtDesc_returnsUserPostsOnly() {
        List<Post> posts = postRepository.findByUserIdOrderByCreatedAtDesc(alice.getId());

        assertThat(posts).hasSize(3);
        assertThat(posts).allMatch(p -> p.getUser().getId().equals(alice.getId()));
    }

    // BB同値分割: 投稿なしユーザー → 空
    @Test
    void findByUserIdOrderByCreatedAtDesc_userWithNoPosts_returnsEmpty() {
        List<Post> posts = postRepository.findByUserIdOrderByCreatedAtDesc(carol.getId());

        assertThat(posts).isEmpty();
    }
}
