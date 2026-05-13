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
import java.util.HashSet;
import java.util.List;
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

        Set<Long> postIds = posts.stream().map(Post::getId).collect(Collectors.toSet());
        Set<Long> likedPostIds = postIds.isEmpty()
            ? Set.of()
            : likeRepository.findLikedPostIdsByUserIdAndPostIds(currentUserId, postIds);

        List<PostResponse> items = posts.stream()
            .map(p -> toPostResponse(p, likedPostIds.contains(p.getId()), currentUserId))
            .toList();

        return new PagedResponse<>(items, nextCursor, hasMore);
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
        return toPostResponse(post, false, userId);
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
        return toPostResponse(post, liked, currentUserId);
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
        Set<Long> likedPostIds = likeRepository
            .findLikedPostIdsByUserIdAndPostIds(currentUserId, postIds);

        return posts.stream()
            .map(p -> toPostResponse(p, likedPostIds.contains(p.getId()), currentUserId))
            .toList();
    }

    PostResponse toPostResponse(Post post, boolean likedByMe, Long currentUserId) {
        User author = post.getUser();
        long followersCount = followRepository.countByFollowingId(author.getId());
        long followingCount = followRepository.countByFollowerId(author.getId());
        boolean isFollowing = !author.getId().equals(currentUserId)
            && followRepository.existsByFollowerIdAndFollowingId(currentUserId, author.getId());

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

        long likeCount = likeRepository.countByPostId(post.getId());
        long commentCount = commentRepository.countByPostId(post.getId());

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
}
