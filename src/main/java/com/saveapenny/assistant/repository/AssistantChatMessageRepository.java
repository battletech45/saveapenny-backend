package com.saveapenny.assistant.repository;

import com.saveapenny.assistant.entity.AssistantChatMessage;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AssistantChatMessageRepository extends JpaRepository<AssistantChatMessage, UUID> {

    List<AssistantChatMessage> findAllBySessionIdOrderByCreatedAtAsc(UUID sessionId);

    List<AssistantChatMessage> findAllBySessionIdOrderByCreatedAtDesc(UUID sessionId, Pageable pageable);
}
