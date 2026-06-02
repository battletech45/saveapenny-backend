package com.saveapenny.assistant.repository;

import com.saveapenny.assistant.entity.AssistantChatSession;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AssistantChatSessionRepository extends JpaRepository<AssistantChatSession, UUID> {

    Optional<AssistantChatSession> findByIdAndUserId(UUID id, UUID userId);
}
