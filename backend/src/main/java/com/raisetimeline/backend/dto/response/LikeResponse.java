package com.raisetimeline.backend.dto.response;

public record LikeResponse(
    Long postId,
    long likeCount,
    boolean likedByMe
) {}
