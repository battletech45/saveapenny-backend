package com.saveapenny.ocr.infrastructure.persistence.repository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.saveapenny.ocr.domain.model.OcrJob;
import com.saveapenny.ocr.domain.model.OcrJobStatus;
import jakarta.persistence.EntityManager;
import java.time.OffsetDateTime;
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
class OcrJobRepositoryTest {

    @Autowired
    private OcrJobRepository ocrJobRepository;

    @Autowired
    private EntityManager entityManager;

    private UUID userId;
    private OcrJob job;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        job = OcrJob.builder()
                .id(UUID.randomUUID())
                .userId(userId)
                .originalFileName("receipt.png")
                .contentType("image/png")
                .status(OcrJobStatus.PENDING)
                .createdAt(OffsetDateTime.now())
                .updatedAt(OffsetDateTime.now())
                .build();
        ocrJobRepository.save(job);
        entityManager.flush();
    }

    @Test
    void findByIdAndUserId_returnsJob() {
        Optional<OcrJob> found = ocrJobRepository.findByIdAndUserId(job.getId(), userId);
        assertTrue(found.isPresent());
        assertEquals(job.getId(), found.get().getId());
        assertEquals(OcrJobStatus.PENDING, found.get().getStatus());
    }

    @Test
    void findByIdAndUserId_returnsEmptyForWrongUser() {
        assertTrue(ocrJobRepository.findByIdAndUserId(job.getId(), UUID.randomUUID()).isEmpty());
    }
}
