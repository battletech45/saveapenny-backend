package com.saveapenny.auth.repository;

import com.saveapenny.auth.entity.RefreshToken;
import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, UUID> {

    Optional<RefreshToken> findByTokenHash(String tokenHash);

    @Query("select rt from RefreshToken rt where rt.tokenHash = :rawToken")
    Optional<RefreshToken> findByLegacyToken(@Param("rawToken") String rawToken);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select rt from RefreshToken rt where rt.tokenHash = :tokenHash")
    Optional<RefreshToken> findByTokenHashForUpdate(@Param("tokenHash") String tokenHash);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select rt from RefreshToken rt where rt.tokenHash = :rawToken")
    Optional<RefreshToken> findByLegacyTokenForUpdate(@Param("rawToken") String rawToken);

    List<RefreshToken> findAllByUserIdAndRevokedFalse(UUID userId);

    List<RefreshToken> findAllByFamilyIdAndRevokedFalse(UUID familyId);
}
