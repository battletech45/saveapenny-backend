package com.saveapenny.stockholding.service.impl;

import com.saveapenny.stock.dto.StockQuoteResponse;
import com.saveapenny.stock.exception.StockClientException;
import com.saveapenny.stock.exception.StockDisabledException;
import com.saveapenny.stock.exception.StockQuoteNotAvailableException;
import com.saveapenny.stock.exception.StockRateLimitExceededException;
import com.saveapenny.stock.service.StockService;
import com.saveapenny.stockholding.dto.CreateHoldingRequest;
import com.saveapenny.stockholding.dto.HoldingResponse;
import com.saveapenny.stockholding.dto.HoldingSummaryResponse;
import com.saveapenny.stockholding.dto.UpdateHoldingRequest;
import com.saveapenny.stockholding.entity.StockHolding;
import com.saveapenny.stockholding.exception.DuplicateHoldingException;
import com.saveapenny.stockholding.exception.HoldingNotFoundException;
import com.saveapenny.stockholding.exception.InvalidHoldingSymbolException;
import com.saveapenny.stockholding.mapper.StockHoldingMapper;
import com.saveapenny.stockholding.repository.StockHoldingRepository;
import com.saveapenny.stockholding.service.StockHoldingService;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Optional;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Pattern;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class StockHoldingServiceImpl implements StockHoldingService {

    private static final BigDecimal ONE_HUNDRED = new BigDecimal("100");
    private static final Pattern SYMBOL_PATTERN = Pattern.compile("^[A-Z0-9.\\-]+$");
    private static final int MAX_SYMBOL_LENGTH = 10;

    private final StockHoldingRepository repository;
    private final StockHoldingMapper mapper;
    private final StockService stockService;

    public StockHoldingServiceImpl(
            StockHoldingRepository repository,
            StockHoldingMapper mapper,
            StockService stockService) {
        this.repository = repository;
        this.mapper = mapper;
        this.stockService = stockService;
    }

    @Override
    public HoldingResponse create(UUID userId, CreateHoldingRequest request) {
        String symbol = normalizeSymbol(request.getSymbol());

        if (repository.existsByUserIdAndSymbolAndPurchaseDate(userId, symbol, request.getPurchaseDate())) {
            throw new DuplicateHoldingException(
                    "A holding for " + symbol + " on " + request.getPurchaseDate() + " already exists.");
        }

        StockQuoteResponse quote = validateSymbolExists(symbol);

        StockHolding entity = mapper.toEntity(request);
        entity.setUserId(userId);
        entity.setSymbol(symbol);

        StockHolding saved = saveHolding(entity, symbol, request.getPurchaseDate());
        return enrichWithMarketData(saved, Optional.of(quote));
    }

    @Override
    @Transactional(readOnly = true)
    public HoldingResponse getById(UUID userId, UUID holdingId) {
        StockHolding holding = findOwnedHolding(userId, holdingId);
        return enrichWithMarketData(holding);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<HoldingResponse> getAll(UUID userId, Pageable pageable) {
        Map<String, Optional<StockQuoteResponse>> quoteCache = new HashMap<>();
        return repository.findAllByUserId(userId, pageable)
                .map(entity -> enrichWithMarketData(entity, quoteCache));
    }

    @Override
    @Transactional(readOnly = true)
    public HoldingSummaryResponse getSummary(UUID userId) {
        List<StockHolding> allHoldings = repository.findAllByUserId(userId);
        Map<String, Optional<StockQuoteResponse>> quoteCache = new HashMap<>();

        List<HoldingResponse> enrichedHoldings = new ArrayList<>();
        BigDecimal totalInvested = BigDecimal.ZERO;
        BigDecimal quotedInvested = BigDecimal.ZERO;
        BigDecimal totalCurrentValue = BigDecimal.ZERO;
        int holdingsWithMarketData = 0;

        for (StockHolding holding : allHoldings) {
            HoldingResponse response = enrichWithMarketData(holding, quoteCache);
            enrichedHoldings.add(response);

            if (response.getInvestedAmount() != null) {
                totalInvested = totalInvested.add(response.getInvestedAmount());
            }
            if (response.getCurrentValue() != null) {
                quotedInvested = quotedInvested.add(response.getInvestedAmount());
                totalCurrentValue = totalCurrentValue.add(response.getCurrentValue());
                holdingsWithMarketData++;
            }
        }

        BigDecimal totalProfitLoss = null;
        BigDecimal totalProfitLossPercent = null;

        if (holdingsWithMarketData > 0 && quotedInvested.compareTo(BigDecimal.ZERO) > 0) {
            totalProfitLoss = totalCurrentValue.subtract(quotedInvested);
            totalProfitLossPercent = totalProfitLoss
                    .multiply(ONE_HUNDRED)
                    .divide(quotedInvested, 2, RoundingMode.HALF_UP);
        }

        return HoldingSummaryResponse.builder()
                .totalInvested(totalInvested)
                .totalCurrentValue(totalCurrentValue)
                .totalProfitLoss(totalProfitLoss)
                .totalProfitLossPercent(totalProfitLossPercent)
                .holdingCount(allHoldings.size())
                .holdings(enrichedHoldings)
                .build();
    }

    @Override
    public HoldingResponse update(UUID userId, UUID holdingId, UpdateHoldingRequest request) {
        StockHolding existing = findOwnedHolding(userId, holdingId);

        if (request.getPurchaseDate() != null
                && !request.getPurchaseDate().equals(existing.getPurchaseDate())
                && repository.existsByUserIdAndSymbolAndPurchaseDate(
                userId, existing.getSymbol(), request.getPurchaseDate())) {
            throw new DuplicateHoldingException(
                    "A holding for " + existing.getSymbol() + " on " + request.getPurchaseDate() + " already exists.");
        }

        mapper.updateEntity(existing, request);

        StockHolding saved = saveHolding(existing, existing.getSymbol(), existing.getPurchaseDate());
        return enrichWithMarketData(saved);
    }

    @Override
    public void delete(UUID userId, UUID holdingId) {
        findOwnedHolding(userId, holdingId);
        repository.deleteByIdAndUserId(holdingId, userId);
    }

    private StockHolding findOwnedHolding(UUID userId, UUID holdingId) {
        return repository.findByIdAndUserId(holdingId, userId)
                .orElseThrow(() -> new HoldingNotFoundException("Stock holding not found."));
    }

    private String normalizeSymbol(String symbol) {
        if (symbol == null || symbol.isBlank()) {
            throw new InvalidHoldingSymbolException("Symbol must not be blank");
        }

        String trimmed = symbol.trim().toUpperCase();

        if (trimmed.length() > MAX_SYMBOL_LENGTH) {
            throw new InvalidHoldingSymbolException(
                    "Symbol must not exceed " + MAX_SYMBOL_LENGTH + " characters");
        }

        if (!SYMBOL_PATTERN.matcher(trimmed).matches()) {
            throw new InvalidHoldingSymbolException(
                    "Symbol contains invalid characters. Allowed: A-Z, 0-9, dot, hyphen");
        }

        return trimmed;
    }

    private StockQuoteResponse validateSymbolExists(String symbol) {
        try {
            return stockService.getQuote(symbol);
        } catch (StockDisabledException e) {
            throw e;
        } catch (StockRateLimitExceededException e) {
            throw e;
        } catch (StockQuoteNotAvailableException e) {
            throw new InvalidHoldingSymbolException("Symbol '" + symbol + "' could not be validated against the market.");
        } catch (StockClientException e) {
            throw e;
        }
    }

    private HoldingResponse enrichWithMarketData(StockHolding entity) {
        return enrichWithMarketData(entity, new HashMap<String, Optional<StockQuoteResponse>>());
    }

    private HoldingResponse enrichWithMarketData(StockHolding entity, Optional<StockQuoteResponse> quote) {
        HoldingResponse response = mapper.toResponse(entity);

        BigDecimal investedAmount = entity.getQuantity().multiply(entity.getPurchasePrice());
        response.setInvestedAmount(investedAmount);

        quote.ifPresent(value -> applyMarketData(response, entity, investedAmount, value));
        return response;
    }

    private HoldingResponse enrichWithMarketData(
            StockHolding entity,
            Map<String, Optional<StockQuoteResponse>> quoteCache) {
        HoldingResponse response = mapper.toResponse(entity);

        BigDecimal investedAmount = entity.getQuantity().multiply(entity.getPurchasePrice());
        response.setInvestedAmount(investedAmount);

        Optional<StockQuoteResponse> quote = quoteCache.computeIfAbsent(entity.getSymbol(), this::fetchQuoteForEnrichment);
        if (quote.isPresent()) {
            applyMarketData(response, entity, investedAmount, quote.get());
        }

        return response;
    }

    private Optional<StockQuoteResponse> fetchQuoteForEnrichment(String symbol) {
        try {
            return Optional.of(stockService.getQuote(symbol));
        } catch (StockDisabledException e) {
            throw e;
        } catch (StockQuoteNotAvailableException | StockClientException | StockRateLimitExceededException e) {
            return Optional.empty();
        }
    }

    private void applyMarketData(
            HoldingResponse response,
            StockHolding entity,
            BigDecimal investedAmount,
            StockQuoteResponse quote) {
        response.setCurrentPrice(quote.getPrice());
        response.setCurrentValue(entity.getQuantity().multiply(quote.getPrice()));
        response.setProfitLoss(response.getCurrentValue().subtract(investedAmount));
        response.setLatestTradingDay(quote.getLatestTradingDay());

        if (investedAmount.compareTo(BigDecimal.ZERO) > 0) {
            response.setProfitLossPercent(
                    response.getProfitLoss()
                            .multiply(ONE_HUNDRED)
                            .divide(investedAmount, 2, RoundingMode.HALF_UP));
        }
    }

    private StockHolding saveHolding(StockHolding entity, String symbol, java.time.LocalDate purchaseDate) {
        try {
            return repository.save(entity);
        } catch (DataIntegrityViolationException e) {
            if (isDuplicateHoldingViolation(e)) {
                throw new DuplicateHoldingException(
                        "A holding for " + symbol + " on " + purchaseDate + " already exists.");
            }
            throw e;
        }
    }

    private boolean isDuplicateHoldingViolation(DataIntegrityViolationException e) {
        String message = e.getMostSpecificCause() != null
                ? e.getMostSpecificCause().getMessage()
                : e.getMessage();

        if (message == null) {
            return false;
        }

        String normalized = message.toLowerCase();
        return normalized.contains("uq_stock_holdings_user_symbol_date")
                || (normalized.contains("stock_holdings") && normalized.contains("unique"));
    }
}
