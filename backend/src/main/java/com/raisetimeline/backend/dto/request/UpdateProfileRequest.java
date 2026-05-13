package com.raisetimeline.backend.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UpdateProfileRequest(
    @NotBlank @Size(max = 50) String username,
    @Size(max = 160) String bio,
    String avatarUrl
) {}
