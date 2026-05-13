package com.raisetimeline.backend.service;

import com.raisetimeline.backend.dto.response.LikeResponse;
import com.raisetimeline.backend.entity.Like;
import com.raisetimeline.backend.entity.Post;
import com.raisetimeline.backend.entity.User;
import com.raisetimeline.backend.exception.ResourceNotFoundException;
import com.raisetimeline.backend.repository.LikeRepository;
import com.raisetimeline.backend.repository.PostRepository;
import com.raisetimeline.backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class LikeService {

    private final LikeRepository likeRepository;
    private final PostRepository postRepository;
    private final UserRepository userRepository;

    @Transactional
    public LikeResponse likePost(Long postId, Long userId) {
        if (!postRepository.existsById(postId)) {
            throw new ResourceNotFoundException("投稿が見つかりません");
        }
        if (!likeRepository.existsByPostIdAndUserId(postId, userId)) {
            Post post = postRepository.getReferenceById(postId);
            User user = userRepository.getReferenceById(userId);
            likeRepository.save(Like.builder().post(post).user(user).build());
        }
        long likeCount = likeRepository.countByPostId(postId);
        return new LikeResponse(postId, likeCount, true);
    }

    @Transactional
    public LikeResponse unlikePost(Long postId, Long userId) {
        if (!postRepository.existsById(postId)) {
            throw new ResourceNotFoundException("投稿が見つかりません");
        }
        likeRepository.findByPostIdAndUserId(postId, userId)
            .ifPresent(likeRepository::delete);
        long likeCount = likeRepository.countByPostId(postId);
        return new LikeResponse(postId, likeCount, false);
    }
}
