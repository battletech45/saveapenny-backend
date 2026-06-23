package com.saveapenny.stockholding.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.saveapenny.stock.dto.StockQuoteResponse;
import com.saveapenny.stock.exception.StockClientException;
import com.saveapenny.stock.exception.StockDisabledException;
import com.saveapenny.stock.exception.StockQuoteNotAvailableException;
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
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
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
class StockHoldingServiceTest {

    @Mock
    private StockHoldingRepository repository;
    @Mock
    private StockHoldingMapper mapper;
    @Mock
    private StockService stockService;

    @InjectMocks
    private StockHoldingServiceImpl service;

    private UUID userId;
    private UUID holdingId;
    private StockHolding entity;
    private StockQuoteResponse quote;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        holdingId = UUID.randomUUID();

        entity = StockHolding.builder()
                .id(holdingId)
                .userId(userId)
                .symbol("IBM")
                .quantity(new BigDecimal("10.00000000"))
                .purchasePrice(new BigDecimal("140.0000"))
                .currency("USD")
                .purchaseDate(LocalDate.of(2025, 4, 25))
                .createdAt(OffsetDateTime.now().minusDays(1))
                .updatedAt(OffsetDateTime.now())
                .build();

        quote = StockQuoteResponse.builder()
                .symbol("IBM")
                .price(new BigDecimal("175.42"))
                .latestTradingDay(LocalDate.of(2026, 6, 20))
                .build();
    }

    private HoldingResponse sampleResponse() {
        return HoldingResponse.builder()
                .id(holdingId)
                .symbol("IBM")
                .quantity(new BigDecimal("10.00000000"))
                .purchasePrice(new BigDecimal("140.0000"))
                .currency("USD")
                .purchaseDate(LocalDate.of(2025, 4, 25))
                .investedAmount(new BigDecimal("1400.00"))
                .build();
    }

    @Test
    void create_returnsHoldingWithLivePl() {
        CreateHoldingRequest request = CreateHoldingRequest.builder()
                .symbol("ibm")
                .quantity(new BigDecimal("10.00000000"))
                .purchasePrice(new BigDecimal("140.0000"))
                .currency("USD")
                .purchaseDate(LocalDate.of(2025, 4, 25))
                .build();

        StockHolding mapped = StockHolding.builder()
                .quantity(request.getQuantity())
                .purchasePrice(request.getPurchasePrice())
                .currency(request.getCurrency())
                .purchaseDate(request.getPurchaseDate())
                .build();

        StockHolding saved = StockHolding.builder()
                .id(holdingId)
                .userId(userId)
                .symbol("IBM")
                .quantity(new BigDecimal("10.00000000"))
                .purchasePrice(new BigDecimal("140.0000"))
                .currency("USD")
                .purchaseDate(LocalDate.of(2025, 4, 25))
                .build();

        when(stockService.getQuote("IBM")).thenReturn(quote);
        when(repository.existsByUserIdAndSymbolAndPurchaseDate(userId, "IBM", request.getPurchaseDate())).thenReturn(false);
        when(mapper.toEntity(request)).thenReturn(mapped);
        when(repository.save(mapped)).thenReturn(saved);
        when(mapper.toResponse(saved)).thenReturn(sampleResponse());

        HoldingResponse result = service.create(userId, request);

        assertNotNull(result);
        assertEquals(holdingId, result.getId());
        assertNotNull(result.getCurrentPrice());
        assertEquals(0, new BigDecimal("175.42").compareTo(result.getCurrentPrice()));
        assertNotNull(result.getCurrentValue());
        assertEquals(0, new BigDecimal("1754.20").compareTo(result.getCurrentValue()));
        assertNotNull(result.getProfitLoss());
        assertEquals(0, new BigDecimal("354.20").compareTo(result.getProfitLoss()));
        assertEquals(0, new BigDecimal("25.30").compareTo(result.getProfitLossPercent()));
        assertEquals("IBM", mapped.getSymbol());
        assertEquals(userId, mapped.getUserId());
    }

    @Test
    void create_throws_whenDuplicate() {
        CreateHoldingRequest request = CreateHoldingRequest.builder()
                .symbol("IBM")
                .quantity(new BigDecimal("10.00000000"))
                .purchasePrice(new BigDecimal("140.0000"))
                .currency("USD")
                .purchaseDate(LocalDate.of(2025, 4, 25))
                .build();

        when(stockService.getQuote("IBM")).thenReturn(quote);
        when(repository.existsByUserIdAndSymbolAndPurchaseDate(userId, "IBM", request.getPurchaseDate())).thenReturn(true);

        assertThrows(DuplicateHoldingException.class, () -> service.create(userId, request));
    }

    @Test
    void create_throws_whenSymbolInvalid() {
        CreateHoldingRequest request = CreateHoldingRequest.builder()
                .symbol("@INVALID")
                .quantity(new BigDecimal("10.00000000"))
                .purchasePrice(new BigDecimal("140.0000"))
                .currency("USD")
                .purchaseDate(LocalDate.of(2025, 4, 25))
                .build();

        assertThrows(InvalidHoldingSymbolException.class, () -> service.create(userId, request));
    }

    @Test
    void create_throws_whenSymbolBlank() {
        CreateHoldingRequest request = CreateHoldingRequest.builder()
                .symbol("")
                .quantity(new BigDecimal("10.00000000"))
                .purchasePrice(new BigDecimal("140.0000"))
                .currency("USD")
                .purchaseDate(LocalDate.of(2025, 4, 25))
                .build();

        assertThrows(InvalidHoldingSymbolException.class, () -> service.create(userId, request));
    }

    @Test
    void create_throws_whenSymbolTooLong() {
        CreateHoldingRequest request = CreateHoldingRequest.builder()
                .symbol("ABCDEFGHIJKLMNO")
                .quantity(new BigDecimal("10.00000000"))
                .purchasePrice(new BigDecimal("140.0000"))
                .currency("USD")
                .purchaseDate(LocalDate.of(2025, 4, 25))
                .build();

        assertThrows(InvalidHoldingSymbolException.class, () -> service.create(userId, request));
    }

    @Test
    void create_throws_whenStocksDisabled() {
        CreateHoldingRequest request = CreateHoldingRequest.builder()
                .symbol("IBM")
                .quantity(new BigDecimal("10.00000000"))
                .purchasePrice(new BigDecimal("140.0000"))
                .currency("USD")
                .purchaseDate(LocalDate.of(2025, 4, 25))
                .build();

        when(stockService.getQuote("IBM")).thenThrow(new StockDisabledException("Stock market feature is not enabled."));

        assertThrows(StockDisabledException.class, () -> service.create(userId, request));
    }

    @Test
    void create_throws_whenSymbolNotFoundInMarket() {
        CreateHoldingRequest request = CreateHoldingRequest.builder()
                .symbol("UNKNOWN")
                .quantity(new BigDecimal("10.00000000"))
                .purchasePrice(new BigDecimal("140.0000"))
                .currency("USD")
                .purchaseDate(LocalDate.of(2025, 4, 25))
                .build();

        when(stockService.getQuote("UNKNOWN")).thenThrow(new StockQuoteNotAvailableException("No data"));

        assertThrows(HoldingNotFoundException.class, () -> service.create(userId, request));
    }

    @Test
    void create_handlesProviderErrorGracefully() {
        CreateHoldingRequest request = CreateHoldingRequest.builder()
                .symbol("IBM")
                .quantity(new BigDecimal("10.00000000"))
                .purchasePrice(new BigDecimal("140.0000"))
                .currency("USD")
                .purchaseDate(LocalDate.of(2025, 4, 25))
                .build();

        StockHolding saved = StockHolding.builder()
                .id(holdingId)
                .userId(userId)
                .symbol("IBM")
                .quantity(new BigDecimal("10.00000000"))
                .purchasePrice(new BigDecimal("140.0000"))
                .currency("USD")
                .purchaseDate(LocalDate.of(2025, 4, 25))
                .build();

        when(stockService.getQuote("IBM")).thenReturn(quote);
        when(repository.existsByUserIdAndSymbolAndPurchaseDate(userId, "IBM", request.getPurchaseDate())).thenReturn(false);
        when(mapper.toEntity(request)).thenReturn(StockHolding.builder().build());
        when(repository.save(any())).thenReturn(saved);
        when(mapper.toResponse(saved)).thenReturn(sampleResponse());

        HoldingResponse result = service.create(userId, request);

        assertNotNull(result);
        assertEquals(0, new BigDecimal("175.42").compareTo(result.getCurrentPrice()));
    }

    @Test
    void getById_returnsHoldingWithPl() {
        when(repository.findByIdAndUserId(holdingId, userId)).thenReturn(Optional.of(entity));
        when(mapper.toResponse(entity)).thenReturn(sampleResponse());
        when(stockService.getQuote("IBM")).thenReturn(quote);

        HoldingResponse result = service.getById(userId, holdingId);

        assertNotNull(result);
        assertNotNull(result.getCurrentPrice());
        assertEquals(0, new BigDecimal("175.42").compareTo(result.getCurrentPrice()));
    }

    @Test
    void getById_throws_whenNotFound() {
        when(repository.findByIdAndUserId(holdingId, userId)).thenReturn(Optional.empty());

        assertThrows(HoldingNotFoundException.class, () -> service.getById(userId, holdingId));
    }

    @Test
    void getById_gracefulDegradation_whenQuoteFails() {
        when(repository.findByIdAndUserId(holdingId, userId)).thenReturn(Optional.of(entity));
        when(mapper.toResponse(entity)).thenReturn(sampleResponse());
        when(stockService.getQuote("IBM")).thenThrow(new StockClientException("Provider error"));

        HoldingResponse result = service.getById(userId, holdingId);

        assertNotNull(result);
        assertNull(result.getCurrentPrice());
        assertNull(result.getCurrentValue());
        assertNull(result.getProfitLoss());
    }

    @Test
    void getAll_returnsPagedHoldingsWithPl() {
        PageRequest pageable = PageRequest.of(0, 20);
        when(repository.findAllByUserId(userId, pageable)).thenReturn(new PageImpl<>(List.of(entity), pageable, 1));
        when(mapper.toResponse(entity)).thenReturn(sampleResponse());
        when(stockService.getQuote("IBM")).thenReturn(quote);

        Page<HoldingResponse> result = service.getAll(userId, pageable);

        assertEquals(1, result.getTotalElements());
        assertNotNull(result.getContent().get(0).getCurrentPrice());
        assertEquals(0, new BigDecimal("175.42").compareTo(result.getContent().get(0).getCurrentPrice()));
    }

    @Test
    void getAll_cachesQuotesPerSymbol() {
        StockHolding secondEntity = StockHolding.builder()
                .id(UUID.randomUUID())
                .userId(userId)
                .symbol("IBM")
                .quantity(new BigDecimal("5.00000000"))
                .purchasePrice(new BigDecimal("150.0000"))
                .currency("USD")
                .purchaseDate(LocalDate.of(2025, 5, 10))
                .build();

        PageRequest pageable = PageRequest.of(0, 20);
        when(repository.findAllByUserId(userId, pageable)).thenReturn(new PageImpl<>(List.of(entity, secondEntity), pageable, 2));
        when(mapper.toResponse(entity)).thenReturn(sampleResponse());
        when(mapper.toResponse(secondEntity)).thenReturn(HoldingResponse.builder().id(secondEntity.getId()).quantity(secondEntity.getQuantity()).purchasePrice(secondEntity.getPurchasePrice()).investedAmount(new BigDecimal("750.00")).build());
        when(stockService.getQuote("IBM")).thenReturn(quote);

        Page<HoldingResponse> result = service.getAll(userId, pageable);

        assertEquals(2, result.getTotalElements());
        verify(stockService).getQuote("IBM");
    }

    @Test
    void getSummary_aggregatesAcrossHoldings() {
        StockHolding secondEntity = StockHolding.builder()
                .id(UUID.randomUUID())
                .userId(userId)
                .symbol("AAPL")
                .quantity(new BigDecimal("5.00000000"))
                .purchasePrice(new BigDecimal("180.0000"))
                .currency("USD")
                .purchaseDate(LocalDate.of(2025, 5, 10))
                .build();

        StockQuoteResponse aaplQuote = StockQuoteResponse.builder()
                .symbol("AAPL")
                .price(new BigDecimal("200.00"))
                .latestTradingDay(LocalDate.of(2026, 6, 20))
                .build();

        when(repository.findAllByUserId(userId)).thenReturn(List.of(entity, secondEntity));
        when(mapper.toResponse(entity)).thenReturn(HoldingResponse.builder()
                .id(entity.getId())
                .symbol("IBM")
                .quantity(new BigDecimal("10.00000000"))
                .purchasePrice(new BigDecimal("140.0000"))
                .investedAmount(new BigDecimal("1400.00"))
                .build());
        when(mapper.toResponse(secondEntity)).thenReturn(HoldingResponse.builder()
                .id(secondEntity.getId())
                .symbol("AAPL")
                .quantity(new BigDecimal("5.00000000"))
                .purchasePrice(new BigDecimal("180.0000"))
                .investedAmount(new BigDecimal("900.00"))
                .build());
        when(stockService.getQuote("IBM")).thenReturn(quote);
        when(stockService.getQuote("AAPL")).thenReturn(aaplQuote);

        HoldingSummaryResponse result = service.getSummary(userId);

        assertEquals(2, result.getHoldingCount());
        assertEquals(0, new BigDecimal("2300.00").compareTo(result.getTotalInvested()));
        assertEquals(0, new BigDecimal("2754.20").compareTo(result.getTotalCurrentValue()));
        assertEquals(0, new BigDecimal("454.20").compareTo(result.getTotalProfitLoss()));
        assertEquals(0, new BigDecimal("19.75").compareTo(result.getTotalProfitLossPercent()));
    }

    @Test
    void getSummary_returnsNullPl_whenNoMarketData() {
        when(repository.findAllByUserId(userId)).thenReturn(List.of(entity));
        when(mapper.toResponse(entity)).thenReturn(HoldingResponse.builder()
                .id(entity.getId())
                .quantity(new BigDecimal("10.00000000"))
                .purchasePrice(new BigDecimal("140.0000"))
                .investedAmount(new BigDecimal("1400.00"))
                .build());
        when(stockService.getQuote("IBM")).thenThrow(new StockClientException("Provider error"));

        HoldingSummaryResponse result = service.getSummary(userId);

        assertEquals(1, result.getHoldingCount());
        assertEquals(0, new BigDecimal("1400.00").compareTo(result.getTotalInvested()));
        assertNull(result.getTotalProfitLoss());
    }

    @Test
    void getSummary_returnsEmptyForNoHoldings() {
        when(repository.findAllByUserId(userId)).thenReturn(List.of());

        HoldingSummaryResponse result = service.getSummary(userId);

        assertEquals(0, result.getHoldingCount());
        assertEquals(BigDecimal.ZERO, result.getTotalInvested());
        assertEquals(BigDecimal.ZERO, result.getTotalCurrentValue());
        assertNull(result.getTotalProfitLoss());
    }

    @Test
    void update_partiallyUpdatesProvidedFields() {
        UpdateHoldingRequest request = UpdateHoldingRequest.builder()
                .quantity(new BigDecimal("15.00000000"))
                .notes("Increased position")
                .build();

        when(repository.findByIdAndUserId(holdingId, userId)).thenReturn(Optional.of(entity));
        when(repository.save(entity)).thenReturn(entity);
        when(mapper.toResponse(entity)).thenReturn(sampleResponse());
        when(stockService.getQuote("IBM")).thenReturn(quote);

        HoldingResponse result = service.update(userId, holdingId, request);

        assertNotNull(result);
        verify(mapper).updateEntity(entity, request);
    }

    @Test
    void update_throws_whenChangingToDuplicateDate() {
        UpdateHoldingRequest request = UpdateHoldingRequest.builder()
                .purchaseDate(LocalDate.of(2025, 5, 1))
                .build();

        when(repository.findByIdAndUserId(holdingId, userId)).thenReturn(Optional.of(entity));
        when(repository.existsByUserIdAndSymbolAndPurchaseDate(userId, "IBM", LocalDate.of(2025, 5, 1))).thenReturn(true);

        assertThrows(DuplicateHoldingException.class, () -> service.update(userId, holdingId, request));
    }

    @Test
    void update_throws_whenNotFound() {
        UpdateHoldingRequest request = UpdateHoldingRequest.builder()
                .quantity(new BigDecimal("15.00000000"))
                .build();

        when(repository.findByIdAndUserId(holdingId, userId)).thenReturn(Optional.empty());

        assertThrows(HoldingNotFoundException.class, () -> service.update(userId, holdingId, request));
    }

    @Test
    void update_doesNotCheckDuplicate_whenSameDate() {
        UpdateHoldingRequest request = UpdateHoldingRequest.builder()
                .purchaseDate(LocalDate.of(2025, 4, 25))
                .build();

        when(repository.findByIdAndUserId(holdingId, userId)).thenReturn(Optional.of(entity));
        when(repository.save(entity)).thenReturn(entity);
        when(mapper.toResponse(entity)).thenReturn(sampleResponse());
        when(stockService.getQuote("IBM")).thenReturn(quote);

        HoldingResponse result = service.update(userId, holdingId, request);

        assertNotNull(result);
    }

    @Test
    void delete_removesHolding() {
        when(repository.findByIdAndUserId(holdingId, userId)).thenReturn(Optional.of(entity));

        service.delete(userId, holdingId);

        verify(repository).deleteByIdAndUserId(holdingId, userId);
    }

    @Test
    void delete_throws_whenNotFound() {
        when(repository.findByIdAndUserId(holdingId, userId)).thenReturn(Optional.empty());

        assertThrows(HoldingNotFoundException.class, () -> service.delete(userId, holdingId));
    }
}
