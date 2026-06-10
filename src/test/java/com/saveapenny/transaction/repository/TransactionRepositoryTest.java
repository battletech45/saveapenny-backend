package com.saveapenny.transaction.repository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.saveapenny.transaction.entity.Transaction;
import com.saveapenny.transaction.entity.TransactionType;
import jakarta.persistence.EntityManager;
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
class TransactionRepositoryTest {

    @Autowired
    private TransactionRepository transactionRepository;

    @Autowired
    private EntityManager entityManager;

    private UUID userId;
    private UUID accountId;
    private UUID categoryId;
    private Transaction expense;
    private Transaction income;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        accountId = UUID.randomUUID();
        categoryId = UUID.randomUUID();

        expense = Transaction.builder()
                .id(UUID.randomUUID())
                .userId(userId)
                .accountId(accountId)
                .categoryId(categoryId)
                .type(TransactionType.EXPENSE)
                .amount(new BigDecimal("150.0000"))
                .currency("USD")
                .description("Groceries")
                .transactionDate(LocalDate.of(2026, 5, 15))
                .createdAt(OffsetDateTime.now())
                .updatedAt(OffsetDateTime.now())
                .build();

        income = Transaction.builder()
                .id(UUID.randomUUID())
                .userId(userId)
                .accountId(accountId)
                .categoryId(UUID.randomUUID())
                .type(TransactionType.INCOME)
                .amount(new BigDecimal("3000.0000"))
                .currency("USD")
                .description("Salary")
                .transactionDate(LocalDate.of(2026, 5, 1))
                .createdAt(OffsetDateTime.now())
                .updatedAt(OffsetDateTime.now())
                .build();

        transactionRepository.save(expense);
        transactionRepository.save(income);
        entityManager.flush();
    }

    @Test
    void findByIdAndUserId_returnsTransaction() {
        Optional<Transaction> found = transactionRepository.findByIdAndUserId(expense.getId(), userId);
        assertTrue(found.isPresent());
        assertEquals(expense.getId(), found.get().getId());
    }

    @Test
    void findByIdAndUserId_returnsEmptyForWrongUser() {
        assertTrue(transactionRepository.findByIdAndUserId(expense.getId(), UUID.randomUUID()).isEmpty());
    }

    @Test
    void findAllByUserId_returnsAllTransactions() {
        Page<Transaction> page = transactionRepository.findAllByUserId(userId, PageRequest.of(0, 20));
        assertEquals(2, page.getTotalElements());
    }

    @Test
    void findAllByUserIdAndType_filtersByType() {
        Page<Transaction> page = transactionRepository.findAllByUserIdAndType(
                userId, TransactionType.EXPENSE, PageRequest.of(0, 20));
        assertEquals(1, page.getTotalElements());
        assertEquals(TransactionType.EXPENSE, page.getContent().getFirst().getType());
    }

    @Test
    void findAllByUserIdAndAccountId_filtersByAccount() {
        Page<Transaction> page = transactionRepository.findAllByUserIdAndAccountId(
                userId, accountId, PageRequest.of(0, 20));
        assertEquals(2, page.getTotalElements());
    }

    @Test
    void findAllByUserIdAndCategoryId_filtersByCategory() {
        Page<Transaction> page = transactionRepository.findAllByUserIdAndCategoryId(
                userId, categoryId, PageRequest.of(0, 20));
        assertEquals(1, page.getTotalElements());
    }

    @Test
    void findAllByUserIdAndTransactionDateBetween_filtersByDateRange() {
        Page<Transaction> page = transactionRepository.findAllByUserIdAndTransactionDateBetween(
                userId, LocalDate.of(2026, 5, 10), LocalDate.of(2026, 5, 31), PageRequest.of(0, 20));
        assertEquals(1, page.getTotalElements());
    }

    @Test
    void findAllByUserIdAndTypeAndTransactionDateBetween_returnsList() {
        List<Transaction> result = transactionRepository.findAllByUserIdAndTypeAndTransactionDateBetween(
                userId, TransactionType.INCOME, LocalDate.of(2026, 5, 1), LocalDate.of(2026, 5, 31));
        assertEquals(1, result.size());
    }

    @Test
    void sumAmountByUserIdAndCategoryIdAndTypeAndTransactionDateBetween_returnsSum() {
        BigDecimal sum = transactionRepository
                .sumAmountByUserIdAndCategoryIdAndTypeAndTransactionDateBetween(
                        userId, categoryId, TransactionType.EXPENSE,
                        LocalDate.of(2026, 5, 1), LocalDate.of(2026, 5, 31));
        assertEquals(0, sum.compareTo(new BigDecimal("150.0000")));
    }

    @Test
    void sumAmountByUserIdAndCategoryIdAndTypeAndTransactionDateBetween_returnsZero_whenNoMatch() {
        BigDecimal sum = transactionRepository
                .sumAmountByUserIdAndCategoryIdAndTypeAndTransactionDateBetween(
                        userId, UUID.randomUUID(), TransactionType.EXPENSE,
                        LocalDate.of(2026, 5, 1), LocalDate.of(2026, 5, 31));
        assertEquals(0, sum.compareTo(BigDecimal.ZERO));
    }

    @Test
    void sumDailyAmountByUserIdAndCategoryIdsAndTypeAndTransactionDateBetween_returnsDailyTotals() {
        List<TransactionRepository.CategoryDailyExpenseTotal> totals =
                transactionRepository.sumDailyAmountByUserIdAndCategoryIdsAndTypeAndTransactionDateBetween(
                        userId, List.of(categoryId), TransactionType.EXPENSE,
                        LocalDate.of(2026, 5, 1), LocalDate.of(2026, 5, 31));
        assertEquals(1, totals.size());
        assertEquals(categoryId, totals.getFirst().getCategoryId());
        assertEquals(LocalDate.of(2026, 5, 15), totals.getFirst().getTransactionDate());
        assertEquals(0, new BigDecimal("150.0000").compareTo(totals.getFirst().getTotalAmount()));
    }
}
