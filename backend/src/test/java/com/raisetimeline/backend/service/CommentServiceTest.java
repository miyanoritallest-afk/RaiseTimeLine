package com.raisetimeline.backend.service;

import com.raisetimeline.backend.dto.request.CreateCommentRequest;
import com.raisetimeline.backend.dto.request.UpdateCommentRequest;
import com.raisetimeline.backend.dto.response.CommentResponse;
import com.raisetimeline.backend.entity.Comment;
import com.raisetimeline.backend.entity.Post;
import com.raisetimeline.backend.entity.User;
import com.raisetimeline.backend.exception.ForbiddenException;
import com.raisetimeline.backend.exception.ResourceNotFoundException;
import com.raisetimeline.backend.repository.CommentRepository;
import com.raisetimeline.backend.repository.FollowRepository;
import com.raisetimeline.backend.repository.PostRepository;
import com.raisetimeline.backend.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;

@ExtendWith(MockitoExtension.class)
class CommentServiceTest {

    @Mock CommentRepository commentRepository;
    @Mock PostRepository postRepository;
    @Mock UserRepository userRepository;
    @Mock FollowRepository followRepository;

    @InjectMocks CommentService commentService;

    private User alice;
    private User bob;
    private Post post;
    private Comment comment;

    @BeforeEach
    void setUp() {
        alice = User.builder().id(1L).username("alice").email("alice@example.com").build();
        bob   = User.builder().id(2L).username("bob").email("bob@example.com").build();
        post  = Post.builder().id(10L).user(alice).content("post content").build();
        comment = Comment.builder().id(20L).post(post).user(alice).content("original comment").build();
    }

    // ─── createComment ────────────────────────────────────────────────────

    // BB同値分割（有効）/ WB分岐1=F: 投稿が存在 → CommentResponse が返される
    @Test
    void createComment_postExists_success() {
        given(postRepository.findById(10L)).willReturn(Optional.of(post));
        given(userRepository.getReferenceById(1L)).willReturn(alice);
        given(commentRepository.save(any(Comment.class))).willReturn(comment);
        given(userRepository.findById(1L)).willReturn(Optional.of(alice));
        given(followRepository.countFollowersByUserIds(any())).willReturn(List.of());
        given(followRepository.countFollowingByUserIds(any())).willReturn(List.of());

        CommentResponse res = commentService.createComment(10L,
            new CreateCommentRequest("original comment"), 1L);

        assertThat(res.postId()).isEqualTo(10L);
        assertThat(res.content()).isEqualTo("original comment");
        assertThat(res.author().username()).isEqualTo("alice");
    }

    // BB同値分割（無効）/ WB分岐1=T: 投稿未発見 → ResourceNotFoundException
    @Test
    void createComment_postNotFound_throwsResourceNotFound() {
        given(postRepository.findById(999L)).willReturn(Optional.empty());

        assertThatThrownBy(() ->
            commentService.createComment(999L, new CreateCommentRequest("hi"), 1L)
        ).isInstanceOf(ResourceNotFoundException.class);
    }

    // ─── updateComment ────────────────────────────────────────────────────

    // WB分岐網羅（分岐2=F, 3=T）: オーナーが更新 → content が変更され save 呼び出し
    @Test
    void updateComment_owner_success() {
        given(commentRepository.findById(20L)).willReturn(Optional.of(comment));
        given(commentRepository.save(any(Comment.class))).willReturn(comment);
        given(followRepository.countFollowersByUserIds(any())).willReturn(List.of());
        given(followRepository.countFollowingByUserIds(any())).willReturn(List.of());

        CommentResponse res = commentService.updateComment(20L,
            new UpdateCommentRequest("updated comment"), 1L);

        then(commentRepository).should().save(argThat(c -> c.getContent().equals("updated comment")));
        assertThat(res).isNotNull();
    }

    // WB分岐網羅（分岐2=T）: コメント未発見 → ResourceNotFoundException
    @Test
    void updateComment_notFound_throwsResourceNotFound() {
        given(commentRepository.findById(999L)).willReturn(Optional.empty());

        assertThatThrownBy(() ->
            commentService.updateComment(999L, new UpdateCommentRequest("edit"), 1L)
        ).isInstanceOf(ResourceNotFoundException.class);
    }

