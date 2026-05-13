package com.raisetimeline.backend.dto.request;

import jakarta.validation.constraints.NotBlank;

public record UpdateCommentRequest(
    @NotBlank String content
) {}
