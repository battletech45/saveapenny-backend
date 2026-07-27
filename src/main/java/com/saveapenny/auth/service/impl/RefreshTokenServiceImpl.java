package com.saveapenny.auth.service.impl;

import com.saveapenny.auth.entity.RefreshToken;
import com.saveapenny.auth.exception.InvalidRefreshTokenException;
import com.saveapenny.auth.exception.RefreshTokenExpiredException;
import com.saveapenny.auth.repository.RefreshTokenRepository;
import com.saveapenny.auth.service.RefreshTokenService;
import com.saveapenny.user.entity.User;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.Base64;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class RefreshTokenServiceImpl implements RefreshTokenService {

    private static final int TOKEN_RANDOM_BYTES = 64;

    /**
     * A refresh token presented again this soon after it was rotated is treated as a
     * legitimate concurrent caller (proactive-refresh races, a dropped response that
     * the client retried) rather than reuse of a stolen token, and gets the pair that
     * already replaced it instead of a hard 401. Reuse past this window revokes the
     * whole rotation family — see rotate().
     */
    private static final Duration REUSE_GRACE_WINDOW = Duration.ofSeconds(5);

    private final RefreshTokenRepository refreshTokenRepository;
    private final SecureRandom secureRandom = new SecureRandom();
    private final long refreshTokenExpiryDays;

    public RefreshTokenServiceImpl(
            RefreshTokenRepository refreshTokenRepository,
            @Value("${security.jwt.refresh-token-expiry-days:7}") long refreshTokenExpiryDays) {
        this.refreshTokenRepository = refreshTokenRepository;
        this.refreshTokenExpiryDays = refreshTokenExpiryDays;
    }

    @Override
    public RefreshToken create(User user) {
        RefreshToken refreshToken = RefreshToken.builder()
                .userId(user.getId())
                .token(generateToken())
                .expiryDate(OffsetDateTime.now().plusDays(refreshTokenExpiryDays))
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
        RefreshToken existingToken = refreshTokenRepository.findByTokenForUpdate(rawToken)
                .orElseThrow(InvalidRefreshTokenException::new);

        if (Boolean.TRUE.equals(existingToken.getRevoked())) {
            return handleReuse(existingToken);
        }

        if (existingToken.getExpiryDate().isBefore(OffsetDateTime.now())) {
            throw new RefreshTokenExpiredException();
        }

        return rotateToken(existingToken);
    }

    /**
     * The presented token was already rotated. If that happened within the grace
     * window and its replacement is still good, this is almost certainly a legitimate
     * concurrent caller (a proactive-refresh race, or a client retry after a dropped
     * response) — hand back the pair that already replaced it instead of a hard 401.
     * Otherwise this is the actual reuse-detection signal: kill the whole rotation
     * family, since a token this stale being replayed means the chain is compromised.
     */
    private RefreshToken handleReuse(RefreshToken existingToken) {
        OffsetDateTime revokedAt = existingToken.getRevokedAt();
        UUID replacedByTokenId = existingToken.getReplacedByTokenId();
        if (revokedAt != null
                && replacedByTokenId != null
                && revokedAt.isAfter(OffsetDateTime.now().minus(REUSE_GRACE_WINDOW))) {
            Optional<RefreshToken> replacement = refreshTokenRepository.findById(replacedByTokenId)
                    .filter(token -> !Boolean.TRUE.equals(token.getRevoked()))
                    .filter(token -> token.getExpiryDate().isAfter(OffsetDateTime.now()));
            if (replacement.isPresent()) {
                return replacement.get();
            }
        }

        revokeFamily(existingToken.getFamilyId());
        throw new InvalidRefreshTokenException();
    }

    private RefreshToken rotateToken(RefreshToken existingToken) {
        RefreshToken rotated = RefreshToken.builder()
                .id(UUID.randomUUID())
                .userId(existingToken.getUserId())
                .token(generateToken())
                .expiryDate(OffsetDateTime.now().plusDays(refreshTokenExpiryDays))
                .revoked(false)
                .familyId(existingToken.getFamilyId())
                .build();
        RefreshToken saved = refreshTokenRepository.save(rotated);

        existingToken.setRevoked(true);
        existingToken.setRevokedAt(OffsetDateTime.now());
        existingToken.setReplacedByTokenId(saved.getId());
        refreshTokenRepository.save(existingToken);

        return saved;
    }

    private void revokeFamily(UUID familyId) {
        List<RefreshToken> tokens = refreshTokenRepository.findAllByFamilyIdAndRevokedFalse(familyId);
        OffsetDateTime now = OffsetDateTime.now();
        for (RefreshToken token : tokens) {
            token.setRevoked(true);
            token.setRevokedAt(now);
        }
        refreshTokenRepository.saveAll(tokens);
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
