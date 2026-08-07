package com.saveapenny.config.security;

import java.util.Set;
import java.util.UUID;

public record CurrentUserPrincipal(UUID userId, Set<String> roles) {

    public CurrentUserPrincipal(UUID userId) {
        this(userId, Set.of());
    }
}
