package com.feedapp.server.post;

import java.util.List;

public record PostWindow(List<Post> content, boolean hasNext, boolean hasPrevious) {
}
