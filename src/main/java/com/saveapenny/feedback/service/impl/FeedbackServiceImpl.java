package com.saveapenny.feedback.service.impl;

import com.saveapenny.feedback.dto.CreateFeedbackRequest;
import com.saveapenny.feedback.dto.FeedbackResponse;
import com.saveapenny.feedback.entity.Feedback;
import com.saveapenny.feedback.entity.FeedbackType;
import com.saveapenny.feedback.exception.FeedbackNotFoundException;
import com.saveapenny.feedback.mapper.FeedbackMapper;
import com.saveapenny.feedback.repository.FeedbackRepository;
import com.saveapenny.feedback.service.FeedbackService;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class FeedbackServiceImpl implements FeedbackService {

    private final FeedbackRepository feedbackRepository;
    private final FeedbackMapper feedbackMapper;

    public FeedbackServiceImpl(FeedbackRepository feedbackRepository, FeedbackMapper feedbackMapper) {
        this.feedbackRepository = feedbackRepository;
        this.feedbackMapper = feedbackMapper;
    }

    @Override
    public FeedbackResponse create(UUID currentUserId, CreateFeedbackRequest request) {
        Feedback feedback = feedbackMapper.toEntity(request);
        feedback.setUserId(currentUserId);
        Feedback saved = feedbackRepository.save(feedback);
        return feedbackMapper.toResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<FeedbackResponse> getAll(UUID currentUserId, FeedbackType type, Pageable pageable) {
        Page<Feedback> page = type == null
                ? feedbackRepository.findAllByUserId(currentUserId, pageable)
                : feedbackRepository.findAllByUserIdAndType(currentUserId, type, pageable);
        return page.map(feedbackMapper::toResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public FeedbackResponse getById(UUID currentUserId, UUID feedbackId) {
        Feedback feedback = findOwnedFeedback(currentUserId, feedbackId);
        return feedbackMapper.toResponse(feedback);
    }

    @Override
    public void delete(UUID currentUserId, UUID feedbackId) {
        Feedback feedback = findOwnedFeedback(currentUserId, feedbackId);
        feedbackRepository.delete(feedback);
    }

    private Feedback findOwnedFeedback(UUID currentUserId, UUID feedbackId) {
        return feedbackRepository.findByIdAndUserId(feedbackId, currentUserId)
                .orElseThrow(() -> new FeedbackNotFoundException(feedbackId));
    }
}
