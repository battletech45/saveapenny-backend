package com.saveapenny.feedback.service;

import com.saveapenny.feedback.dto.CreateFeedbackRequest;
import com.saveapenny.feedback.dto.FeedbackResponse;
import com.saveapenny.feedback.entity.FeedbackType;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface FeedbackService {

    FeedbackResponse create(UUID currentUserId, CreateFeedbackRequest request);

    Page<FeedbackResponse> getAll(UUID currentUserId, FeedbackType type, Pageable pageable);

    FeedbackResponse getById(UUID currentUserId, UUID feedbackId);

    void delete(UUID currentUserId, UUID feedbackId);
}
