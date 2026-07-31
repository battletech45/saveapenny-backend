package com.saveapenny.auth.repository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.saveapenny.auth.entity.RefreshToken;
import jakarta.persistence.EntityManager;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.OffsetDateTime;
import java.util.HexFormat;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.test.context.TestPropertySource;

@DataJpaTest
@TestPropertySource(properties = {
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.flyway.enabled=false"
})
class RefreshTokenRepositoryTest {

    @Autowired
    private RefreshTokenRepository refreshTokenRepository;

    @Autowired
    private EntityManager entityManager;

    private UUID userId;
    private RefreshToken token;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        token = RefreshToken.builder()
                .id(UUID.randomUUID())
                .userId(userId)
                .tokenHash(hash("test-refresh-token-value"))
                .expiryDate(OffsetDateTime.now().plusDays(7))
                .revoked(false)
                .createdAt(OffsetDateTime.now())
                .build();
        refreshTokenRepository.save(token);
        entityManager.flush();
    }

    @Test
    void findByTokenHash_returnsToken() {
        Optional<RefreshToken> found = refreshTokenRepository.findByTokenHash(hash("test-refresh-token-value"));
        assertTrue(found.isPresent());
        assertEquals(token.getId(), found.get().getId());
    }

    @Test
    void findByTokenHashForUpdate_returnsToken() {
        Optional<RefreshToken> found = refreshTokenRepository.findByTokenHashForUpdate(hash("test-refresh-token-value"));
        assertTrue(found.isPresent());
        assertEquals(token.getId(), found.get().getId());
    }

    @Test
    void findByTokenHash_returnsEmpty_whenNotFound() {
        assertTrue(refreshTokenRepository.findByTokenHash(hash("nonexistent")).isEmpty());
    }

    @Test
    void findAllByUserIdAndRevokedFalse_returnsActiveTokens() {
        List<RefreshToken> tokens = refreshTokenRepository.findAllByUserIdAndRevokedFalse(userId);
        assertEquals(1, tokens.size());
    }

    @Test
    void findAllByUserIdAndRevokedFalse_excludesRevoked() {
        RefreshToken revoked = RefreshToken.builder()
                .id(UUID.randomUUID())
                .userId(userId)
                .tokenHash(hash("revoked-token"))
                .expiryDate(OffsetDateTime.now().plusDays(7))
                .revoked(true)
                .createdAt(OffsetDateTime.now())
                .build();
        refreshTokenRepository.save(revoked);
        entityManager.flush();

        List<RefreshToken> tokens = refreshTokenRepository.findAllByUserIdAndRevokedFalse(userId);
        assertEquals(1, tokens.size());
    }

    @Test
    void findAllByFamilyIdAndRevokedFalse_returnsActiveMembersOfFamily() {
        UUID familyId = refreshTokenRepository.findByTokenHash(hash("test-refresh-token-value")).orElseThrow().getFamilyId();
        RefreshToken sibling = RefreshToken.builder()
                .id(UUID.randomUUID())
                .userId(userId)
                .tokenHash(hash("rotated-sibling-token"))
                .expiryDate(OffsetDateTime.now().plusDays(7))
                .revoked(false)
                .familyId(familyId)
                .createdAt(OffsetDateTime.now())
                .build();
        RefreshToken otherFamily = RefreshToken.builder()
                .id(UUID.randomUUID())
                .userId(userId)
                .tokenHash(hash("unrelated-token"))
                .expiryDate(OffsetDateTime.now().plusDays(7))
                .revoked(false)
                .createdAt(OffsetDateTime.now())
                .build();
        refreshTokenRepository.save(sibling);
        refreshTokenRepository.save(otherFamily);
        entityManager.flush();

        List<RefreshToken> tokens = refreshTokenRepository.findAllByFamilyIdAndRevokedFalse(familyId);
        assertEquals(2, tokens.size());
    }

    private String hash(String rawToken) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(rawToken.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException(ex);
        }
    }
}
