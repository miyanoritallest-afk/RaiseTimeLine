package com.raisetimeline.backend.dto.response;

import java.time.LocalDateTime;

public record CommentResponse(
    Long id,
    Long postId,
    UserResponse author,
    String content,
    LocalDateTime createdAt,
    LocalDateTime updatedAt
) {}
