package com.saveapenny.auth.service;

import com.saveapenny.user.entity.User;
import java.util.UUID;

public interface JwtService {

    String generateAccessToken(User user);

    boolean isAccessTokenValid(String token);

    UUID extractUserId(String token);

    String extractEmail(String token);

    long getAccessTokenExpirySeconds();
}
