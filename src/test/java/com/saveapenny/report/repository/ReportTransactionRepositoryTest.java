package com.saveapenny.report.repository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.saveapenny.category.entity.Category;
import com.saveapenny.category.entity.CategoryType;
import com.saveapenny.transaction.entity.Transaction;
import com.saveapenny.transaction.entity.TransactionType;
import jakarta.persistence.EntityManager;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.TestPropertySource;

@DataJpaTest
@TestPropertySource(properties = {
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.flyway.enabled=false"
})
class ReportTransactionRepositoryTest {

    @Autowired
    private ReportTransactionRepository reportTransactionRepository;

    @Autowired
    private EntityManager entityManager;

    private UUID userId;
    private UUID categoryId;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        categoryId = UUID.randomUUID();

        Category category = Category.builder()
                .id(categoryId)
                .name("Food")
                .type(CategoryType.EXPENSE)
                .userId(userId)
                .createdAt(OffsetDateTime.now())
                .updatedAt(OffsetDateTime.now())
                .build();
        entityManager.persist(category);

        Transaction expense = Transaction.builder()
                .id(UUID.randomUUID())
                .userId(userId)
                .accountId(UUID.randomUUID())
                .categoryId(categoryId)
                .type(TransactionType.EXPENSE)
                .amount(new BigDecimal("200.0000"))
                .currency("USD")
                .description("Restaurant")
                .transactionDate(LocalDate.of(2026, 5, 10))
                .createdAt(OffsetDateTime.now())
                .updatedAt(OffsetDateTime.now())
                .build();
        entityManager.persist(expense);

        Transaction income = Transaction.builder()
                .id(UUID.randomUUID())
                .userId(userId)
                .accountId(UUID.randomUUID())
                .categoryId(categoryId)
                .type(TransactionType.INCOME)
                .amount(new BigDecimal("5000.0000"))
                .currency("USD")
                .description("Salary")
                .transactionDate(LocalDate.of(2026, 5, 1))
                .createdAt(OffsetDateTime.now())
                .updatedAt(OffsetDateTime.now())
                .build();
        entityManager.persist(income);

        entityManager.flush();
    }

    @Test
    void sumAmountByUserIdAndTypeAndTransactionDateBetween_returnsSum() {
        BigDecimal sum = reportTransactionRepository
                .sumAmountByUserIdAndTypeAndTransactionDateBetween(
                        userId, TransactionType.EXPENSE,
                        LocalDate.of(2026, 5, 1), LocalDate.of(2026, 5, 31));
        assertEquals(0, sum.compareTo(new BigDecimal("200.0000")));
    }

    @Test
    void findCategorySpendingByUserIdAndTransactionDateBetween_returnsCategorySpending() {
        List<CategorySpendingView> result =
                reportTransactionRepository.findCategorySpendingByUserIdAndTransactionDateBetween(
                        userId, LocalDate.of(2026, 5, 1), LocalDate.of(2026, 5, 31));
        assertEquals(1, result.size());
        assertEquals("Food", result.getFirst().getCategoryName());
    }

    @Test
    void findCashFlowByUserIdAndTransactionDateBetween_returnsDailyBreakdown() {
        List<CashFlowPointView> result =
                reportTransactionRepository.findCashFlowByUserIdAndTransactionDateBetween(
                        userId, LocalDate.of(2026, 5, 1), LocalDate.of(2026, 5, 31));
        assertEquals(2, result.size());
    }
}
