package com.raisetimeline.backend.dto.response;

public record FollowResponse(
    Long userId,
    long followersCount,
    long followingCount,
    boolean isFollowing
) {}
