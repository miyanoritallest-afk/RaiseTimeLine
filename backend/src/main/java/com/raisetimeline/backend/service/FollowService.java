package com.raisetimeline.backend.service;

import com.raisetimeline.backend.dto.response.FollowResponse;
import com.raisetimeline.backend.dto.response.UserResponse;
import com.raisetimeline.backend.entity.Follow;
import com.raisetimeline.backend.entity.User;
import com.raisetimeline.backend.exception.DuplicateResourceException;
import com.raisetimeline.backend.exception.ResourceNotFoundException;
import com.raisetimeline.backend.repository.FollowRepository;
import com.raisetimeline.backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class FollowService {

    private final FollowRepository followRepository;
    private final UserRepository userRepository;

    @Transactional
    public FollowResponse follow(Long targetUserId, Long currentUserId) {
        if (targetUserId.equals(currentUserId)) {
            throw new IllegalArgumentException("自分自身をフォローすることはできません");
        }
        if (!userRepository.existsById(targetUserId)) {
            throw new ResourceNotFoundException("ユーザーが見つかりません");
        }
        if (!followRepository.existsByFollowerIdAndFollowingId(currentUserId, targetUserId)) {
            User follower = userRepository.getReferenceById(currentUserId);
            User following = userRepository.getReferenceById(targetUserId);
            followRepository.save(Follow.builder().follower(follower).following(following).build());
        }
        return buildFollowResponse(targetUserId, currentUserId, true);
    }

    @Transactional
    public FollowResponse unfollow(Long targetUserId, Long currentUserId) {
        if (!userRepository.existsById(targetUserId)) {
            throw new ResourceNotFoundException("ユーザーが見つかりません");
        }
        followRepository.findByFollowerIdAndFollowingId(currentUserId, targetUserId)
            .ifPresent(followRepository::delete);
        return buildFollowResponse(targetUserId, currentUserId, false);
    }

    @Transactional(readOnly = true)
    public List<UserResponse> getFollowers(Long userId, Long currentUserId) {
        List<User> followers = followRepository.findFollowersByFollowingId(userId).stream()
            .map(Follow::getFollower)
            .toList();
        return toUserResponses(followers, currentUserId);
    }

    @Transactional(readOnly = true)
    public List<UserResponse> getFollowing(Long userId, Long currentUserId) {
        List<User> following = followRepository.findFollowingByFollowerId(userId).stream()
            .map(Follow::getFollowing)
            .toList();
        return toUserResponses(following, currentUserId);
    }

    private List<UserResponse> toUserResponses(List<User> users, Long currentUserId) {
        if (users.isEmpty()) return List.of();
        Set<Long> userIds = users.stream().map(User::getId).collect(Collectors.toSet());
        Set<Long> followingIds = followRepository
            .findFollowingIdsByFollowerIdAndUserIds(currentUserId, userIds);
        return users.stream()
            .map(u -> new UserResponse(
                u.getId(),
                u.getUsername(),
                u.getEmail(),
                u.getBio(),
                u.getAvatarUrl(),
                followRepository.countByFollowingId(u.getId()),
                followRepository.countByFollowerId(u.getId()),
                followingIds.contains(u.getId())
            ))
            .toList();
    }

    private FollowResponse buildFollowResponse(Long targetUserId, Long currentUserId, boolean isFollowing) {
        long followersCount = followRepository.countByFollowingId(targetUserId);
        long followingCount = followRepository.countByFollowerId(targetUserId);
        return new FollowResponse(targetUserId, followersCount, followingCount, isFollowing);
    }
}
