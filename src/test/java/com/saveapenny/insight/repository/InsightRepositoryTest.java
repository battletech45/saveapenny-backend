package com.saveapenny.insight.repository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.saveapenny.insight.entity.InsightEntity;
import com.saveapenny.insight.entity.InsightType;
import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.TestPropertySource;

@DataJpaTest
@TestPropertySource(properties = {
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.flyway.enabled=false"
})
class InsightRepositoryTest {

    @Autowired
    private InsightRepository insightRepository;

    private UUID userId;
    private InsightEntity entity;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        entity = InsightEntity.builder()
                .id(UUID.randomUUID())
                .userId(userId)
                .type(InsightType.SPENDING_PATTERN)
                .title("Test title")
                .summary("Test summary")
                .severity("INFO")
                .read(false)
                .dismissed(false)
                .generatedAt(OffsetDateTime.now())
                .createdAt(OffsetDateTime.now())
                .updatedAt(OffsetDateTime.now())
                .build();
        insightRepository.save(entity);
    }

    @Test
    void findAllByUserId_returnsUserInsights() {
        Page<InsightEntity> page = insightRepository.findAllByUserId(userId, PageRequest.of(0, 20));

        assertEquals(1, page.getTotalElements());
        assertEquals(entity.getId(), page.getContent().getFirst().getId());
    }

    @Test
    void findAllByUserId_doesNotReturnOtherUsers() {
        Page<InsightEntity> page = insightRepository.findAllByUserId(UUID.randomUUID(), PageRequest.of(0, 20));

        assertEquals(0, page.getTotalElements());
    }

    @Test
    void findAllByUserIdAndType_filtersByType() {
        Page<InsightEntity> page = insightRepository.findAllByUserIdAndType(
                userId, InsightType.SPENDING_PATTERN, PageRequest.of(0, 20));

        assertEquals(1, page.getTotalElements());
    }

    @Test
    void findAllByUserIdAndType_excludesOtherTypes() {
        Page<InsightEntity> page = insightRepository.findAllByUserIdAndType(
                userId, InsightType.ANOMALY, PageRequest.of(0, 20));

        assertEquals(0, page.getTotalElements());
    }

    @Test
    void findAllByUserIdAndSeverity_filtersBySeverity() {
        Page<InsightEntity> page = insightRepository.findAllByUserIdAndSeverity(
                userId, "INFO", PageRequest.of(0, 20));

        assertEquals(1, page.getTotalElements());
    }

    @Test
    void findAllByUserIdAndRead_filtersByReadStatus() {
        Page<InsightEntity> readPage = insightRepository.findAllByUserIdAndRead(
                userId, true, PageRequest.of(0, 20));
        Page<InsightEntity> unreadPage = insightRepository.findAllByUserIdAndRead(
                userId, false, PageRequest.of(0, 20));

        assertEquals(0, readPage.getTotalElements());
        assertEquals(1, unreadPage.getTotalElements());
    }

    @Test
    void findByIdAndUserId_returnsInsight() {
        Optional<InsightEntity> result = insightRepository.findByIdAndUserId(entity.getId(), userId);

        assertTrue(result.isPresent());
        assertEquals(entity.getId(), result.get().getId());
    }

    @Test
    void findByIdAndUserId_returnsEmptyForWrongUser() {
        Optional<InsightEntity> result = insightRepository.findByIdAndUserId(
                entity.getId(), UUID.randomUUID());

        assertFalse(result.isPresent());
    }

    @Test
    void existsByUserIdAndTypeAndTitleAndGeneratedAtAfter_returnsTrueWhenExists() {
        boolean exists = insightRepository.existsByUserIdAndTypeAndTitleAndGeneratedAtAfter(
                userId, InsightType.SPENDING_PATTERN, "Test title",
                OffsetDateTime.now().minusDays(1));

        assertTrue(exists);
    }

    @Test
    void existsByUserIdAndTypeAndTitleAndGeneratedAtAfter_returnsFalseWhenOutsideWindow() {
        boolean exists = insightRepository.existsByUserIdAndTypeAndTitleAndGeneratedAtAfter(
                userId, InsightType.SPENDING_PATTERN, "Test title",
                OffsetDateTime.now().plusDays(1));

        assertFalse(exists);
    }

    @Test
    void existsByUserIdAndTypeAndTitleAndGeneratedAtAfter_returnsFalseForDifferentTitle() {
        boolean exists = insightRepository.existsByUserIdAndTypeAndTitleAndGeneratedAtAfter(
                userId, InsightType.SPENDING_PATTERN, "Non-existent title",
                OffsetDateTime.now().minusDays(1));

        assertFalse(exists);
    }
}
