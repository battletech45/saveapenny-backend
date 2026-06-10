package com.saveapenny.assistant.repository;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.saveapenny.assistant.entity.AssistantChatMessage;
import jakarta.persistence.EntityManager;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.TestPropertySource;

@DataJpaTest
@TestPropertySource(properties = {
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.flyway.enabled=false"
})
class AssistantChatMessageRepositoryTest {

    @Autowired
    private AssistantChatMessageRepository assistantChatMessageRepository;

    @Autowired
    private EntityManager entityManager;

    private UUID sessionId;

    @BeforeEach
    void setUp() {
        sessionId = UUID.randomUUID();

        AssistantChatMessage msg1 = AssistantChatMessage.builder()
                .id(UUID.randomUUID())
                .sessionId(sessionId)
                .role("user")
                .content("Hello")
                .createdAt(OffsetDateTime.now().minusMinutes(5))
                .build();

        AssistantChatMessage msg2 = AssistantChatMessage.builder()
                .id(UUID.randomUUID())
                .sessionId(sessionId)
                .role("assistant")
                .content("Hi there!")
                .createdAt(OffsetDateTime.now().minusMinutes(4))
                .build();

        AssistantChatMessage msg3 = AssistantChatMessage.builder()
                .id(UUID.randomUUID())
                .sessionId(UUID.randomUUID())
                .role("user")
                .content("Other session")
                .createdAt(OffsetDateTime.now())
                .build();

        assistantChatMessageRepository.save(msg1);
        assistantChatMessageRepository.save(msg2);
        assistantChatMessageRepository.save(msg3);
        entityManager.flush();
    }

    @Test
    void findAllBySessionIdOrderByCreatedAtAsc_returnsMessagesAscending() {
        List<AssistantChatMessage> messages =
                assistantChatMessageRepository.findAllBySessionIdOrderByCreatedAtAsc(sessionId);
        assertEquals(2, messages.size());
        assertEquals("user", messages.get(0).getRole());
        assertEquals("assistant", messages.get(1).getRole());
    }

    @Test
    void findAllBySessionIdOrderByCreatedAtDesc_returnsMessagesDescendingWithLimit() {
        List<AssistantChatMessage> messages =
                assistantChatMessageRepository.findAllBySessionIdOrderByCreatedAtDesc(
                        sessionId, PageRequest.of(0, 1));
        assertEquals(1, messages.size());
        assertEquals("assistant", messages.getFirst().getRole());
    }
}
