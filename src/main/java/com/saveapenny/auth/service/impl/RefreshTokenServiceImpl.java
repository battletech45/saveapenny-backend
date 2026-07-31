package com.saveapenny.auth.service.impl;

import com.saveapenny.auth.entity.RefreshToken;
import com.saveapenny.auth.exception.InvalidRefreshTokenException;
import com.saveapenny.auth.exception.RefreshTokenExpiredException;
import com.saveapenny.auth.repository.RefreshTokenRepository;
import com.saveapenny.auth.service.RefreshTokenService;
import com.saveapenny.user.entity.User;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.Base64;
import java.util.HexFormat;
import java.util.List;
import java.util.UUID;
import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class RefreshTokenServiceImpl implements RefreshTokenService {

    private static final int TOKEN_RANDOM_BYTES = 64;
    private static final int GCM_IV_BYTES = 12;
    private static final int GCM_TAG_BITS = 128;
    private static final String TEST_SECRET = "0123456789012345678901234567890123456789012345678901234567890123";

    private static final Duration REUSE_GRACE_WINDOW = Duration.ofSeconds(5);

    private final RefreshTokenRepository refreshTokenRepository;
    private final SecureRandom secureRandom = new SecureRandom();
    private final long refreshTokenExpiryDays;
    private final SecretKeySpec replayEncryptionKey;

    @Autowired
    public RefreshTokenServiceImpl(
            RefreshTokenRepository refreshTokenRepository,
            @Value("${security.jwt.secret}") String jwtSecret,
            @Value("${security.jwt.refresh-token-expiry-days:7}") long refreshTokenExpiryDays) {
        this.refreshTokenRepository = refreshTokenRepository;
        this.refreshTokenExpiryDays = refreshTokenExpiryDays;
        this.replayEncryptionKey = new SecretKeySpec(deriveEncryptionKey(jwtSecret), "AES");
    }

    RefreshTokenServiceImpl(RefreshTokenRepository refreshTokenRepository, long refreshTokenExpiryDays) {
        this(refreshTokenRepository, TEST_SECRET, refreshTokenExpiryDays);
    }

    @Override
    public RefreshToken create(User user) {
        String rawToken = generateToken();
        RefreshToken refreshToken = RefreshToken.builder()
                .userId(user.getId())
                .token(rawToken)
                .tokenHash(hashToken(rawToken))
                .expiryDate(OffsetDateTime.now().plusDays(refreshTokenExpiryDays))
                .revoked(false)
                .build();
        return refreshTokenRepository.save(refreshToken);
    }

    @Override
    public RefreshToken validate(String rawToken) {
        RefreshToken refreshToken = resolveStoredToken(rawToken, false)
                .orElseThrow(InvalidRefreshTokenException::new);

        if (Boolean.TRUE.equals(refreshToken.getRevoked())) {
            throw new InvalidRefreshTokenException();
        }

        if (refreshToken.getExpiryDate().isBefore(OffsetDateTime.now())) {
            throw new RefreshTokenExpiredException();
        }

        refreshToken.setToken(rawToken);
        return refreshToken;
    }

    @Override
    public RefreshToken rotate(String rawToken) {
        RefreshToken existingToken = resolveStoredToken(rawToken, true)
                .orElseThrow(InvalidRefreshTokenException::new);

        if (Boolean.TRUE.equals(existingToken.getRevoked())) {
            return handleReuse(existingToken);
        }

        if (existingToken.getExpiryDate().isBefore(OffsetDateTime.now())) {
            throw new RefreshTokenExpiredException();
        }

        return rotateToken(existingToken);
    }

    private RefreshToken handleReuse(RefreshToken existingToken) {
        OffsetDateTime revokedAt = existingToken.getRevokedAt();
        UUID replacedByTokenId = existingToken.getReplacedByTokenId();
        OffsetDateTime availableUntil = existingToken.getReplacementTokenAvailableUntil();
        if (revokedAt != null
                && replacedByTokenId != null
                && availableUntil != null
                && OffsetDateTime.now().isBefore(availableUntil)) {
            RefreshToken replacement = refreshTokenRepository.findById(replacedByTokenId)
                    .filter(token -> !Boolean.TRUE.equals(token.getRevoked()))
                    .filter(token -> token.getExpiryDate().isAfter(OffsetDateTime.now()))
                    .orElseThrow(InvalidRefreshTokenException::new);
            replacement.setToken(decryptReplacementToken(existingToken.getReplacementTokenCiphertext()));
            return replacement;
        }

        revokeFamily(existingToken.getFamilyId());
        throw new InvalidRefreshTokenException();
    }

    private RefreshToken rotateToken(RefreshToken existingToken) {
        String rawToken = generateToken();
        RefreshToken rotated = RefreshToken.builder()
                .id(UUID.randomUUID())
                .userId(existingToken.getUserId())
                .token(rawToken)
                .tokenHash(hashToken(rawToken))
                .expiryDate(OffsetDateTime.now().plusDays(refreshTokenExpiryDays))
                .revoked(false)
                .familyId(existingToken.getFamilyId())
                .build();
        RefreshToken saved = refreshTokenRepository.save(rotated);
        saved.setToken(rawToken);

        OffsetDateTime now = OffsetDateTime.now();
        existingToken.setRevoked(true);
        existingToken.setRevokedAt(now);
        existingToken.setReplacedByTokenId(saved.getId());
        existingToken.setReplacementTokenCiphertext(encryptReplacementToken(rawToken));
        existingToken.setReplacementTokenAvailableUntil(now.plus(REUSE_GRACE_WINDOW));
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
        resolveStoredToken(rawToken, false).ifPresent(token -> {
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

    private String hashToken(String rawToken) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(rawToken.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 not available", ex);
        }
    }

    private byte[] deriveEncryptionKey(String jwtSecret) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return digest.digest(jwtSecret.getBytes(StandardCharsets.UTF_8));
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 not available", ex);
        }
    }

    private String encryptReplacementToken(String rawToken) {
        try {
            byte[] iv = new byte[GCM_IV_BYTES];
            secureRandom.nextBytes(iv);
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.ENCRYPT_MODE, replayEncryptionKey, new GCMParameterSpec(GCM_TAG_BITS, iv));
            byte[] ciphertext = cipher.doFinal(rawToken.getBytes(StandardCharsets.UTF_8));
            byte[] payload = new byte[iv.length + ciphertext.length];
            System.arraycopy(iv, 0, payload, 0, iv.length);
            System.arraycopy(ciphertext, 0, payload, iv.length, ciphertext.length);
            return Base64.getEncoder().encodeToString(payload);
        } catch (Exception ex) {
            throw new IllegalStateException("Failed to encrypt replacement refresh token", ex);
        }
    }

    private String decryptReplacementToken(String ciphertext) {
        try {
            byte[] payload = Base64.getDecoder().decode(ciphertext);
            byte[] iv = java.util.Arrays.copyOfRange(payload, 0, GCM_IV_BYTES);
            byte[] encrypted = java.util.Arrays.copyOfRange(payload, GCM_IV_BYTES, payload.length);
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.DECRYPT_MODE, replayEncryptionKey, new GCMParameterSpec(GCM_TAG_BITS, iv));
            return new String(cipher.doFinal(encrypted), StandardCharsets.UTF_8);
        } catch (Exception ex) {
            throw new IllegalStateException("Failed to decrypt replacement refresh token", ex);
        }
    }

    private java.util.Optional<RefreshToken> resolveStoredToken(String rawToken, boolean forUpdate) {
        String tokenHash = hashToken(rawToken);
        java.util.Optional<RefreshToken> hashed = forUpdate
                ? refreshTokenRepository.findByTokenHashForUpdate(tokenHash)
                : refreshTokenRepository.findByTokenHash(tokenHash);
        if (hashed.isPresent()) {
            return hashed;
        }

        java.util.Optional<RefreshToken> legacy = forUpdate
                ? refreshTokenRepository.findByLegacyTokenForUpdate(rawToken)
                : refreshTokenRepository.findByLegacyToken(rawToken);
        legacy.ifPresent(token -> {
            token.setTokenHash(tokenHash);
            refreshTokenRepository.save(token);
        });
        return legacy;
    }

}
