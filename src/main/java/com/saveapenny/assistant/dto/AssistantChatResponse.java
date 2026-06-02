package com.saveapenny.assistant.dto;

import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AssistantChatResponse {

    private UUID sessionId;

    private String reply;

    private String disclaimer;
}
