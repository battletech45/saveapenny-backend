package com.saveapenny.stockholding.mapper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import com.saveapenny.stockholding.dto.CreateHoldingRequest;
import com.saveapenny.stockholding.dto.HoldingResponse;
import com.saveapenny.stockholding.dto.UpdateHoldingRequest;
import com.saveapenny.stockholding.entity.StockHolding;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class StockHoldingMapperTest {

    private StockHoldingMapper mapper;

    @BeforeEach
    void setUp() {
        mapper = new StockHoldingMapperImpl();
    }

    @Test
    void toEntity_mapsAllFields() {
        CreateHoldingRequest request = CreateHoldingRequest.builder()
                .symbol("IBM")
                .quantity(new BigDecimal("10.00000000"))
                .purchasePrice(new BigDecimal("140.0000"))
                .currency("USD")
                .purchaseDate(LocalDate.of(2025, 4, 25))
                .notes("First position")
                .build();

        StockHolding entity = mapper.toEntity(request);

        assertEquals(new BigDecimal("10.00000000"), entity.getQuantity());
        assertEquals(new BigDecimal("140.0000"), entity.getPurchasePrice());
        assertEquals("USD", entity.getCurrency());
        assertEquals(LocalDate.of(2025, 4, 25), entity.getPurchaseDate());
        assertEquals("First position", entity.getNotes());
        assertNull(entity.getId());
        assertNull(entity.getUserId());
        assertNull(entity.getCreatedAt());
        assertNull(entity.getUpdatedAt());
    }

    @Test
    void toEntity_doesNotSetIdOrTimestamps() {
        CreateHoldingRequest request = CreateHoldingRequest.builder()
                .symbol("IBM")
                .quantity(new BigDecimal("10.00000000"))
                .purchasePrice(new BigDecimal("140.0000"))
                .currency("USD")
                .purchaseDate(LocalDate.of(2025, 4, 25))
                .build();

        StockHolding entity = mapper.toEntity(request);

        assertNull(entity.getId());
        assertNull(entity.getUserId());
        assertNull(entity.getCreatedAt());
        assertNull(entity.getUpdatedAt());
        assertNull(entity.getVersion());
    }

    @Test
    void updateEntity_mapsNonNullFields() {
        StockHolding entity = new StockHolding();
        entity.setId(UUID.randomUUID());
        entity.setUserId(UUID.randomUUID());
        entity.setSymbol("IBM");
        entity.setQuantity(new BigDecimal("10.00000000"));
        entity.setPurchasePrice(new BigDecimal("140.0000"));
        entity.setCurrency("USD");
        entity.setPurchaseDate(LocalDate.of(2025, 4, 25));
        entity.setNotes("Original");

        UpdateHoldingRequest request = UpdateHoldingRequest.builder()
                .quantity(new BigDecimal("15.00000000"))
                .notes("Updated")
                .build();

        mapper.updateEntity(entity, request);

        assertEquals(new BigDecimal("15.00000000"), entity.getQuantity());
        assertEquals(new BigDecimal("140.0000"), entity.getPurchasePrice());
        assertEquals("USD", entity.getCurrency());
        assertEquals(LocalDate.of(2025, 4, 25), entity.getPurchaseDate());
        assertEquals("Updated", entity.getNotes());
        assertEquals("IBM", entity.getSymbol());
    }

    @Test
    void updateEntity_doesNotOverwriteWithNull() {
        StockHolding entity = new StockHolding();
        entity.setQuantity(new BigDecimal("10.00000000"));
        entity.setPurchasePrice(new BigDecimal("140.0000"));
        entity.setCurrency("USD");

        UpdateHoldingRequest request = UpdateHoldingRequest.builder()
                .quantity(null)
                .purchasePrice(null)
                .currency(null)
                .purchaseDate(null)
                .notes(null)
                .build();

        mapper.updateEntity(entity, request);

        assertEquals(new BigDecimal("10.00000000"), entity.getQuantity());
        assertEquals(new BigDecimal("140.0000"), entity.getPurchasePrice());
        assertEquals("USD", entity.getCurrency());
    }

    @Test
    void toResponse_mapsStaticFields() {
        StockHolding entity = StockHolding.builder()
                .id(UUID.randomUUID())
                .symbol("IBM")
                .quantity(new BigDecimal("10.00000000"))
                .purchasePrice(new BigDecimal("140.0000"))
                .currency("USD")
                .purchaseDate(LocalDate.of(2025, 4, 25))
                .notes("Test")
                .createdAt(OffsetDateTime.now().minusDays(3))
                .updatedAt(OffsetDateTime.now())
                .build();

        HoldingResponse response = mapper.toResponse(entity);

        assertEquals(entity.getId(), response.getId());
        assertEquals("IBM", response.getSymbol());
        assertEquals(new BigDecimal("10.00000000"), response.getQuantity());
        assertEquals(new BigDecimal("140.0000"), response.getPurchasePrice());
        assertEquals("USD", response.getCurrency());
        assertEquals(LocalDate.of(2025, 4, 25), response.getPurchaseDate());
        assertEquals("Test", response.getNotes());
        assertEquals(entity.getCreatedAt(), response.getCreatedAt());
        assertEquals(entity.getUpdatedAt(), response.getUpdatedAt());
    }

    @Test
    void toResponse_doesNotSetMarketFields() {
        StockHolding entity = StockHolding.builder()
                .id(UUID.randomUUID())
                .symbol("IBM")
                .quantity(new BigDecimal("10.00000000"))
                .purchasePrice(new BigDecimal("140.0000"))
                .currency("USD")
                .purchaseDate(LocalDate.of(2025, 4, 25))
                .build();

        HoldingResponse response = mapper.toResponse(entity);

        assertNull(response.getInvestedAmount());
        assertNull(response.getCurrentPrice());
        assertNull(response.getCurrentValue());
        assertNull(response.getProfitLoss());
        assertNull(response.getProfitLossPercent());
        assertNull(response.getLatestTradingDay());
    }

    @Test
    void toResponse_handlesMinimalEntity() {
        StockHolding entity = StockHolding.builder()
                .symbol("IBM")
                .build();

        HoldingResponse response = mapper.toResponse(entity);

        assertNotNull(response);
        assertNull(response.getId());
        assertEquals("IBM", response.getSymbol());
    }
}
