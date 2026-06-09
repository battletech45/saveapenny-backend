package com.saveapenny.insight.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.saveapenny.insight.dto.GenerateInsightsRequest;
import com.saveapenny.insight.dto.InsightListResponse;
import com.saveapenny.insight.dto.InsightResponse;
import com.saveapenny.insight.entity.InsightEntity;
import com.saveapenny.insight.entity.InsightType;
import com.saveapenny.insight.exception.InsightNotFoundException;
import com.saveapenny.insight.mapper.InsightMapper;
import com.saveapenny.insight.repository.InsightRepository;
import com.saveapenny.insight.service.impl.InsightGenerationPipeline;
import com.saveapenny.insight.service.impl.InsightServiceImpl;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mapstruct.factory.Mappers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

@ExtendWith(MockitoExtension.class)
class InsightServiceImplTest {

    @Mock
    private InsightRepository insightRepository;

    @Spy
    private InsightMapper insightMapper = Mappers.getMapper(InsightMapper.class);

    @Mock
    private InsightGenerationPipeline insightGenerationPipeline;

    @InjectMocks
    private InsightServiceImpl insightService;

    private UUID userId;
    private UUID insightId;
    private InsightEntity insightEntity;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        insightId = UUID.randomUUID();
        insightEntity = InsightEntity.builder()
                .id(insightId)
                .userId(userId)
                .type(InsightType.SPENDING_PATTERN)
                .title("Test insight")
                .summary("Test summary")
                .severity("INFO")
                .read(false)
                .dismissed(false)
                .generatedAt(OffsetDateTime.now())
                .createdAt(OffsetDateTime.now())
                .updatedAt(OffsetDateTime.now())
                .build();
    }

    @Test
    void getAll_returnsPaginatedResults() {
        Page<InsightEntity> page = new PageImpl<>(List.of(insightEntity));
        when(insightRepository.findAllByUserId(userId, PageRequest.of(0, 20)))
                .thenReturn(page);

        InsightListResponse response = insightService.getAll(userId, null, null, null, PageRequest.of(0, 20));

        assertNotNull(response);
        assertEquals(1, response.getInsights().size());
        assertEquals(insightId, response.getInsights().getFirst().getId());
    }

    @Test
    void getById_returnsInsight() {
        when(insightRepository.findByIdAndUserId(insightId, userId))
                .thenReturn(Optional.of(insightEntity));

        InsightResponse response = insightService.getById(userId, insightId);

        assertNotNull(response);
        assertEquals(insightId, response.getId());
    }

    @Test
    void getById_throwsWhenNotFound() {
        when(insightRepository.findByIdAndUserId(insightId, userId))
                .thenReturn(Optional.empty());

        assertThrows(InsightNotFoundException.class, () -> insightService.getById(userId, insightId));
    }

    @Test
    void markAsRead_setsReadToTrue() {
        insightEntity.setRead(false);
        when(insightRepository.findByIdAndUserId(insightId, userId))
                .thenReturn(Optional.of(insightEntity));
        when(insightRepository.save(any(InsightEntity.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        InsightResponse response = insightService.markAsRead(userId, insightId);

        assertNotNull(response);
    }

    @Test
    void dismiss_setsDismissedToTrue() {
        insightEntity.setDismissed(false);
        when(insightRepository.findByIdAndUserId(insightId, userId))
                .thenReturn(Optional.of(insightEntity));
        when(insightRepository.save(any(InsightEntity.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        InsightResponse response = insightService.dismiss(userId, insightId);

        assertNotNull(response);
    }

    @Test
    void generate_callsPipeline() {
        when(insightGenerationPipeline.execute(userId)).thenReturn(5);

        int count = insightService.generate(userId, new GenerateInsightsRequest());

        assertEquals(5, count);
        verify(insightGenerationPipeline).execute(userId);
    }

    @Test
    void getAll_filtersByType() {
        Page<InsightEntity> page = new PageImpl<>(List.of(insightEntity));
        when(insightRepository.findAllByUserIdAndType(userId, InsightType.SPENDING_PATTERN, PageRequest.of(0, 20)))
                .thenReturn(page);

        InsightListResponse response = insightService.getAll(userId, InsightType.SPENDING_PATTERN, null, null, PageRequest.of(0, 20));

        assertNotNull(response);
        assertEquals(1, response.getInsights().size());
    }
}
