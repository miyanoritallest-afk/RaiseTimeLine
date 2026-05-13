package com.raisetimeline.backend.dto.response;

import java.time.LocalDateTime;
import java.util.List;

public record PostResponse(
    Long id,
    UserResponse author,
    String content,
    List<String> imageUrls,
    long likeCount,
    long commentCount,
    boolean likedByMe,
    LocalDateTime createdAt,
    LocalDateTime updatedAt
) {}
