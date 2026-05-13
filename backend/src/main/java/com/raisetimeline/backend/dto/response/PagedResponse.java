package com.raisetimeline.backend.dto.response;

import java.util.List;

public record PagedResponse<T>(
    List<T> items,
    Long nextCursor,
    boolean hasMore
) {}
