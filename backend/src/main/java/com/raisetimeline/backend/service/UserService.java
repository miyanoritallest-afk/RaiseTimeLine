package com.raisetimeline.backend.service;

import com.raisetimeline.backend.dto.request.UpdateProfileRequest;
import com.raisetimeline.backend.dto.response.UserResponse;
import com.raisetimeline.backend.entity.User;
import com.raisetimeline.backend.exception.ForbiddenException;
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
public class UserService {

    private final UserRepository userRepository;
    private final FollowRepository followRepository;

    @Transactional(readOnly = true)
    public UserResponse getUserProfile(Long userId, Long currentUserId) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new ResourceNotFoundException("ユーザーが見つかりません"));
        return toUserResponse(user, currentUserId);
    }

    @Transactional(readOnly = true)
    public List<UserResponse> searchUsers(String query, Long currentUserId) {
        if (query == null || query.isBlank()) return List.of();
        List<User> users = userRepository.findByUsernameContainingIgnoreCase(query);
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

    @Transactional
    public UserResponse updateProfile(Long userId, UpdateProfileRequest req, Long currentUserId) {
        if (!userId.equals(currentUserId)) {
            throw new ForbiddenException("他のユーザーのプロフィールは編集できません");
        }
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new ResourceNotFoundException("ユーザーが見つかりません"));
        user.setUsername(req.username());
        if (req.bio() != null) {
            user.setBio(req.bio());
        }
        if (req.avatarUrl() != null) {
            user.setAvatarUrl(req.avatarUrl());
        }
        user = userRepository.save(user);
        return toUserResponse(user, currentUserId);
    }

    private UserResponse toUserResponse(User user, Long currentUserId) {
        long followersCount = followRepository.countByFollowingId(user.getId());
        long followingCount = followRepository.countByFollowerId(user.getId());
        boolean isFollowing = !user.getId().equals(currentUserId)
            && followRepository.existsByFollowerIdAndFollowingId(currentUserId, user.getId());
        return new UserResponse(
            user.getId(),
            user.getUsername(),
            user.getEmail(),
            user.getBio(),
            user.getAvatarUrl(),
            followersCount,
            followingCount,
            isFollowing
        );
    }
}
