package com.feedapp.server.member;

public class ConflictException extends RuntimeException {

    public ConflictException(String message) {
        super(message);
    }
}
