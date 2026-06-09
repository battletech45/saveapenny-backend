package com.saveapenny.goal.mapper;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.saveapenny.goal.dto.CreateGoalRequest;
import com.saveapenny.goal.dto.GoalResponse;
import com.saveapenny.goal.dto.GoalRunResponse;
import com.saveapenny.goal.dto.ScenarioResponse;
import com.saveapenny.goal.entity.GoalEntity;
import com.saveapenny.goal.entity.GoalRunEntity;
import com.saveapenny.goal.entity.ScenarioEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.springframework.beans.factory.annotation.Autowired;

@Mapper(componentModel = "spring")
public abstract class GoalMapper {

    @Autowired
    protected ObjectMapper objectMapper;

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "userId", ignore = true)
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "inputsJson", expression = "java(jsonNodeToString(request.getInputs()))")
    @Mapping(target = "deletedAt", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    public abstract GoalEntity toEntity(CreateGoalRequest request);

    @Mapping(target = "inputs", expression = "java(stringToJsonNode(entity.getInputsJson()))")
    public abstract GoalResponse toResponse(GoalEntity entity);

    @Mapping(target = "inputs", expression = "java(stringToJsonNode(entity.getInputsJson()))")
    public abstract ScenarioResponse toResponse(ScenarioEntity entity);

    @Mapping(target = "inputsSnapshot", expression = "java(stringToJsonNode(entity.getInputsSnapshotJson()))")
    @Mapping(target = "outputSummary", expression = "java(stringToJsonNode(entity.getOutputSummaryJson()))")
    @Mapping(target = "outputSeries", expression = "java(stringToJsonNode(entity.getOutputSeriesJson()))")
    public abstract GoalRunResponse toResponse(GoalRunEntity entity);

    public String jsonNodeToString(JsonNode jsonNode) {
        if (jsonNode == null) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(jsonNode);
        } catch (JsonProcessingException ex) {
            throw new IllegalArgumentException("Failed to serialize goal JSON.", ex);
        }
    }

    public JsonNode stringToJsonNode(String json) {
        if (json == null || json.isBlank()) {
            return null;
        }
        try {
            return objectMapper.readTree(json);
        } catch (JsonProcessingException ex) {
            throw new IllegalArgumentException("Failed to deserialize goal JSON.", ex);
        }
    }
}
