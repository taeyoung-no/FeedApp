package com.feedapp.server.post;

import java.util.List;

public record CursorPage<T>(
        List<T> content,
        String nextCursor,
        String prevCursor,
        boolean hasNext,
        boolean hasPrevious
) {
}
