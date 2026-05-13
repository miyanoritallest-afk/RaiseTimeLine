package com.raisetimeline.backend.dto.response;

public record AuthResponse(
    String accessToken,
    String refreshToken,
    Long expiresIn,
    UserResponse user
) {}
