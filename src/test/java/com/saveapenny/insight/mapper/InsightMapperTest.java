package com.saveapenny.insight.mapper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import com.saveapenny.insight.dto.InsightResponse;
import com.saveapenny.insight.entity.InsightEntity;
import com.saveapenny.insight.entity.InsightType;
import java.time.OffsetDateTime;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

class InsightMapperTest {

    private final InsightMapper mapper = Mappers.getMapper(InsightMapper.class);

    @Test
    void toResponse_mapsAllFields() {
        UUID id = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        UUID categoryId = UUID.randomUUID();
        OffsetDateTime now = OffsetDateTime.now();

        InsightEntity entity = InsightEntity.builder()
                .id(id)
                .userId(userId)
                .type(InsightType.TREND)
                .title("Test title")
                .summary("Test summary")
                .detail("Test detail")
                .categoryId(categoryId)
                .severity("WARNING")
                .metadata("{\"key\":\"value\"}")
                .read(true)
                .dismissed(false)
                .generatedAt(now)
                .createdAt(now)
                .updatedAt(now)
                .build();

        InsightResponse response = mapper.toResponse(entity);

        assertEquals(id, response.getId());
        assertEquals(InsightType.TREND, response.getType());
        assertEquals("Test title", response.getTitle());
        assertEquals("Test summary", response.getSummary());
        assertEquals("Test detail", response.getDetail());
        assertEquals(categoryId, response.getCategoryId());
        assertEquals("WARNING", response.getSeverity());
        assertEquals("{\"key\":\"value\"}", response.getMetadata());
        assertEquals(true, response.getRead());
        assertEquals(false, response.getDismissed());
        assertEquals(now, response.getGeneratedAt());
        assertEquals(now, response.getCreatedAt());
    }

    @Test
    void toResponse_mapsNullFields() {
        InsightEntity entity = InsightEntity.builder()
                .id(UUID.randomUUID())
                .userId(UUID.randomUUID())
                .type(InsightType.SPENDING_PATTERN)
                .title("Title")
                .summary("Summary")
                .severity("INFO")
                .read(false)
                .dismissed(false)
                .generatedAt(OffsetDateTime.now())
                .createdAt(OffsetDateTime.now())
                .updatedAt(OffsetDateTime.now())
                .build();

        InsightResponse response = mapper.toResponse(entity);

        assertEquals("Title", response.getTitle());
        assertNull(response.getDetail());
        assertNull(response.getCategoryId());
        assertNull(response.getMetadata());
    }
}
