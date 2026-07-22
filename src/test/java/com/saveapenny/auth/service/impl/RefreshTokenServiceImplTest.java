package com.saveapenny.auth.service.impl;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.saveapenny.auth.entity.RefreshToken;
import com.saveapenny.auth.exception.InvalidRefreshTokenException;
import com.saveapenny.auth.exception.RefreshTokenExpiredException;
import com.saveapenny.auth.repository.RefreshTokenRepository;
import com.saveapenny.user.entity.User;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class RefreshTokenServiceImplTest {

    @Mock
    private RefreshTokenRepository refreshTokenRepository;

    private RefreshTokenServiceImpl refreshTokenService;

    private User user;

    @BeforeEach
    void setUp() {
        user = User.builder().id(UUID.randomUUID()).build();
        refreshTokenService = new RefreshTokenServiceImpl(refreshTokenRepository, 7);
    }

    @Test
    void create_persistsTokenWithExpectedDefaults() {
        when(refreshTokenRepository.save(any(RefreshToken.class))).thenAnswer(invocation -> invocation.getArgument(0));

        RefreshToken created = refreshTokenService.create(user);

        assertEquals(user.getId(), created.getUserId());
        assertFalse(created.getRevoked());
        assertTrue(created.getExpiryDate().isAfter(OffsetDateTime.now().plusDays(6)));
        assertTrue(created.getToken().length() > 40);
    }

    @Test
    void validate_returnsTokenWhenActiveAndNotExpired() {
        RefreshToken token = RefreshToken.builder()
                .userId(user.getId())
                .token("valid-token")
                .revoked(false)
                .expiryDate(OffsetDateTime.now().plusMinutes(5))
                .build();
        when(refreshTokenRepository.findByToken("valid-token")).thenReturn(Optional.of(token));

        RefreshToken validated = refreshTokenService.validate("valid-token");

        assertEquals("valid-token", validated.getToken());
    }

    @Test
    void validate_throwsWhenMissing() {
        when(refreshTokenRepository.findByToken("missing")).thenReturn(Optional.empty());

        assertThrows(InvalidRefreshTokenException.class, () -> refreshTokenService.validate("missing"));
    }

    @Test
    void validate_throwsWhenRevoked() {
        RefreshToken token = RefreshToken.builder()
                .token("revoked-token")
                .revoked(true)
                .expiryDate(OffsetDateTime.now().plusMinutes(5))
                .build();
        when(refreshTokenRepository.findByToken("revoked-token")).thenReturn(Optional.of(token));

        assertThrows(InvalidRefreshTokenException.class, () -> refreshTokenService.validate("revoked-token"));
    }

    @Test
    void validate_throwsWhenExpired() {
        RefreshToken token = RefreshToken.builder()
                .token("expired-token")
                .revoked(false)
                .expiryDate(OffsetDateTime.now().minusSeconds(1))
                .build();
        when(refreshTokenRepository.findByToken("expired-token")).thenReturn(Optional.of(token));

        assertThrows(RefreshTokenExpiredException.class, () -> refreshTokenService.validate("expired-token"));
    }

    @Test
    void rotate_revokesExistingAndCreatesNewToken() {
        UUID familyId = UUID.randomUUID();
        RefreshToken existing = RefreshToken.builder()
                .userId(user.getId())
                .token("old-token")
                .revoked(false)
                .familyId(familyId)
                .expiryDate(OffsetDateTime.now().plusMinutes(5))
                .build();
        when(refreshTokenRepository.findByTokenForUpdate("old-token")).thenReturn(Optional.of(existing));
        when(refreshTokenRepository.save(any(RefreshToken.class))).thenAnswer(invocation -> invocation.getArgument(0));

        RefreshToken rotated = refreshTokenService.rotate("old-token");

        assertTrue(existing.getRevoked());
        assertEquals(rotated.getId(), existing.getReplacedByTokenId());
        assertEquals(user.getId(), rotated.getUserId());
        assertEquals(familyId, rotated.getFamilyId());
        assertFalse(rotated.getRevoked());
        assertFalse("old-token".equals(rotated.getToken()));
        verify(refreshTokenRepository).findByTokenForUpdate("old-token");
        verify(refreshTokenRepository, never()).findByToken("old-token");
    }

    @Test
    void rotate_reusedWithinGraceWindow_returnsReplacementInsteadOfThrowing() {
        UUID familyId = UUID.randomUUID();
        RefreshToken existing = RefreshToken.builder()
                .userId(user.getId())
                .token("old-token")
                .revoked(false)
                .familyId(familyId)
                .expiryDate(OffsetDateTime.now().plusMinutes(5))
                .build();
        when(refreshTokenRepository.findByTokenForUpdate("old-token")).thenReturn(Optional.of(existing));
        when(refreshTokenRepository.save(any(RefreshToken.class))).thenAnswer(invocation -> invocation.getArgument(0));

        RefreshToken firstRotation = refreshTokenService.rotate("old-token");
        when(refreshTokenRepository.findById(firstRotation.getId())).thenReturn(Optional.of(firstRotation));

        RefreshToken secondCaller = refreshTokenService.rotate("old-token");

        assertEquals(firstRotation.getId(), secondCaller.getId());
        assertEquals(firstRotation.getToken(), secondCaller.getToken());
        verify(refreshTokenRepository, times(2)).findByTokenForUpdate("old-token");
        verify(refreshTokenRepository, never()).findAllByFamilyIdAndRevokedFalse(any());
    }

    @Test
    void rotate_reusedAfterGraceWindow_revokesFamilyAndThrows() {
        UUID familyId = UUID.randomUUID();
        UUID replacementId = UUID.randomUUID();
        RefreshToken replacement = RefreshToken.builder()
                .id(replacementId)
                .userId(user.getId())
                .token("new-token")
                .revoked(false)
                .familyId(familyId)
                .expiryDate(OffsetDateTime.now().plusMinutes(5))
                .build();
        RefreshToken existing = RefreshToken.builder()
                .userId(user.getId())
                .token("old-token")
                .revoked(true)
                .revokedAt(OffsetDateTime.now().minusSeconds(30))
                .replacedByTokenId(replacementId)
                .familyId(familyId)
                .expiryDate(OffsetDateTime.now().plusMinutes(5))
                .build();
        when(refreshTokenRepository.findByTokenForUpdate("old-token")).thenReturn(Optional.of(existing));
        when(refreshTokenRepository.findAllByFamilyIdAndRevokedFalse(familyId)).thenReturn(List.of(replacement));

        assertThrows(InvalidRefreshTokenException.class, () -> refreshTokenService.rotate("old-token"));

        assertTrue(replacement.getRevoked());
        verify(refreshTokenRepository).saveAll(List.of(replacement));
    }

    @Test
    void revoke_marksTokenRevokedWhenFound() {
        RefreshToken token = RefreshToken.builder()
                .token("token-to-revoke")
                .revoked(false)
                .expiryDate(OffsetDateTime.now().plusMinutes(1))
                .build();
        when(refreshTokenRepository.findByToken("token-to-revoke")).thenReturn(Optional.of(token));
        when(refreshTokenRepository.save(any(RefreshToken.class))).thenAnswer(invocation -> invocation.getArgument(0));

        refreshTokenService.revoke("token-to-revoke");

        assertTrue(token.getRevoked());
        verify(refreshTokenRepository).save(token);
    }

    @Test
    void revoke_doesNothingWhenTokenMissing() {
        when(refreshTokenRepository.findByToken("unknown")).thenReturn(Optional.empty());

        assertDoesNotThrow(() -> refreshTokenService.revoke("unknown"));
        verify(refreshTokenRepository, never()).save(any(RefreshToken.class));
    }

    @Test
    void revokeAllByUser_revokesAllActiveTokens() {
        RefreshToken first = RefreshToken.builder().token("t1").revoked(false).build();
        RefreshToken second = RefreshToken.builder().token("t2").revoked(false).build();
        when(refreshTokenRepository.findAllByUserIdAndRevokedFalse(user.getId())).thenReturn(List.of(first, second));

        refreshTokenService.revokeAllByUser(user);

        assertTrue(first.getRevoked());
        assertTrue(second.getRevoked());
        verify(refreshTokenRepository).saveAll(List.of(first, second));
    }
}
