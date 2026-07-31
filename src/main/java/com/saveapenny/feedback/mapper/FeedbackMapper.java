package com.saveapenny.feedback.mapper;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.saveapenny.feedback.dto.CreateFeedbackRequest;
import com.saveapenny.feedback.dto.FeedbackResponse;
import com.saveapenny.feedback.entity.Feedback;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.springframework.beans.factory.annotation.Autowired;

@Mapper(componentModel = "spring")
public abstract class FeedbackMapper {

    @Autowired
    protected ObjectMapper objectMapper;

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "userId", ignore = true)
    @Mapping(target = "metadata", expression = "java(jsonNodeToString(request.getMetadata()))")
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    public abstract Feedback toEntity(CreateFeedbackRequest request);

    @Mapping(target = "metadata", expression = "java(stringToJsonNode(feedback.getMetadata()))")
    public abstract FeedbackResponse toResponse(Feedback feedback);

    public String jsonNodeToString(JsonNode jsonNode) {
        if (jsonNode == null) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(jsonNode);
        } catch (JsonProcessingException ex) {
            throw new IllegalArgumentException("Failed to serialize feedback metadata.", ex);
        }
    }

    public JsonNode stringToJsonNode(String json) {
        if (json == null || json.isBlank()) {
            return null;
        }
        try {
            return objectMapper.readTree(json);
        } catch (JsonProcessingException ex) {
            throw new IllegalArgumentException("Failed to deserialize feedback metadata.", ex);
        }
    }
}
