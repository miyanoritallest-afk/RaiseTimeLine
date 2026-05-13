package com.raisetimeline.backend.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.List;

public record CreatePostRequest(
    @NotBlank @Size(max = 280) String content,
    List<String> imageUrls
) {}
