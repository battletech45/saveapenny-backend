package com.saveapenny.transaction.mapper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import com.saveapenny.transaction.dto.TransactionResponse;
import com.saveapenny.transaction.entity.Transaction;
import com.saveapenny.transaction.entity.TransactionType;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

class TransactionMapperTest {

    private final TransactionMapper transactionMapper = Mappers.getMapper(TransactionMapper.class);

    @Test
    void toResponse_mapsAllFields() {
        UUID id = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        UUID accountId = UUID.randomUUID();
        UUID categoryId = UUID.randomUUID();
        OffsetDateTime now = OffsetDateTime.now();

        Transaction entity = Transaction.builder()
                .id(id)
                .userId(userId)
                .accountId(accountId)
                .categoryId(categoryId)
                .type(TransactionType.EXPENSE)
                .amount(new BigDecimal("150.0000"))
                .currency("USD")
                .description("Groceries")
                .transactionDate(LocalDate.of(2026, 5, 15))
                .createdAt(now)
                .updatedAt(now)
                .build();

        TransactionResponse response = transactionMapper.toResponse(entity);

        assertEquals(id, response.getId());
        assertEquals(userId, response.getUserId());
        assertEquals(accountId, response.getAccountId());
        assertEquals(categoryId, response.getCategoryId());
        assertEquals(TransactionType.EXPENSE, response.getType());
        assertEquals(0, new BigDecimal("150.0000").compareTo(response.getAmount()));
        assertEquals("USD", response.getCurrency());
        assertEquals("Groceries", response.getDescription());
        assertEquals(LocalDate.of(2026, 5, 15), response.getTransactionDate());
        assertEquals(now, response.getCreatedAt());
        assertEquals(now, response.getUpdatedAt());
    }
}
