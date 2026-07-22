package com.feedapp.server;

import java.util.Optional;

import org.springframework.stereotype.Component;

@Component
public class RefreshTokenStore {

    public void save(String sid, String jti) {
    }

    public Optional<String> find(String sid) {
        return Optional.empty();
    }
}
