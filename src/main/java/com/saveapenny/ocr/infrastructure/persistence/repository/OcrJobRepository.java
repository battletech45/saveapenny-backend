package com.saveapenny.ocr.infrastructure.persistence.repository;

import com.saveapenny.ocr.domain.model.OcrJob;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OcrJobRepository extends JpaRepository<OcrJob, UUID> {

    Optional<OcrJob> findByIdAndUserId(UUID id, UUID userId);
}
