package com.saveapenny.insight.service.impl;

import com.saveapenny.insight.dto.GenerateInsightsRequest;
import com.saveapenny.insight.dto.InsightResponse;
import com.saveapenny.insight.entity.InsightEntity;
import com.saveapenny.insight.entity.InsightType;
import com.saveapenny.insight.exception.InsightGenerationException;
import com.saveapenny.insight.exception.InsightNotFoundException;
import com.saveapenny.insight.mapper.InsightMapper;
import com.saveapenny.insight.repository.InsightRepository;
import com.saveapenny.insight.service.InsightService;
import com.saveapenny.billing.service.BillingAccessService;
import com.saveapenny.shared.api.PagedResponse;
import com.saveapenny.shared.api.PagedResponses;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class InsightServiceImpl implements InsightService {

    private final InsightRepository insightRepository;
    private final InsightMapper insightMapper;
    private final InsightGenerationPipeline insightGenerationPipeline;
    private final BillingAccessService billingAccessService;

    public InsightServiceImpl(
            InsightRepository insightRepository,
            InsightMapper insightMapper,
            InsightGenerationPipeline insightGenerationPipeline,
            BillingAccessService billingAccessService) {
        this.insightRepository = insightRepository;
        this.insightMapper = insightMapper;
        this.insightGenerationPipeline = insightGenerationPipeline;
        this.billingAccessService = billingAccessService;
    }

    @Override
    @Transactional(readOnly = true)
    public PagedResponse<InsightResponse> getAll(UUID currentUserId, InsightType type, String severity, Boolean isRead, Pageable pageable) {
        Page<InsightEntity> page;

        if (type != null && severity != null && isRead != null) {
            page = insightRepository.findAllByUserIdAndTypeAndSeverityAndRead(currentUserId, type, severity, isRead, pageable);
        } else if (type != null && severity != null) {
            page = insightRepository.findAllByUserIdAndTypeAndSeverity(currentUserId, type, severity, pageable);
        } else if (type != null && isRead != null) {
            page = insightRepository.findAllByUserIdAndTypeAndRead(currentUserId, type, isRead, pageable);
        } else if (severity != null && isRead != null) {
            page = insightRepository.findAllByUserIdAndSeverityAndRead(currentUserId, severity, isRead, pageable);
        } else if (type != null) {
            page = insightRepository.findAllByUserIdAndType(currentUserId, type, pageable);
        } else if (severity != null) {
            page = insightRepository.findAllByUserIdAndSeverity(currentUserId, severity, pageable);
        } else if (isRead != null) {
            page = insightRepository.findAllByUserIdAndRead(currentUserId, isRead, pageable);
        } else {
            page = insightRepository.findAllByUserId(currentUserId, pageable);
        }

        return PagedResponses.from(page.map(insightMapper::toResponse));
    }

    @Override
    @Transactional(readOnly = true)
    public InsightResponse getById(UUID currentUserId, UUID insightId) {
        return insightMapper.toResponse(findOwnedInsight(currentUserId, insightId));
    }

    @Override
    public InsightResponse markAsRead(UUID currentUserId, UUID insightId) {
        InsightEntity entity = findOwnedInsight(currentUserId, insightId);
        entity.setRead(true);
        return insightMapper.toResponse(insightRepository.save(entity));
    }

    @Override
    public InsightResponse dismiss(UUID currentUserId, UUID insightId) {
        InsightEntity entity = findOwnedInsight(currentUserId, insightId);
        entity.setDismissed(true);
        return insightMapper.toResponse(insightRepository.save(entity));
    }

    @Override
    public int generate(UUID currentUserId, GenerateInsightsRequest request) {
        billingAccessService.requireFeature(currentUserId, "insights");
        try {
            return insightGenerationPipeline.execute(currentUserId);
        } catch (RuntimeException ex) {
            throw new InsightGenerationException("Failed to generate insights for userId=" + currentUserId, ex);
        }
    }

    private InsightEntity findOwnedInsight(UUID userId, UUID insightId) {
        return insightRepository.findByIdAndUserId(insightId, userId)
                .orElseThrow(() -> new InsightNotFoundException(insightId));
    }
}
