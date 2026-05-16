package com.raisetimeline.backend.service;

import com.raisetimeline.backend.dto.request.CreatePostRequest;
import com.raisetimeline.backend.dto.request.UpdatePostRequest;
import com.raisetimeline.backend.dto.response.PagedResponse;
import com.raisetimeline.backend.dto.response.PostResponse;
import com.raisetimeline.backend.entity.Like;
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
import org.springframework.data.domain.Pageable;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

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

    // ─── createPost ───────────────────────────────────────────────────────

    // WB分岐網羅（分岐6=false）: 画像なし → 保存成功
    @Test
    void createPost_noImages_success() {
        given(userRepository.getReferenceById(1L)).willReturn(user);
        given(postRepository.save(any(Post.class))).willReturn(post);
        given(userRepository.findById(1L)).willReturn(Optional.of(user));
        stubBulkCountsForPost(post.getId(), user.getId());

        PostResponse res = postService.createPost(
            new CreatePostRequest("hello", List.of()), 1L);

        assertThat(res.content()).isEqualTo("hello");
        assertThat(res.imageUrls()).isEmpty();
        assertThat(res.likedByMe()).isFalse();
    }

    // WB分岐網羅（分岐6=true）: 画像あり → PostImage が生成される
    @Test
    void createPost_withImages_success() {
        Post postWithImages = Post.builder().id(10L).user(user).content("img post")
            .images(new ArrayList<>()).build();
        given(userRepository.getReferenceById(1L)).willReturn(user);
        given(postRepository.save(any(Post.class))).willReturn(postWithImages);
        given(userRepository.findById(1L)).willReturn(Optional.of(user));
        stubBulkCountsForPost(10L, 1L);

        PostResponse res = postService.createPost(
            new CreatePostRequest("img post", List.of("http://img1.jpg", "http://img2.jpg")), 1L);

        assertThat(res.content()).isEqualTo("img post");
        then(postRepository).should().save(argThat(p -> p.getImages().size() == 2));
    }

    // ─── getTimeline ──────────────────────────────────────────────────────

    // WB分岐パス1（分岐1=F, 3=T）: allフィード・カーソルなし → findAllForFeed 呼び出し
    @Test
    void getTimeline_allFeed_noCursor_callsFindAll() {
        given(postRepository.findAllForFeed(any(Pageable.class))).willReturn(List.of());

        PagedResponse<PostResponse> res = postService.getTimeline("all", null, 1L);

        then(postRepository).should().findAllForFeed(any());
        then(postRepository).should(never()).findAllForFeedBefore(any(), any());
        assertThat(res.items()).isEmpty();
        assertThat(res.hasMore()).isFalse();
    }

    // WB分岐パス2（分岐1=F, 3=F）: allフィード・カーソルあり → findAllForFeedBefore 呼び出し
    @Test
    void getTimeline_allFeed_withCursor_callsFindAllBefore() {
        given(postRepository.findAllForFeedBefore(eq(42L), any(Pageable.class))).willReturn(List.of());

        postService.getTimeline("all", 42L, 1L);

        then(postRepository).should().findAllForFeedBefore(eq(42L), any());
        then(postRepository).should(never()).findAllForFeed(any());
    }

    // WB分岐パス3（分岐1=T, 2=T）: followingフィード・カーソルなし → findFollowingFeed 呼び出し
    @Test
    void getTimeline_followingFeed_noCursor_callsFollowingFeed() {
        given(postRepository.findFollowingFeed(eq(1L), any(Pageable.class))).willReturn(List.of());

        postService.getTimeline("following", null, 1L);

        then(postRepository).should().findFollowingFeed(eq(1L), any());
        then(postRepository).should(never()).findFollowingFeedBefore(any(), any(), any());
    }

    // WB分岐パス4（分岐1=T, 2=F）: followingフィード・カーソルあり → findFollowingFeedBefore 呼び出し
    @Test
    void getTimeline_followingFeed_withCursor_callsFollowingFeedBefore() {
        given(postRepository.findFollowingFeedBefore(eq(1L), eq(42L), any(Pageable.class))).willReturn(List.of());

        postService.getTimeline("following", 42L, 1L);

        then(postRepository).should().findFollowingFeedBefore(eq(1L), eq(42L), any());
    }

    // WB分岐網羅（分岐4=F, 5=T）: 結果が空 → hasMore=false、バルク取得スキップ
    @Test
    void getTimeline_emptyResult_returnsEmptyResponse() {
        given(postRepository.findAllForFeed(any(Pageable.class))).willReturn(List.of());

        PagedResponse<PostResponse> res = postService.getTimeline("all", null, 1L);

        assertThat(res.items()).isEmpty();
        assertThat(res.hasMore()).isFalse();
        assertThat(res.nextCursor()).isNull();
        then(likeRepository).should(never()).countByPostIds(any());
    }

    // BB境界値（20件ちょうど）: hasMore=false
    @Test
    void getTimeline_exactPageSize_hasMoreFalse() {
        List<Post> posts = buildPosts(20);
        given(postRepository.findAllForFeed(any(Pageable.class))).willReturn(posts);
        stubBulkCountsForPosts(posts);

        PagedResponse<PostResponse> res = postService.getTimeline("all", null, 1L);

        assertThat(res.hasMore()).isFalse();
        assertThat(res.items()).hasSize(20);
    }

    // BB境界値（21件）: hasMore=true、items=20件、nextCursor=最後のid
    @Test
    void getTimeline_overPageSize_hasMoreTrue() {
        List<Post> posts = buildPosts(21);
        given(postRepository.findAllForFeed(any(Pageable.class))).willReturn(posts);
        stubBulkCountsForPosts(posts.subList(0, 20));

        PagedResponse<PostResponse> res = postService.getTimeline("all", null, 1L);

        assertThat(res.hasMore()).isTrue();
        assertThat(res.items()).hasSize(20);
        assertThat(res.nextCursor()).isEqualTo(posts.get(19).getId());
    }

    // WB命令網羅: likedByMe=true が正しく設定されること
    @Test
    void getTimeline_likedByMe_trueWhenInLikedSet() {
        List<Post> posts = buildPosts(1);
        Post p = posts.get(0);
        given(postRepository.findAllForFeed(any(Pageable.class))).willReturn(posts);
        given(likeRepository.findLikedPostIdsByUserIdAndPostIds(eq(1L), any())).willReturn(Set.of(p.getId()));
        List<Object[]> likeCounts = new ArrayList<>();
        likeCounts.add(new Object[]{p.getId(), 1L});
        given(likeRepository.countByPostIds(any())).willReturn(likeCounts);
        given(commentRepository.countByPostIds(any())).willReturn(List.of());
        given(followRepository.countFollowersByUserIds(any())).willReturn(List.of());
        given(followRepository.countFollowingByUserIds(any())).willReturn(List.of());
        given(followRepository.findFollowingIdsByFollowerIdAndUserIds(any(), any())).willReturn(Set.of());

        PagedResponse<PostResponse> res = postService.getTimeline("all", null, 1L);

        assertThat(res.items().get(0).likedByMe()).isTrue();
    }

    // ─── updatePost ───────────────────────────────────────────────────────

    // WB分岐網羅（分岐7=F, 8=T, 9=T）: オーナーが画像付きで更新 → 保存・レスポンス返却
    @Test
    void updatePost_success_withImages() {
        given(postRepository.findById(10L)).willReturn(Optional.of(post));
        given(postRepository.save(any(Post.class))).willReturn(post);
        given(likeRepository.existsByPostIdAndUserId(10L, 1L)).willReturn(false);
        stubBulkCountsForPost(10L, 1L);

        PostResponse res = postService.updatePost(10L,
            new UpdatePostRequest("edited", List.of("http://img.jpg")), 1L);

        assertThat(res.content()).isEqualTo("edited");
        then(postRepository).should().save(any());
    }

    // WB分岐網羅（分岐9=F）: 画像なしで更新 → images がクリアされる
    @Test
    void updatePost_success_clearImages() {
        given(postRepository.findById(10L)).willReturn(Optional.of(post));
        given(postRepository.save(any(Post.class))).willReturn(post);
        given(likeRepository.existsByPostIdAndUserId(10L, 1L)).willReturn(false);
        stubBulkCountsForPost(10L, 1L);

        postService.updatePost(10L, new UpdatePostRequest("edited", List.of()), 1L);

        then(postRepository).should().save(argThat(p -> p.getImages().isEmpty()));
    }

    // WB分岐網羅（分岐7=T）: 投稿未発見 → ResourceNotFoundException
    @Test
    void updatePost_notFound_throwsResourceNotFound() {
        given(postRepository.findById(999L)).willReturn(Optional.empty());

        assertThatThrownBy(() ->
            postService.updatePost(999L, new UpdatePostRequest("edit", List.of()), 1L)
        ).isInstanceOf(ResourceNotFoundException.class);
    }

    // WB分岐網羅（分岐8=F）: 他人が更新 → ForbiddenException
    @Test
    void updatePost_notOwner_throwsForbidden() {
        given(postRepository.findById(10L)).willReturn(Optional.of(post));

        assertThatThrownBy(() ->
            postService.updatePost(10L, new UpdatePostRequest("edit", List.of()), 99L)
        ).isInstanceOf(ForbiddenException.class);
    }

    // ─── deletePost ───────────────────────────────────────────────────────

    // WB分岐網羅（分岐10=T）: 投稿未発見 → ResourceNotFoundException
    @Test
    void deletePost_notFound_throwsResourceNotFound() {
        given(postRepository.findById(999L)).willReturn(Optional.empty());

        assertThatThrownBy(() ->
            postService.deletePost(999L, 1L)
        ).isInstanceOf(ResourceNotFoundException.class);
    }

    // WB分岐網羅（分岐11=F）: 他人が削除 → ForbiddenException
    @Test
    void deletePost_notOwner_throwsForbidden() {
        given(postRepository.findById(10L)).willReturn(Optional.of(post));

        assertThatThrownBy(() ->
            postService.deletePost(10L, 99L)
        ).isInstanceOf(ForbiddenException.class);
    }

    // WB分岐網羅（分岐10=F, 11=T）: オーナーが削除 → delete 呼び出し
    @Test
    void deletePost_owner_success() {
        given(postRepository.findById(10L)).willReturn(Optional.of(post));

        postService.deletePost(10L, 1L);

        then(postRepository).should().delete(post);
    }

    // ─── getLikedPostsByUser ──────────────────────────────────────────────

    // WB分岐網羅（分岐12=T）: いいねなし → List.of()
    @Test
    void getLikedPostsByUser_emptyLikes_returnsEmpty() {
        given(likeRepository.findByUserIdOrderByCreatedAtDesc(1L)).willReturn(List.of());

        List<PostResponse> res = postService.getLikedPostsByUser(1L, 1L);

        assertThat(res).isEmpty();
        then(likeRepository).should(never()).countByPostIds(any());
    }

    // WB分岐網羅（分岐12=F）: いいねあり → バルク取得後にリスト返却
    @Test
    void getLikedPostsByUser_withLikes_returnsPosts() {
        Like like = Like.builder().post(post).user(user).build();
        given(likeRepository.findByUserIdOrderByCreatedAtDesc(1L)).willReturn(List.of(like));
        stubBulkCountsForPosts(List.of(post));
        given(likeRepository.findLikedPostIdsByUserIdAndPostIds(eq(1L), any())).willReturn(Set.of(10L));

        List<PostResponse> res = postService.getLikedPostsByUser(1L, 1L);

        assertThat(res).hasSize(1);
        assertThat(res.get(0).likedByMe()).isTrue();
    }

    // WB分岐網羅（分岐13=T）: 投稿なし → List.of()
    @Test
    void getPostsByUser_emptyPosts_returnsEmpty() {
        given(postRepository.findByUserIdOrderByCreatedAtDesc(1L)).willReturn(List.of());

        List<PostResponse> res = postService.getPostsByUser(1L, 1L);

        assertThat(res).isEmpty();
    }

    // ─── ヘルパー ──────────────────────────────────────────────────────────

    private List<Post> buildPosts(int count) {
        return IntStream.rangeClosed(1, count)
            .mapToObj(i -> Post.builder()
                .id((long) i)
                .user(user)
                .content("post " + i)
                .images(new ArrayList<>())
                .build())
            .collect(Collectors.toList());
    }

    private void stubBulkCountsForPost(Long postId, Long authorId) {
        // toPostResponseSingle: countByPostIds (like+comment), countFollowers, countFollowing, findFollowingIds
        given(likeRepository.countByPostIds(any())).willReturn(List.of());
        given(commentRepository.countByPostIds(any())).willReturn(List.of());
        given(followRepository.countFollowersByUserIds(any())).willReturn(List.of());
        given(followRepository.countFollowingByUserIds(any())).willReturn(List.of());
        given(followRepository.findFollowingIdsByFollowerIdAndUserIds(any(), any())).willReturn(Set.of());
    }

    private void stubBulkCountsForPosts(List<Post> posts) {
        given(likeRepository.findLikedPostIdsByUserIdAndPostIds(any(), any())).willReturn(Set.of());
        given(likeRepository.countByPostIds(any())).willReturn(List.of());
        given(commentRepository.countByPostIds(any())).willReturn(List.of());
        given(followRepository.countFollowersByUserIds(any())).willReturn(List.of());
        given(followRepository.countFollowingByUserIds(any())).willReturn(List.of());
        given(followRepository.findFollowingIdsByFollowerIdAndUserIds(any(), any())).willReturn(Set.of());
    }
}
