package com.raisetimeline.backend.service;

import com.raisetimeline.backend.dto.request.CreatePostRequest;
import com.raisetimeline.backend.dto.request.UpdatePostRequest;
import com.raisetimeline.backend.dto.response.PagedResponse;
import com.raisetimeline.backend.dto.response.PostResponse;
import com.raisetimeline.backend.dto.response.UserResponse;
import com.raisetimeline.backend.entity.Post;
import com.raisetimeline.backend.entity.PostImage;
import com.raisetimeline.backend.entity.User;
import com.raisetimeline.backend.exception.ForbiddenException;
import com.raisetimeline.backend.exception.ResourceNotFoundException;
import com.raisetimeline.backend.repository.CommentRepository;
import com.raisetimeline.backend.repository.FollowRepository;
import com.raisetimeline.backend.repository.LikeRepository;
import com.raisetimeline.backend.repository.PostRepository;
import com.raisetimeline.backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PostService {

    private static final int PAGE_SIZE = 20;

    private final PostRepository postRepository;
    private final LikeRepository likeRepository;
    private final CommentRepository commentRepository;
    private final FollowRepository followRepository;
    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public PagedResponse<PostResponse> getTimeline(String feed, Long cursor, Long currentUserId) {
        Pageable pageable = PageRequest.of(0, PAGE_SIZE + 1);

        List<Post> posts;
        if ("following".equals(feed)) {
            posts = (cursor == null)
                ? postRepository.findFollowingFeed(currentUserId, pageable)
                : postRepository.findFollowingFeedBefore(currentUserId, cursor, pageable);
        } else {
            posts = (cursor == null)
                ? postRepository.findAllForFeed(pageable)
                : postRepository.findAllForFeedBefore(cursor, pageable);
        }

        boolean hasMore = posts.size() > PAGE_SIZE;
        if (hasMore) posts = posts.subList(0, PAGE_SIZE);

        Long nextCursor = hasMore ? posts.get(posts.size() - 1).getId() : null;

        if (posts.isEmpty()) {
            return new PagedResponse<>(List.of(), nextCursor, hasMore);
        }

        Set<Long> postIds = posts.stream().map(Post::getId).collect(Collectors.toSet());
        Set<Long> authorIds = posts.stream().map(p -> p.getUser().getId()).collect(Collectors.toSet());

        Set<Long> likedPostIds = likeRepository.findLikedPostIdsByUserIdAndPostIds(currentUserId, postIds);
        Map<Long, Long> likeCountMap    = toCountMap(likeRepository.countByPostIds(postIds));
        Map<Long, Long> commentCountMap = toCountMap(commentRepository.countByPostIds(postIds));
        Map<Long, Long> followersMap    = toCountMap(followRepository.countFollowersByUserIds(authorIds));
        Map<Long, Long> followingMap    = toCountMap(followRepository.countFollowingByUserIds(authorIds));
        Set<Long> followingIds          = followRepository.findFollowingIdsByFollowerIdAndUserIds(currentUserId, authorIds);

        List<PostResponse> items = posts.stream()
            .map(p -> toPostResponse(p, likedPostIds.contains(p.getId()), currentUserId,
                likeCountMap, commentCountMap, followersMap, followingMap, followingIds))
            .toList();

        return new PagedResponse<>(items, nextCursor, hasMore);
    }

    @Transactional(readOnly = true)
    public PostResponse getPost(Long postId, Long currentUserId) {
        Post post = postRepository.findById(postId)
            .orElseThrow(() -> new ResourceNotFoundException("投稿が見つかりません"));
        boolean liked = likeRepository.existsByPostIdAndUserId(postId, currentUserId);
        return toPostResponseSingle(post, liked, currentUserId, post.getUser());
    }

    @Transactional
    public PostResponse createPost(CreatePostRequest req, Long userId) {
        User user = userRepository.getReferenceById(userId);
        Post post = Post.builder()
            .user(user)
            .content(req.content())
            .build();
        if (req.imageUrls() != null && !req.imageUrls().isEmpty()) {
            List<PostImage> images = new ArrayList<>();
            for (int i = 0; i < req.imageUrls().size(); i++) {
                images.add(PostImage.builder()
                    .post(post)
                    .imageUrl(req.imageUrls().get(i))
                    .displayOrder((short) (i + 1))
                    .build());
            }
            post.setImages(images);
        }
        post = postRepository.save(post);
        User savedUser = userRepository.findById(userId)
            .orElseThrow(() -> new ResourceNotFoundException("ユーザーが見つかりません"));
        return toPostResponseSingle(post, false, userId, savedUser);
    }

    @Transactional
    public PostResponse updatePost(Long postId, UpdatePostRequest req, Long currentUserId) {
        Post post = postRepository.findById(postId)
            .orElseThrow(() -> new ResourceNotFoundException("投稿が見つかりません"));
        if (!post.getUser().getId().equals(currentUserId)) {
            throw new ForbiddenException("他のユーザーの投稿は編集できません");
        }
        post.setContent(req.content());
        post.getImages().clear();
        if (req.imageUrls() != null && !req.imageUrls().isEmpty()) {
            for (int i = 0; i < req.imageUrls().size(); i++) {
                post.getImages().add(PostImage.builder()
                    .post(post)
                    .imageUrl(req.imageUrls().get(i))
                    .displayOrder((short) (i + 1))
                    .build());
            }
        }
        post = postRepository.save(post);
        boolean liked = likeRepository.existsByPostIdAndUserId(postId, currentUserId);
        return toPostResponseSingle(post, liked, currentUserId, post.getUser());
    }

    @Transactional
    public void deletePost(Long postId, Long currentUserId) {
        Post post = postRepository.findById(postId)
            .orElseThrow(() -> new ResourceNotFoundException("投稿が見つかりません"));
        if (!post.getUser().getId().equals(currentUserId)) {
            throw new ForbiddenException("他のユーザーの投稿は削除できません");
        }
        postRepository.delete(post);
    }

    @Transactional(readOnly = true)
    public List<PostResponse> getPostsByUser(Long userId, Long currentUserId) {
        List<Post> posts = postRepository.findByUserIdOrderByCreatedAtDesc(userId);
        if (posts.isEmpty()) return List.of();

        Set<Long> postIds = posts.stream().map(Post::getId).collect(Collectors.toSet());
        Set<Long> authorIds = posts.stream().map(p -> p.getUser().getId()).collect(Collectors.toSet());

        Set<Long> likedPostIds          = likeRepository.findLikedPostIdsByUserIdAndPostIds(currentUserId, postIds);
        Map<Long, Long> likeCountMap    = toCountMap(likeRepository.countByPostIds(postIds));
        Map<Long, Long> commentCountMap = toCountMap(commentRepository.countByPostIds(postIds));
        Map<Long, Long> followersMap    = toCountMap(followRepository.countFollowersByUserIds(authorIds));
        Map<Long, Long> followingMap    = toCountMap(followRepository.countFollowingByUserIds(authorIds));
        Set<Long> followingIds          = followRepository.findFollowingIdsByFollowerIdAndUserIds(currentUserId, authorIds);

        return posts.stream()
            .map(p -> toPostResponse(p, likedPostIds.contains(p.getId()), currentUserId,
                likeCountMap, commentCountMap, followersMap, followingMap, followingIds))
            .toList();
    }

    private PostResponse toPostResponseSingle(Post post, boolean likedByMe, Long currentUserId, User author) {
        List<Long> pid = List.of(post.getId());
        List<Long> aid = List.of(author.getId());
        Map<Long, Long> likeCountMap    = toCountMap(likeRepository.countByPostIds(pid));
        Map<Long, Long> commentCountMap = toCountMap(commentRepository.countByPostIds(pid));
        Map<Long, Long> followersMap    = toCountMap(followRepository.countFollowersByUserIds(aid));
        Map<Long, Long> followingMap    = toCountMap(followRepository.countFollowingByUserIds(aid));
        Set<Long> followingIds          = followRepository.findFollowingIdsByFollowerIdAndUserIds(currentUserId, aid);
        return toPostResponse(post, likedByMe, currentUserId,
            likeCountMap, commentCountMap, followersMap, followingMap, followingIds);
    }

    PostResponse toPostResponse(Post post, boolean likedByMe, Long currentUserId,
                                Map<Long, Long> likeCountMap,
                                Map<Long, Long> commentCountMap,
                                Map<Long, Long> followersCountMap,
                                Map<Long, Long> followingCountMap,
                                Set<Long> followingIds) {
        User author = post.getUser();
        long followersCount = followersCountMap.getOrDefault(author.getId(), 0L);
        long followingCount = followingCountMap.getOrDefault(author.getId(), 0L);
        boolean isFollowing = followingIds.contains(author.getId());

        UserResponse authorResponse = new UserResponse(
            author.getId(),
            author.getUsername(),
            author.getEmail(),
            author.getBio(),
            author.getAvatarUrl(),
            followersCount,
            followingCount,
            isFollowing
        );

        List<String> imageUrls = post.getImages().stream()
            .map(PostImage::getImageUrl)
            .toList();

        long likeCount    = likeCountMap.getOrDefault(post.getId(), 0L);
        long commentCount = commentCountMap.getOrDefault(post.getId(), 0L);

        return new PostResponse(
            post.getId(),
            authorResponse,
            post.getContent(),
            imageUrls,
            likeCount,
            commentCount,
            likedByMe,
            post.getCreatedAt(),
            post.getUpdatedAt()
        );
    }

    private Map<Long, Long> toCountMap(List<Object[]> rows) {
        return rows.stream().collect(
            Collectors.toMap(r -> (Long) r[0], r -> (Long) r[1])
        );
    }
}
