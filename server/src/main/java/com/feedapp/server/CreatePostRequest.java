package com.feedapp.server;

public record CreatePostRequest(String title, String content, String author) {
}
