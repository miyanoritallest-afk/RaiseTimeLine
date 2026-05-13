package com.raisetimeline.backend.service;

import com.raisetimeline.backend.dto.request.CreateCommentRequest;
import com.raisetimeline.backend.dto.request.UpdateCommentRequest;
import com.raisetimeline.backend.dto.response.CommentResponse;
import com.raisetimeline.backend.dto.response.UserResponse;
import com.raisetimeline.backend.entity.Comment;
import com.raisetimeline.backend.entity.Post;
import com.raisetimeline.backend.entity.User;
import com.raisetimeline.backend.exception.ForbiddenException;
import com.raisetimeline.backend.exception.ResourceNotFoundException;
import com.raisetimeline.backend.repository.CommentRepository;
import com.raisetimeline.backend.repository.FollowRepository;
import com.raisetimeline.backend.repository.PostRepository;
import com.raisetimeline.backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CommentService {

    private final CommentRepository commentRepository;
    private final PostRepository postRepository;
    private final UserRepository userRepository;
    private final FollowRepository followRepository;

    @Transactional
    public CommentResponse createComment(Long postId, CreateCommentRequest req, Long userId) {
        Post post = postRepository.findById(postId)
            .orElseThrow(() -> new ResourceNotFoundException("投稿が見つかりません"));
        User user = userRepository.getReferenceById(userId);
        Comment comment = Comment.builder()
            .post(post)
            .user(user)
            .content(req.content())
            .build();
        comment = commentRepository.save(comment);
        User savedUser = userRepository.findById(userId)
            .orElseThrow(() -> new ResourceNotFoundException("ユーザーが見つかりません"));
        return toCommentResponse(comment, savedUser, postId);
    }

    @Transactional
    public CommentResponse updateComment(Long commentId, UpdateCommentRequest req, Long currentUserId) {
        Comment comment = commentRepository.findById(commentId)
            .orElseThrow(() -> new ResourceNotFoundException("コメントが見つかりません"));
        if (!comment.getUser().getId().equals(currentUserId)) {
            throw new ForbiddenException("他のユーザーのコメントは編集できません");
        }
        comment.setContent(req.content());
        comment = commentRepository.save(comment);
        return toCommentResponse(comment, comment.getUser(), comment.getPost().getId());
    }

    @Transactional
    public void deleteComment(Long commentId, Long currentUserId) {
        Comment comment = commentRepository.findById(commentId)
            .orElseThrow(() -> new ResourceNotFoundException("コメントが見つかりません"));
        if (!comment.getUser().getId().equals(currentUserId)) {
            throw new ForbiddenException("他のユーザーのコメントは削除できません");
        }
        commentRepository.delete(comment);
    }

    @Transactional(readOnly = true)
    public List<CommentResponse> getCommentsByPost(Long postId) {
        return commentRepository.findByPostIdOrderByCreatedAtAsc(postId).stream()
            .map(c -> toCommentResponse(c, c.getUser(), postId))
            .toList();
    }

    private CommentResponse toCommentResponse(Comment comment, User author, Long postId) {
        long followersCount = followRepository.countByFollowingId(author.getId());
        long followingCount = followRepository.countByFollowerId(author.getId());
        UserResponse authorResponse = new UserResponse(
            author.getId(),
            author.getUsername(),
            author.getEmail(),
            author.getBio(),
            author.getAvatarUrl(),
            followersCount,
            followingCount,
            false
        );
        return new CommentResponse(
            comment.getId(),
            postId,
            authorResponse,
            comment.getContent(),
            comment.getCreatedAt(),
            comment.getUpdatedAt()
        );
    }
}