    // WB分岐網羅（分岐3=F）: 他人が更新 → ForbiddenException
    @Test
    void updateComment_notOwner_throwsForbidden() {
        given(commentRepository.findById(20L)).willReturn(Optional.of(comment));

        assertThatThrownBy(() ->
            commentService.updateComment(20L, new UpdateCommentRequest("hack"), 2L)
        ).isInstanceOf(ForbiddenException.class);
    }

    // ─── deleteComment ────────────────────────────────────────────────────

    // WB分岐網羅（分岐4=F, 5=T）: オーナーが削除 → delete 呼び出し
    @Test
    void deleteComment_owner_success() {
        given(commentRepository.findById(20L)).willReturn(Optional.of(comment));

        commentService.deleteComment(20L, 1L);

        then(commentRepository).should().delete(comment);
    }

    // WB分岐網羅（分岐4=T）: コメント未発見 → ResourceNotFoundException
    @Test
    void deleteComment_notFound_throwsResourceNotFound() {
        given(commentRepository.findById(999L)).willReturn(Optional.empty());

        assertThatThrownBy(() ->
            commentService.deleteComment(999L, 1L)
        ).isInstanceOf(ResourceNotFoundException.class);
    }

    // WB分岐網羅（分岐5=F）: 他人が削除 → ForbiddenException
    @Test
    void deleteComment_notOwner_throwsForbidden() {
        given(commentRepository.findById(20L)).willReturn(Optional.of(comment));

        assertThatThrownBy(() ->
            commentService.deleteComment(20L, 2L)
        ).isInstanceOf(ForbiddenException.class);
    }

    // ─── getCommentsByPost ────────────────────────────────────────────────

    // WB分岐網羅（分岐6=T）: コメントなし → List.of()（バルク取得スキップ）
    @Test
    void getCommentsByPost_noComments_returnsEmpty() {
        given(commentRepository.findByPostIdOrderByCreatedAtAsc(10L)).willReturn(List.of());

        List<CommentResponse> res = commentService.getCommentsByPost(10L);

        assertThat(res).isEmpty();
        then(followRepository).should(never()).countFollowersByUserIds(any());
    }

    // WB分岐網羅（分岐6=F）: コメントあり → リスト返却・followRepository 呼び出し
    @Test
    void getCommentsByPost_withComments_returnsList() {
        given(commentRepository.findByPostIdOrderByCreatedAtAsc(10L)).willReturn(List.of(comment));
        given(followRepository.countFollowersByUserIds(any())).willReturn(List.of());
        given(followRepository.countFollowingByUserIds(any())).willReturn(List.of());

        List<CommentResponse> res = commentService.getCommentsByPost(10L);

        assertThat(res).hasSize(1);
        then(followRepository).should().countFollowersByUserIds(any());
    }

    // ─── getCommentsByUser ────────────────────────────────────────────────

    // WB分岐網羅（分岐7=T）: コメントなし → List.of()
    @Test
    void getCommentsByUser_noComments_returnsEmpty() {
        given(commentRepository.findByUserIdOrderByCreatedAtDesc(1L)).willReturn(List.of());

        List<CommentResponse> res = commentService.getCommentsByUser(1L);

        assertThat(res).isEmpty();
    }

    // WB分岐網羅（分岐7=F）: コメントあり → リスト返却
    @Test
    void getCommentsByUser_withComments_returnsList() {
        given(commentRepository.findByUserIdOrderByCreatedAtDesc(1L)).willReturn(List.of(comment));
        given(followRepository.countFollowersByUserIds(any())).willReturn(List.of());
        given(followRepository.countFollowingByUserIds(any())).willReturn(List.of());

        List<CommentResponse> res = commentService.getCommentsByUser(1L);

        assertThat(res).hasSize(1);
        assertThat(res.get(0).author().username()).isEqualTo("alice");
    }
}
