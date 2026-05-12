package com.saveapenny.auth.service;

import com.saveapenny.auth.entity.RefreshToken;
import com.saveapenny.user.entity.User;

public interface RefreshTokenService {

    RefreshToken create(User user);

    RefreshToken validate(String rawToken);

    RefreshToken rotate(String rawToken);

    void revoke(String rawToken);

    void revokeAllByUser(User user);
}
