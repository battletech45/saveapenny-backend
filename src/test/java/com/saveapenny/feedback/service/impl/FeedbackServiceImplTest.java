package com.saveapenny.feedback.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.saveapenny.feedback.dto.CreateFeedbackRequest;
import com.saveapenny.feedback.dto.FeedbackResponse;
import com.saveapenny.feedback.entity.Feedback;
import com.saveapenny.feedback.entity.FeedbackStatus;
import com.saveapenny.feedback.entity.FeedbackType;
import com.saveapenny.feedback.exception.FeedbackNotFoundException;
import com.saveapenny.feedback.mapper.FeedbackMapper;
import com.saveapenny.feedback.notification.FeedbackStatusNotifier;
import com.saveapenny.feedback.repository.FeedbackRepository;
import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

@ExtendWith(MockitoExtension.class)
class FeedbackServiceImplTest {

    @Mock
    private FeedbackRepository feedbackRepository;

    @Mock
    private FeedbackMapper feedbackMapper;

    @Mock
    private FeedbackStatusNotifier feedbackStatusNotifier;

    @InjectMocks
    private FeedbackServiceImpl feedbackService;

    private UUID userId;
    private UUID feedbackId;
    private Feedback feedback;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        feedbackId = UUID.randomUUID();
        feedback = Feedback.builder()
                .id(feedbackId)
                .userId(userId)
                .type(FeedbackType.BUG_REPORT)
                .rating(2)
                .message("App freezes on startup")
                .metadata("{\"screen\":\"home\"}")
                .status(FeedbackStatus.OPEN)
                .createdAt(OffsetDateTime.now().minusDays(1))
                .updatedAt(OffsetDateTime.now())
                .build();
    }

    @Test
    void create_returnsResponse_whenValid() {
        CreateFeedbackRequest request = CreateFeedbackRequest.builder()
                .type(FeedbackType.FEATURE_REQUEST)
                .rating(5)
                .message("Please add widgets")
                .metadata(JsonNodeFactory.instance.objectNode().put("platform", "ios"))
                .build();
        Feedback mapped = Feedback.builder()
                .type(FeedbackType.FEATURE_REQUEST)
                .rating(5)
                .message("Please add widgets")
                .metadata("{\"platform\":\"ios\"}")
                .build();
        FeedbackResponse response = FeedbackResponse.builder().id(feedbackId).userId(userId).build();

        when(feedbackMapper.toEntity(request)).thenReturn(mapped);
        when(feedbackRepository.save(mapped)).thenReturn(feedback);
        when(feedbackMapper.toResponse(feedback)).thenReturn(response);

        FeedbackResponse result = feedbackService.create(userId, request);

        assertEquals(feedbackId, result.getId());
        assertEquals(userId, mapped.getUserId());
    }

    @Test
    void getAll_returnsFilteredPage_whenTypeProvided() {
        PageRequest pageable = PageRequest.of(0, 10);
        Page<Feedback> page = new PageImpl<>(java.util.List.of(feedback), pageable, 1);
        FeedbackResponse response = FeedbackResponse.builder().id(feedbackId).type(FeedbackType.BUG_REPORT).build();

        when(feedbackRepository.findAllByUserIdAndType(userId, FeedbackType.BUG_REPORT, pageable)).thenReturn(page);
        when(feedbackMapper.toResponse(feedback)).thenReturn(response);

        Page<FeedbackResponse> result = feedbackService.getAll(userId, FeedbackType.BUG_REPORT, pageable);

        assertEquals(1, result.getTotalElements());
        assertEquals(FeedbackType.BUG_REPORT, result.getContent().getFirst().getType());
    }

    @Test
    void getById_throws_whenNotFound() {
        when(feedbackRepository.findByIdAndUserId(feedbackId, userId)).thenReturn(Optional.empty());

        assertThrows(FeedbackNotFoundException.class, () -> feedbackService.getById(userId, feedbackId));
    }

    @Test
    void delete_removesOwnedFeedback() {
        when(feedbackRepository.findByIdAndUserId(feedbackId, userId)).thenReturn(Optional.of(feedback));

        feedbackService.delete(userId, feedbackId);

        verify(feedbackRepository).delete(feedback);
    }

    @Test
    void delete_throws_whenNotOwned() {
        when(feedbackRepository.findByIdAndUserId(feedbackId, userId)).thenReturn(Optional.empty());

        assertThrows(FeedbackNotFoundException.class, () -> feedbackService.delete(userId, feedbackId));
    }

    @Test
    void updateStatus_updatesAndNotifies_whenStatusChanges() {
        FeedbackResponse response = FeedbackResponse.builder().id(feedbackId).status(FeedbackStatus.RESOLVED).build();

        when(feedbackRepository.findById(feedbackId)).thenReturn(Optional.of(feedback));
        when(feedbackRepository.save(feedback)).thenReturn(feedback);
        when(feedbackMapper.toResponse(feedback)).thenReturn(response);

        FeedbackResponse result = feedbackService.updateStatus(feedbackId, FeedbackStatus.RESOLVED);

        assertEquals(FeedbackStatus.RESOLVED, result.getStatus());
        assertEquals(FeedbackStatus.RESOLVED, feedback.getStatus());
        verify(feedbackStatusNotifier).notifyStatusChanged(feedback, FeedbackStatus.OPEN);
    }

    @Test
    void updateStatus_throws_whenNotFound() {
        when(feedbackRepository.findById(feedbackId)).thenReturn(Optional.empty());

        assertThrows(FeedbackNotFoundException.class, () -> feedbackService.updateStatus(feedbackId, FeedbackStatus.RESOLVED));
    }
}
