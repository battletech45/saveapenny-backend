package com.saveapenny.automation.repository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.saveapenny.automation.entity.RecurringFrequency;
import com.saveapenny.automation.entity.RecurringTransaction;
import com.saveapenny.transaction.entity.TransactionType;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.TestPropertySource;

@DataJpaTest
@TestPropertySource(properties = {
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.flyway.enabled=false"
})
class RecurringTransactionRepositoryTest {

    @Autowired
    private RecurringTransactionRepository recurringTransactionRepository;

    private UUID userId;
    private RecurringTransaction activeDue;
    private RecurringTransaction activeNotDue;
    private RecurringTransaction inactiveDue;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();

        activeDue = RecurringTransaction.builder()
                .id(UUID.randomUUID())
                .userId(userId)
                .accountId(UUID.randomUUID())
                .categoryId(UUID.randomUUID())
                .type(TransactionType.EXPENSE)
                .amount(new BigDecimal("50.0000"))
                .frequency(RecurringFrequency.MONTHLY)
                .nextRunDate(LocalDate.now())
                .active(true)
                .createdAt(OffsetDateTime.now())
                .updatedAt(OffsetDateTime.now())
                .build();

        activeNotDue = RecurringTransaction.builder()
                .id(UUID.randomUUID())
                .userId(userId)
                .accountId(UUID.randomUUID())
                .categoryId(UUID.randomUUID())
                .type(TransactionType.EXPENSE)
                .amount(new BigDecimal("100.0000"))
                .frequency(RecurringFrequency.MONTHLY)
                .nextRunDate(LocalDate.now().plusDays(5))
                .active(true)
                .createdAt(OffsetDateTime.now())
                .updatedAt(OffsetDateTime.now())
                .build();

        inactiveDue = RecurringTransaction.builder()
                .id(UUID.randomUUID())
                .userId(userId)
                .accountId(UUID.randomUUID())
                .categoryId(UUID.randomUUID())
                .type(TransactionType.INCOME)
                .amount(new BigDecimal("2000.0000"))
                .frequency(RecurringFrequency.MONTHLY)
                .nextRunDate(LocalDate.now().minusDays(1))
                .active(false)
                .createdAt(OffsetDateTime.now())
                .updatedAt(OffsetDateTime.now())
                .build();

        recurringTransactionRepository.save(activeDue);
        recurringTransactionRepository.save(activeNotDue);
        recurringTransactionRepository.save(inactiveDue);
    }

    @Test
    void findByIdAndUserIdAndActiveTrue_returnsActive() {
        Optional<RecurringTransaction> found = recurringTransactionRepository.findByIdAndUserIdAndActiveTrue(
                activeDue.getId(), userId);
        assertTrue(found.isPresent());
        assertEquals(activeDue.getId(), found.get().getId());
    }

    @Test
    void findByIdAndUserIdAndActiveTrue_returnsEmptyForWrongUser() {
        Optional<RecurringTransaction> found = recurringTransactionRepository.findByIdAndUserIdAndActiveTrue(
                activeDue.getId(), UUID.randomUUID());
        assertTrue(found.isEmpty());
    }

    @Test
    void findByIdAndUserIdAndActiveTrue_returnsEmptyForInactive() {
        Optional<RecurringTransaction> found = recurringTransactionRepository.findByIdAndUserIdAndActiveTrue(
                inactiveDue.getId(), userId);
        assertTrue(found.isEmpty());
    }

    @Test
    void findAllByUserIdAndActiveTrue_returnsActiveOnly() {
        Page<RecurringTransaction> page = recurringTransactionRepository.findAllByUserIdAndActiveTrue(
                userId, PageRequest.of(0, 20));
        assertEquals(2, page.getTotalElements());
    }

    @Test
    void findAllByUserIdAndActiveTrue_excludesInactive() {
        Page<RecurringTransaction> page = recurringTransactionRepository.findAllByUserIdAndActiveTrue(
                UUID.randomUUID(), PageRequest.of(0, 20));
        assertTrue(page.isEmpty());
    }

    @Test
    void findAllByActiveTrueAndNextRunDateLessThanEqual_returnsDueTransactions() {
        List<RecurringTransaction> due = recurringTransactionRepository.findAllByActiveTrueAndNextRunDateLessThanEqual(
                LocalDate.now());
        assertEquals(1, due.size());
        assertEquals(activeDue.getId(), due.getFirst().getId());
    }

    @Test
    void findAllByActiveTrueAndNextRunDateLessThanEqual_excludesFutureAndInactive() {
        List<RecurringTransaction> due = recurringTransactionRepository.findAllByActiveTrueAndNextRunDateLessThanEqual(
                LocalDate.now().minusDays(1));
        assertTrue(due.isEmpty());
    }
}
