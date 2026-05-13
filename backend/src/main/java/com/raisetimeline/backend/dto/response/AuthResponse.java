package com.raisetimeline.backend.dto.response;

public record AuthResponse(
    String token,
    UserResponse user
) {}
