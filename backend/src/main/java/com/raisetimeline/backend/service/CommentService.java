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
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

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
        return toCommentResponseSingle(comment, savedUser, postId);
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
        return toCommentResponseSingle(comment, comment.getUser(), comment.getPost().getId());
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
    public List<CommentResponse> getCommentsByUser(Long userId) {
        List<Comment> comments = commentRepository.findByUserIdOrderByCreatedAtDesc(userId);
        if (comments.isEmpty()) return List.of();

        Set<Long> authorIds = comments.stream()
            .map(c -> c.getUser().getId())
            .collect(Collectors.toSet());

        Map<Long, Long> followersCountMap = toCountMap(followRepository.countFollowersByUserIds(authorIds));
        Map<Long, Long> followingCountMap = toCountMap(followRepository.countFollowingByUserIds(authorIds));

        return comments.stream()
            .map(c -> toCommentResponse(c, c.getUser(), c.getPost().getId(), followersCountMap, followingCountMap))
            .toList();
    }

    @Transactional(readOnly = true)
    public List<CommentResponse> getCommentsByPost(Long postId) {
        List<Comment> comments = commentRepository.findByPostIdOrderByCreatedAtAsc(postId);
        if (comments.isEmpty()) return List.of();

        Set<Long> authorIds = comments.stream()
            .map(c -> c.getUser().getId())
            .collect(Collectors.toSet());

        Map<Long, Long> followersCountMap = toCountMap(followRepository.countFollowersByUserIds(authorIds));
        Map<Long, Long> followingCountMap = toCountMap(followRepository.countFollowingByUserIds(authorIds));

        return comments.stream()
            .map(c -> toCommentResponse(c, c.getUser(), postId, followersCountMap, followingCountMap))
            .toList();
    }

    private CommentResponse toCommentResponseSingle(Comment comment, User author, Long postId) {
        List<Long> aid = List.of(author.getId());
        Map<Long, Long> followersMap = toCountMap(followRepository.countFollowersByUserIds(aid));
        Map<Long, Long> followingMap = toCountMap(followRepository.countFollowingByUserIds(aid));
        return toCommentResponse(comment, author, postId, followersMap, followingMap);
    }

    private CommentResponse toCommentResponse(Comment comment, User author, Long postId,
                                              Map<Long, Long> followersCountMap,
                                              Map<Long, Long> followingCountMap) {
        long followersCount = followersCountMap.getOrDefault(author.getId(), 0L);
        long followingCount = followingCountMap.getOrDefault(author.getId(), 0L);
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

    private Map<Long, Long> toCountMap(List<Object[]> rows) {
        return rows.stream().collect(
            Collectors.toMap(r -> (Long) r[0], r -> (Long) r[1])
        );
    }
}
