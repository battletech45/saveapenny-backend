package com.saveapenny.stockholding.service;

import com.saveapenny.stockholding.dto.CreateHoldingRequest;
import com.saveapenny.stockholding.dto.HoldingResponse;
import com.saveapenny.stockholding.dto.HoldingSummaryResponse;
import com.saveapenny.stockholding.dto.UpdateHoldingRequest;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface StockHoldingService {

    HoldingResponse create(UUID userId, CreateHoldingRequest request);

    HoldingResponse getById(UUID userId, UUID holdingId);

    Page<HoldingResponse> getAll(UUID userId, Pageable pageable);

    HoldingSummaryResponse getSummary(UUID userId);

    HoldingResponse update(UUID userId, UUID holdingId, UpdateHoldingRequest request);

    void delete(UUID userId, UUID holdingId);
}
