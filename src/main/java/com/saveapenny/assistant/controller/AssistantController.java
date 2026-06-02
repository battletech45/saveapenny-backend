package com.saveapenny.assistant.controller;

import com.saveapenny.assistant.dto.AssistantChatRequest;
import com.saveapenny.assistant.dto.AssistantChatResponse;
import com.saveapenny.assistant.service.AssistantService;
import com.saveapenny.config.security.CurrentUserPrincipal;
import com.saveapenny.shared.api.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/assistant")
@PreAuthorize("isAuthenticated()")
@Tag(name = "Assistant", description = "AI assistant chat endpoint for finance and savings guidance.")
public class AssistantController {

    private final AssistantService assistantService;

    public AssistantController(AssistantService assistantService) {
        this.assistantService = assistantService;
    }

    @PostMapping("/chat")
    public ResponseEntity<ApiResponse<AssistantChatResponse>> chat(
            @AuthenticationPrincipal CurrentUserPrincipal principal,
            @Valid @RequestBody AssistantChatRequest request) {
        AssistantChatResponse response = assistantService.chat(getCurrentUserId(principal), request);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    private UUID getCurrentUserId(CurrentUserPrincipal principal) {
        if (principal == null || principal.userId() == null) {
            throw new AccessDeniedException("Missing authenticated user context.");
        }
        return principal.userId();
    }
}
