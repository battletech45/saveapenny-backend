package com.saveapenny.stockholding.repository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.saveapenny.stockholding.entity.StockHolding;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.TestPropertySource;

@DataJpaTest
@TestPropertySource(properties = {
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.flyway.enabled=false"
})
class StockHoldingRepositoryTest {

    @Autowired
    private StockHoldingRepository repository;

    private UUID userId;
    private UUID otherUserId;
    private StockHolding ibmHolding;
    private StockHolding aaplHolding;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        otherUserId = UUID.randomUUID();

        ibmHolding = StockHolding.builder()
                .id(UUID.randomUUID())
                .userId(userId)
                .symbol("IBM")
                .quantity(new BigDecimal("10.00000000"))
                .purchasePrice(new BigDecimal("140.0000"))
                .currency("USD")
                .purchaseDate(LocalDate.of(2025, 4, 25))
                .build();

        aaplHolding = StockHolding.builder()
                .id(UUID.randomUUID())
                .userId(userId)
                .symbol("AAPL")
                .quantity(new BigDecimal("5.00000000"))
                .purchasePrice(new BigDecimal("180.0000"))
                .currency("USD")
                .purchaseDate(LocalDate.of(2025, 5, 10))
                .build();

        repository.saveAll(List.of(ibmHolding, aaplHolding));
    }

    @Test
    void findByIdAndUserId_returnsHolding_whenExists() {
        Optional<StockHolding> result = repository.findByIdAndUserId(ibmHolding.getId(), userId);

        assertTrue(result.isPresent());
        assertEquals("IBM", result.get().getSymbol());
    }

    @Test
    void findByIdAndUserId_returnsEmpty_whenWrongUser() {
        Optional<StockHolding> result = repository.findByIdAndUserId(ibmHolding.getId(), otherUserId);

        assertFalse(result.isPresent());
    }

    @Test
    void findByIdAndUserId_returnsEmpty_whenNotFound() {
        Optional<StockHolding> result = repository.findByIdAndUserId(UUID.randomUUID(), userId);

        assertFalse(result.isPresent());
    }

    @Test
    void findAllByUserId_returnsPagedResults() {
        Page<StockHolding> page = repository.findAllByUserId(userId, PageRequest.of(0, 20));

        assertEquals(2, page.getTotalElements());
    }

    @Test
    void findAllByUserId_returnsEmptyForWrongUser() {
        Page<StockHolding> page = repository.findAllByUserId(otherUserId, PageRequest.of(0, 20));

        assertEquals(0, page.getTotalElements());
    }

    @Test
    void findAllByUserId_returnsAllHoldings() {
        List<StockHolding> holdings = repository.findAllByUserId(userId);

        assertEquals(2, holdings.size());
    }

    @Test
    void findAllByUserIdAndSymbol_filtersBySymbol() {
        List<StockHolding> holdings = repository.findAllByUserIdAndSymbol(userId, "IBM");

        assertEquals(1, holdings.size());
        assertEquals("IBM", holdings.get(0).getSymbol());
    }

    @Test
    void existsByUserIdAndSymbolAndPurchaseDate_returnsTrueForExisting() {
        boolean exists = repository.existsByUserIdAndSymbolAndPurchaseDate(
                userId, "IBM", LocalDate.of(2025, 4, 25));

        assertTrue(exists);
    }

    @Test
    void existsByUserIdAndSymbolAndPurchaseDate_returnsFalseForDifferentDate() {
        boolean exists = repository.existsByUserIdAndSymbolAndPurchaseDate(
                userId, "IBM", LocalDate.of(2025, 4, 26));

        assertFalse(exists);
    }

    @Test
    void deleteByIdAndUserId_deletesHolding() {
        int deleted = repository.deleteByIdAndUserId(ibmHolding.getId(), userId);

        assertEquals(1, deleted);
        Optional<StockHolding> result = repository.findByIdAndUserId(ibmHolding.getId(), userId);
        assertFalse(result.isPresent());
    }

    @Test
    void deleteByIdAndUserId_returnsZeroForWrongUser() {
        int deleted = repository.deleteByIdAndUserId(ibmHolding.getId(), otherUserId);

        assertEquals(0, deleted);
        Optional<StockHolding> result = repository.findByIdAndUserId(ibmHolding.getId(), userId);
        assertTrue(result.isPresent());
    }

    @Test
    void save_persistsAllFields() {
        StockHolding holding = StockHolding.builder()
                .id(UUID.randomUUID())
                .userId(userId)
                .symbol("GOOGL")
                .quantity(new BigDecimal("3.50000000"))
                .purchasePrice(new BigDecimal("185.5000"))
                .currency("USD")
                .purchaseDate(LocalDate.of(2026, 1, 15))
                .notes("Late entry")
                .build();

        StockHolding saved = repository.save(holding);

        assertNotNull(saved.getId());
        assertEquals("GOOGL", saved.getSymbol());
        assertEquals(new BigDecimal("3.50000000"), saved.getQuantity());
        assertEquals(new BigDecimal("185.5000"), saved.getPurchasePrice());
        assertEquals("USD", saved.getCurrency());
        assertEquals(LocalDate.of(2026, 1, 15), saved.getPurchaseDate());
        assertEquals("Late entry", saved.getNotes());
        assertNotNull(saved.getCreatedAt());
        assertNotNull(saved.getUpdatedAt());
    }
}
