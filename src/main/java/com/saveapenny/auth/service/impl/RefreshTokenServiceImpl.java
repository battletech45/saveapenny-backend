package com.saveapenny.auth.service.impl;

import com.saveapenny.auth.entity.RefreshToken;
import com.saveapenny.auth.exception.InvalidRefreshTokenException;
import com.saveapenny.auth.exception.RefreshTokenExpiredException;
import com.saveapenny.auth.repository.RefreshTokenRepository;
import com.saveapenny.auth.service.RefreshTokenService;
import com.saveapenny.user.entity.User;
import java.security.SecureRandom;
import java.time.OffsetDateTime;
import java.util.Base64;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class RefreshTokenServiceImpl implements RefreshTokenService {

    private static final long REFRESH_TOKEN_EXPIRY_DAYS = 7L;
    private static final int TOKEN_RANDOM_BYTES = 64;

    private final RefreshTokenRepository refreshTokenRepository;
    private final SecureRandom secureRandom = new SecureRandom();

    public RefreshTokenServiceImpl(RefreshTokenRepository refreshTokenRepository) {
        this.refreshTokenRepository = refreshTokenRepository;
    }

    @Override
    public RefreshToken create(User user) {
        RefreshToken refreshToken = RefreshToken.builder()
                .userId(user.getId())
                .token(generateToken())
                .expiryDate(OffsetDateTime.now().plusDays(REFRESH_TOKEN_EXPIRY_DAYS))
                .revoked(false)
                .build();
        return refreshTokenRepository.save(refreshToken);
    }

    @Override
    public RefreshToken validate(String rawToken) {
        RefreshToken refreshToken = refreshTokenRepository.findByToken(rawToken)
                .orElseThrow(InvalidRefreshTokenException::new);

        if (Boolean.TRUE.equals(refreshToken.getRevoked())) {
            throw new InvalidRefreshTokenException();
        }

        if (refreshToken.getExpiryDate().isBefore(OffsetDateTime.now())) {
            throw new RefreshTokenExpiredException();
        }

        return refreshToken;
    }

    @Override
    public RefreshToken rotate(String rawToken) {
        RefreshToken existingToken = validate(rawToken);
        existingToken.setRevoked(true);
        refreshTokenRepository.save(existingToken);

        RefreshToken rotated = RefreshToken.builder()
                .userId(existingToken.getUserId())
                .token(generateToken())
                .expiryDate(OffsetDateTime.now().plusDays(REFRESH_TOKEN_EXPIRY_DAYS))
                .revoked(false)
                .build();
        return refreshTokenRepository.save(rotated);
    }

    @Override
    public void revoke(String rawToken) {
        refreshTokenRepository.findByToken(rawToken).ifPresent(token -> {
            token.setRevoked(true);
            refreshTokenRepository.save(token);
        });
    }

    @Override
    public void revokeAllByUser(User user) {
        List<RefreshToken> tokens = refreshTokenRepository.findAllByUserIdAndRevokedFalse(user.getId());
        for (RefreshToken token : tokens) {
            token.setRevoked(true);
        }
        refreshTokenRepository.saveAll(tokens);
    }

    private String generateToken() {
        byte[] bytes = new byte[TOKEN_RANDOM_BYTES];
        secureRandom.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
}
