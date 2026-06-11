package com.saveapenny.imports.repository;

import com.saveapenny.imports.entity.Import;
import com.saveapenny.imports.entity.ImportStatus;
import jakarta.persistence.LockModeType;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ImportRepository extends JpaRepository<Import, UUID> {

    Optional<Import> findByIdAndUserId(UUID id, UUID userId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select i from Import i where i.id = :id and i.userId = :userId")
    Optional<Import> findByIdAndUserIdForUpdate(@Param("id") UUID id, @Param("userId") UUID userId);

    Page<Import> findAllByUserId(UUID userId, Pageable pageable);

    Page<Import> findAllByUserIdAndStatus(UUID userId, ImportStatus status, Pageable pageable);
}
