package com.saveapenny.insight.repository;

import com.saveapenny.insight.entity.InsightEntity;
import com.saveapenny.insight.entity.InsightType;
import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface InsightRepository extends JpaRepository<InsightEntity, UUID> {

    Page<InsightEntity> findAllByUserId(UUID userId, Pageable pageable);

    Page<InsightEntity> findAllByUserIdAndType(UUID userId, InsightType type, Pageable pageable);

    Page<InsightEntity> findAllByUserIdAndSeverity(UUID userId, String severity, Pageable pageable);

    Page<InsightEntity> findAllByUserIdAndRead(UUID userId, Boolean read, Pageable pageable);

    Page<InsightEntity> findAllByUserIdAndTypeAndSeverity(UUID userId, InsightType type, String severity, Pageable pageable);

    Page<InsightEntity> findAllByUserIdAndTypeAndRead(UUID userId, InsightType type, Boolean read, Pageable pageable);

    Page<InsightEntity> findAllByUserIdAndSeverityAndRead(UUID userId, String severity, Boolean read, Pageable pageable);

    Page<InsightEntity> findAllByUserIdAndTypeAndSeverityAndRead(
            UUID userId, InsightType type, String severity, Boolean read, Pageable pageable);

    Optional<InsightEntity> findByIdAndUserId(UUID id, UUID userId);

    boolean existsByUserIdAndTypeAndTitleAndGeneratedAtAfter(
            UUID userId, InsightType type, String title, OffsetDateTime after);
}
