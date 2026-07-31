package com.saveapenny.feedback.dto;

import com.fasterxml.jackson.databind.JsonNode;
import com.saveapenny.feedback.entity.FeedbackType;
import java.time.OffsetDateTime;
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
public class FeedbackResponse {

    private UUID id;
    private UUID userId;
    private FeedbackType type;
    private Integer rating;
    private String message;
    private JsonNode metadata;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
}
