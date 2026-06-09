package com.saveapenny.budget.repository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.saveapenny.budget.entity.Budget;
import com.saveapenny.budget.entity.BudgetPeriod;
import jakarta.persistence.EntityManager;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.Set;
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
class BudgetRepositoryTest {

    @Autowired
    private BudgetRepository budgetRepository;

    @Autowired
    private EntityManager entityManager;

    private UUID userId;
    private UUID categoryId;
    private Budget entity;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        categoryId = UUID.randomUUID();
        entity = Budget.builder()
                .id(UUID.randomUUID())
                .userId(userId)
                .categoryId(categoryId)
                .amount(new BigDecimal("400.0000"))
                .period(BudgetPeriod.MONTHLY)
                .startDate(LocalDate.of(2026, 6, 1))
                .endDate(LocalDate.of(2026, 6, 30))
                .createdAt(OffsetDateTime.now())
                .updatedAt(OffsetDateTime.now())
                .build();
        budgetRepository.save(entity);
    }

    @Test
    void findByIdAndUserId_returnsBudget() {
        Optional<Budget> found = budgetRepository.findByIdAndUserId(entity.getId(), userId);
        assertTrue(found.isPresent());
        assertEquals(entity.getId(), found.get().getId());
    }

    @Test
    void findByIdAndUserId_returnsEmptyForWrongUser() {
        Optional<Budget> found = budgetRepository.findByIdAndUserId(entity.getId(), UUID.randomUUID());
        assertTrue(found.isEmpty());
    }

    @Test
    void findAllByUserId_returnsUserBudgets() {
        Page<Budget> page = budgetRepository.findAllByUserId(userId, PageRequest.of(0, 20));
        assertEquals(1, page.getTotalElements());
        assertEquals(entity.getId(), page.getContent().getFirst().getId());
    }

    @Test
    void findAllByUserId_returnsEmptyForOtherUser() {
        Page<Budget> page = budgetRepository.findAllByUserId(UUID.randomUUID(), PageRequest.of(0, 20));
        assertTrue(page.isEmpty());
    }

    @Test
    void findAllByUserIdAndPeriod_filtersByPeriod() {
        Page<Budget> page = budgetRepository.findAllByUserIdAndPeriod(userId, BudgetPeriod.MONTHLY, PageRequest.of(0, 20));
        assertEquals(1, page.getTotalElements());
    }

    @Test
    void findAllByUserIdAndPeriod_filtersExcludesWrongPeriod() {
        Page<Budget> page = budgetRepository.findAllByUserIdAndPeriod(userId, BudgetPeriod.YEARLY, PageRequest.of(0, 20));
        assertTrue(page.isEmpty());
    }

    @Test
    void existsByUserIdAndCategoryIdAndPeriodAndStartDateAndEndDate_returnsTrueWhenExists() {
        boolean exists = budgetRepository.existsByUserIdAndCategoryIdAndPeriodAndStartDateAndEndDate(
                userId, categoryId, BudgetPeriod.MONTHLY, LocalDate.of(2026, 6, 1), LocalDate.of(2026, 6, 30));
        assertTrue(exists);
    }

    @Test
    void existsByUserIdAndCategoryIdAndPeriodAndStartDateAndEndDate_returnsFalseWhenNotExists() {
        boolean exists = budgetRepository.existsByUserIdAndCategoryIdAndPeriodAndStartDateAndEndDate(
                userId, categoryId, BudgetPeriod.YEARLY, LocalDate.of(2026, 6, 1), LocalDate.of(2026, 12, 31));
        assertFalse(exists);
    }

    @Test
    void existsByUserIdAndCategoryIdAndPeriodAndStartDateAndEndDateAndIdNot_excludesOwnId() {
        boolean exists = budgetRepository.existsByUserIdAndCategoryIdAndPeriodAndStartDateAndEndDateAndIdNot(
                userId, categoryId, BudgetPeriod.MONTHLY, LocalDate.of(2026, 6, 1), LocalDate.of(2026, 6, 30), entity.getId());
        assertFalse(exists);
    }

    @Test
    void deleteAllByIdAndUserId_deletesMatchingBudgets() {
        Budget second = Budget.builder()
                .id(UUID.randomUUID())
                .userId(userId)
                .categoryId(UUID.randomUUID())
                .amount(new BigDecimal("200.0000"))
                .period(BudgetPeriod.MONTHLY)
                .startDate(LocalDate.of(2026, 7, 1))
                .endDate(LocalDate.of(2026, 7, 31))
                .createdAt(OffsetDateTime.now())
                .updatedAt(OffsetDateTime.now())
                .build();
        budgetRepository.save(second);

        int deleted = budgetRepository.deleteAllByIdAndUserId(Set.of(entity.getId(), second.getId()), userId);
        assertEquals(2, deleted);
        entityManager.clear();
        assertTrue(budgetRepository.findById(entity.getId()).isEmpty());
        assertTrue(budgetRepository.findById(second.getId()).isEmpty());
    }

    @Test
    void deleteAllByIdAndUserId_ignoresNonOwnedIds() {
        int deleted = budgetRepository.deleteAllByIdAndUserId(Set.of(entity.getId(), UUID.randomUUID()), userId);
        assertEquals(1, deleted);
    }
}
