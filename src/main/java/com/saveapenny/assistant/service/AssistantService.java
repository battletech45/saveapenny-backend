package com.saveapenny.assistant.service;

import com.saveapenny.assistant.dto.AssistantChatRequest;
import com.saveapenny.assistant.dto.AssistantChatResponse;
import java.util.UUID;

public interface AssistantService {

    AssistantChatResponse chat(UUID userId, AssistantChatRequest request);
}
