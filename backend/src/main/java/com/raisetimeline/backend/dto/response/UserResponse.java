package com.raisetimeline.backend.dto.response;

public record UserResponse(
    Long id,
    String username,
    String email,
    String bio,
    String avatarUrl,
    long followersCount,
    long followingCount,
    boolean isFollowing
) {}
