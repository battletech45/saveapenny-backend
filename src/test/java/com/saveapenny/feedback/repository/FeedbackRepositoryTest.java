package com.saveapenny.feedback.repository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.saveapenny.feedback.entity.Feedback;
import com.saveapenny.feedback.entity.FeedbackType;
import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.TestPropertySource;

@DataJpaTest
@TestPropertySource(properties = {
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.flyway.enabled=false"
})
class FeedbackRepositoryTest {

    @Autowired
    private FeedbackRepository feedbackRepository;

    private UUID userId;
    private UUID otherUserId;
    private Feedback feedback;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        otherUserId = UUID.randomUUID();
        feedback = Feedback.builder()
                .id(UUID.randomUUID())
                .userId(userId)
                .type(FeedbackType.FEATURE_REQUEST)
                .rating(4)
                .message("Add recurring transfer presets")
                .metadata("{\"platform\":\"ios\"}")
                .createdAt(OffsetDateTime.now().minusDays(1))
                .updatedAt(OffsetDateTime.now())
                .build();
        feedbackRepository.save(feedback);
        feedbackRepository.save(Feedback.builder()
                .id(UUID.randomUUID())
                .userId(otherUserId)
                .type(FeedbackType.GENERAL)
                .rating(null)
                .message("Other user's feedback")
                .createdAt(OffsetDateTime.now())
                .updatedAt(OffsetDateTime.now())
                .build());
    }

    @Test
    void findByIdAndUserId_returnsOwnedFeedback() {
        Optional<Feedback> found = feedbackRepository.findByIdAndUserId(feedback.getId(), userId);

        assertTrue(found.isPresent());
        assertEquals(feedback.getId(), found.get().getId());
    }

    @Test
    void findByIdAndUserId_returnsEmptyForWrongUser() {
        Optional<Feedback> found = feedbackRepository.findByIdAndUserId(feedback.getId(), otherUserId);

        assertTrue(found.isEmpty());
    }

    @Test
    void findAllByUserId_returnsOnlyOwnedFeedback() {
        Page<Feedback> page = feedbackRepository.findAllByUserId(userId, PageRequest.of(0, 20));

        assertEquals(1, page.getTotalElements());
        assertEquals(feedback.getId(), page.getContent().getFirst().getId());
    }

    @Test
    void findAllByUserIdAndType_filtersByType() {
        feedbackRepository.save(Feedback.builder()
                .id(UUID.randomUUID())
                .userId(userId)
                .type(FeedbackType.BUG_REPORT)
                .rating(2)
                .message("Crash on report export")
                .createdAt(OffsetDateTime.now())
                .updatedAt(OffsetDateTime.now())
                .build());

        Page<Feedback> page = feedbackRepository.findAllByUserIdAndType(
                userId, FeedbackType.FEATURE_REQUEST, PageRequest.of(0, 20));

        assertEquals(1, page.getTotalElements());
        assertEquals(FeedbackType.FEATURE_REQUEST, page.getContent().getFirst().getType());
    }
}
