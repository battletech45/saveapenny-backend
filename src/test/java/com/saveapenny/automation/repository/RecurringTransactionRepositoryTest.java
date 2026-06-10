package com.saveapenny.automation.repository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.saveapenny.automation.entity.RecurringFrequency;
import com.saveapenny.automation.entity.RecurringStatus;
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
                .status(RecurringStatus.ACTIVE)
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
                .status(RecurringStatus.ACTIVE)
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
                .status(RecurringStatus.EXPIRED)
                .createdAt(OffsetDateTime.now())
                .updatedAt(OffsetDateTime.now())
                .build();

        recurringTransactionRepository.save(activeDue);
        recurringTransactionRepository.save(activeNotDue);
        recurringTransactionRepository.save(inactiveDue);
    }

    @Test
    void findByIdAndUserIdAndStatus_returnsActive() {
        Optional<RecurringTransaction> found = recurringTransactionRepository.findByIdAndUserIdAndStatus(
                activeDue.getId(), userId, RecurringStatus.ACTIVE);
        assertTrue(found.isPresent());
        assertEquals(activeDue.getId(), found.get().getId());
    }

    @Test
    void findByIdAndUserIdAndStatus_returnsEmptyForWrongUser() {
        Optional<RecurringTransaction> found = recurringTransactionRepository.findByIdAndUserIdAndStatus(
                activeDue.getId(), UUID.randomUUID(), RecurringStatus.ACTIVE);
        assertTrue(found.isEmpty());
    }

    @Test
    void findByIdAndUserIdAndStatus_returnsEmptyForExpired() {
        Optional<RecurringTransaction> found = recurringTransactionRepository.findByIdAndUserIdAndStatus(
                inactiveDue.getId(), userId, RecurringStatus.ACTIVE);
        assertTrue(found.isEmpty());
    }

    @Test
    void findAllByUserIdAndStatus_returnsActiveOnly() {
        Page<RecurringTransaction> page = recurringTransactionRepository.findAllByUserIdAndStatus(
                userId, RecurringStatus.ACTIVE, PageRequest.of(0, 20));
        assertEquals(2, page.getTotalElements());
    }

    @Test
    void findAllByUserIdAndStatus_excludesExpired() {
        Page<RecurringTransaction> page = recurringTransactionRepository.findAllByUserIdAndStatus(
                UUID.randomUUID(), RecurringStatus.ACTIVE, PageRequest.of(0, 20));
        assertTrue(page.isEmpty());
    }

    @Test
    void findAllByStatusAndNextRunDateLessThanEqual_returnsDueTransactions() {
        List<RecurringTransaction> due = recurringTransactionRepository.findAllByStatusAndNextRunDateLessThanEqual(
                RecurringStatus.ACTIVE, LocalDate.now());
        assertEquals(1, due.size());
        assertEquals(activeDue.getId(), due.getFirst().getId());
    }

    @Test
    void findAllByStatusAndNextRunDateLessThanEqual_excludesFutureAndInactive() {
        List<RecurringTransaction> due = recurringTransactionRepository.findAllByStatusAndNextRunDateLessThanEqual(
                RecurringStatus.ACTIVE, LocalDate.now().minusDays(1));
        assertTrue(due.isEmpty());
    }
}
