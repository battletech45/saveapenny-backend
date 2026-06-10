package com.saveapenny.assistant.repository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.saveapenny.assistant.entity.AssistantChatSession;
import jakarta.persistence.EntityManager;
import java.time.OffsetDateTime;
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
class AssistantChatSessionRepositoryTest {

    @Autowired
    private AssistantChatSessionRepository assistantChatSessionRepository;

    @Autowired
    private EntityManager entityManager;

    private UUID userId;
    private AssistantChatSession session;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        session = AssistantChatSession.builder()
                .id(UUID.randomUUID())
                .userId(userId)
                .title("Test Session")
                .createdAt(OffsetDateTime.now())
                .updatedAt(OffsetDateTime.now())
                .build();
        assistantChatSessionRepository.save(session);
        entityManager.flush();
    }

    @Test
    void findByIdAndUserId_returnsSession() {
        Optional<AssistantChatSession> found = assistantChatSessionRepository.findByIdAndUserId(
                session.getId(), userId);
        assertTrue(found.isPresent());
        assertEquals(session.getId(), found.get().getId());
    }

    @Test
    void findByIdAndUserId_returnsEmptyForWrongUser() {
        assertTrue(assistantChatSessionRepository.findByIdAndUserId(
                session.getId(), UUID.randomUUID()).isEmpty());
    }
}
