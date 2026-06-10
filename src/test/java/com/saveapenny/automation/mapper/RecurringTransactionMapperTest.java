package com.saveapenny.automation.mapper;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.saveapenny.automation.dto.RecurringTransactionResponse;
import com.saveapenny.automation.entity.RecurringFrequency;
import com.saveapenny.automation.entity.RecurringStatus;
import com.saveapenny.automation.entity.RecurringTransaction;
import com.saveapenny.transaction.entity.TransactionType;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

class RecurringTransactionMapperTest {

    private final RecurringTransactionMapper recurringTransactionMapper =
            Mappers.getMapper(RecurringTransactionMapper.class);

    @Test
    void toResponse_mapsAllFields() {
        UUID id = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        OffsetDateTime now = OffsetDateTime.now();

        RecurringTransaction entity = RecurringTransaction.builder()
                .id(id)
                .userId(userId)
                .accountId(UUID.randomUUID())
                .categoryId(UUID.randomUUID())
                .type(TransactionType.EXPENSE)
                .amount(new BigDecimal("250.0000"))
                .frequency(RecurringFrequency.MONTHLY)
                .nextRunDate(LocalDate.of(2026, 7, 1))
                .status(RecurringStatus.ACTIVE)
                .createdAt(now)
                .updatedAt(now)
                .build();

        RecurringTransactionResponse response = recurringTransactionMapper.toResponse(entity);

        assertEquals(id, response.getId());
        assertEquals(userId, response.getUserId());
        assertEquals(entity.getAccountId(), response.getAccountId());
        assertEquals(entity.getCategoryId(), response.getCategoryId());
        assertEquals(TransactionType.EXPENSE, response.getType());
        assertEquals(0, new BigDecimal("250.0000").compareTo(response.getAmount()));
        assertEquals(RecurringFrequency.MONTHLY, response.getFrequency());
        assertEquals(LocalDate.of(2026, 7, 1), response.getNextRunDate());
        assertEquals(RecurringStatus.ACTIVE, response.getStatus());
        assertEquals(now, response.getCreatedAt());
        assertEquals(now, response.getUpdatedAt());
    }
}
