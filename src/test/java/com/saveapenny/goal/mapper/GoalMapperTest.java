package com.saveapenny.goal.mapper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.saveapenny.goal.dto.CreateGoalRequest;
import com.saveapenny.goal.dto.GoalResponse;
import com.saveapenny.goal.dto.GoalRunResponse;
import com.saveapenny.goal.dto.ScenarioResponse;
import com.saveapenny.goal.entity.Feasibility;
import com.saveapenny.goal.entity.GoalEntity;
import com.saveapenny.goal.entity.GoalRunEntity;
import com.saveapenny.goal.entity.GoalRunTrigger;
import com.saveapenny.goal.entity.GoalStatus;
import com.saveapenny.goal.entity.GoalType;
import com.saveapenny.goal.entity.ScenarioEntity;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = GoalMapperTest.GoalMapperTestConfig.class)
class GoalMapperTest {

    @Autowired
    private GoalMapper goalMapper;

    @Autowired
    private ObjectMapper objectMapper;

    private ObjectNode sampleInputs;
    private ObjectNode sampleOutputSummary;

    @BeforeEach
    void setUp() {
        sampleInputs = objectMapper.createObjectNode();
        sampleInputs.put("startBalance", 5000);
        sampleInputs.put("contribution", 500);

        sampleOutputSummary = objectMapper.createObjectNode();
        sampleOutputSummary.put("projectedAmount", 15000);
    }

    @Test
    void toEntity_mapsCreateRequest() {
        CreateGoalRequest request = CreateGoalRequest.builder()
                .type(GoalType.SAVINGS)
                .title("Emergency Fund")
                .targetAmount(new BigDecimal("10000.0000"))
                .currency("USD")
                .targetDate(LocalDate.of(2027, 1, 1))
                .linkedAccountId(UUID.randomUUID())
                .inputs(sampleInputs)
                .build();

        GoalEntity entity = goalMapper.toEntity(request);

        assertNull(entity.getId());
        assertNull(entity.getUserId());
        assertEquals(GoalType.SAVINGS, entity.getType());
        assertEquals("Emergency Fund", entity.getTitle());
        assertEquals(0, new BigDecimal("10000.0000").compareTo(entity.getTargetAmount()));
        assertEquals("USD", entity.getCurrency());
        assertEquals(LocalDate.of(2027, 1, 1), entity.getTargetDate());
        assertEquals(request.getLinkedAccountId(), entity.getLinkedAccountId());
        assertNull(entity.getStatus());
        assertNotNull(entity.getInputsJson());
        assertNull(entity.getCreatedAt());
        assertNull(entity.getUpdatedAt());
    }

    @Test
    void toResponse_mapsGoalEntity() {
        UUID id = UUID.randomUUID();
        OffsetDateTime now = OffsetDateTime.now();
        GoalEntity entity = GoalEntity.builder()
                .id(id)
                .userId(UUID.randomUUID())
                .type(GoalType.SAVINGS)
                .title("Test Goal")
                .targetAmount(new BigDecimal("5000.0000"))
                .currency("USD")
                .targetDate(LocalDate.of(2027, 6, 1))
                .linkedAccountId(UUID.randomUUID())
                .status(GoalStatus.ACTIVE)
                .inputsJson("{\"startBalance\":1000}")
                .createdAt(now)
                .updatedAt(now)
                .build();

        GoalResponse response = goalMapper.toResponse(entity);

        assertEquals(id, response.getId());
        assertEquals("Test Goal", response.getTitle());
        assertEquals(GoalStatus.ACTIVE, response.getStatus());
        assertNotNull(response.getInputs());
        assertEquals(1000, response.getInputs().get("startBalance").asInt());
    }

    @Test
    void toResponse_mapsScenarioEntity() {
        UUID id = UUID.randomUUID();
        ScenarioEntity entity = ScenarioEntity.builder()
                .id(id)
                .goalId(UUID.randomUUID())
                .name("Baseline")
                .inputsJson("{\"contribution\":300}")
                .isBaseline(true)
                .createdAt(OffsetDateTime.now())
                .build();

        ScenarioResponse response = goalMapper.toResponse(entity);

        assertEquals(id, response.getId());
        assertEquals("Baseline", response.getName());
        assertEquals(true, response.getIsBaseline());
        assertNotNull(response.getInputs());
        assertEquals(300, response.getInputs().get("contribution").asInt());
    }

    @Test
    void toResponse_mapsGoalRunEntity() {
        UUID id = UUID.randomUUID();
        GoalRunEntity entity = GoalRunEntity.builder()
                .id(id)
                .goalId(UUID.randomUUID())
                .scenarioId(UUID.randomUUID())
                .inputsSnapshotJson("{\"startBalance\":1000}")
                .outputSummaryJson("{\"projectedAmount\":20000}")
                .outputSeriesJson("{\"monthly\":[1000,2000,3000]}")
                .feasibility(Feasibility.ON_TRACK)
                .triggeredBy(GoalRunTrigger.USER)
                .createdAt(OffsetDateTime.now())
                .build();

        GoalRunResponse response = goalMapper.toResponse(entity);

        assertEquals(id, response.getId());
        assertEquals(Feasibility.ON_TRACK, response.getFeasibility());
        assertEquals(GoalRunTrigger.USER, response.getTriggeredBy());
        assertNotNull(response.getInputsSnapshot());
        assertNotNull(response.getOutputSummary());
        assertEquals(20000, response.getOutputSummary().get("projectedAmount").asInt());
        assertNotNull(response.getOutputSeries());
    }

    @Test
    void jsonNodeToString_serializesJson() throws Exception {
        String result = goalMapper.jsonNodeToString(sampleInputs);
        assertNotNull(result);
        assertEquals(5000, objectMapper.readTree(result).get("startBalance").asInt());
    }

    @Test
    void jsonNodeToString_returnsNull_whenNull() {
        assertNull(goalMapper.jsonNodeToString(null));
    }

    @Test
    void stringToJsonNode_deserializesJson() {
        JsonNode result = goalMapper.stringToJsonNode("{\"key\":\"value\"}");
        assertNotNull(result);
        assertEquals("value", result.get("key").asText());
    }

    @Test
    void stringToJsonNode_returnsNull_whenNull() {
        assertNull(goalMapper.stringToJsonNode(null));
    }

    @Test
    void stringToJsonNode_returnsNull_whenBlank() {
        assertNull(goalMapper.stringToJsonNode("  "));
    }

    @TestConfiguration
    static class GoalMapperTestConfig {
        @Bean
        public ObjectMapper objectMapper() {
            return new ObjectMapper();
        }

        @Bean
        public GoalMapper goalMapper(ObjectMapper objectMapper) {
            GoalMapperImpl impl = new GoalMapperImpl();
            impl.objectMapper = objectMapper;
            return impl;
        }
    }
}
