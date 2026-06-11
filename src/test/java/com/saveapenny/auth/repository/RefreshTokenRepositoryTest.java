package com.saveapenny.auth.repository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.saveapenny.auth.entity.RefreshToken;
import jakarta.persistence.EntityManager;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
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
                .token("test-refresh-token-value")
                .expiryDate(OffsetDateTime.now().plusDays(7))
                .revoked(false)
                .createdAt(OffsetDateTime.now())
                .build();
        refreshTokenRepository.save(token);
        entityManager.flush();
    }

    @Test
    void findByToken_returnsToken() {
        Optional<RefreshToken> found = refreshTokenRepository.findByToken("test-refresh-token-value");
        assertTrue(found.isPresent());
        assertEquals(token.getId(), found.get().getId());
    }

    @Test
    void findByTokenForUpdate_returnsToken() {
        Optional<RefreshToken> found = refreshTokenRepository.findByTokenForUpdate("test-refresh-token-value");
        assertTrue(found.isPresent());
        assertEquals(token.getId(), found.get().getId());
    }

    @Test
    void findByToken_returnsEmpty_whenNotFound() {
        assertTrue(refreshTokenRepository.findByToken("nonexistent").isEmpty());
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
                .token("revoked-token")
                .expiryDate(OffsetDateTime.now().plusDays(7))
                .revoked(true)
                .createdAt(OffsetDateTime.now())
                .build();
        refreshTokenRepository.save(revoked);
        entityManager.flush();

        List<RefreshToken> tokens = refreshTokenRepository.findAllByUserIdAndRevokedFalse(userId);
        assertEquals(1, tokens.size());
    }
}
