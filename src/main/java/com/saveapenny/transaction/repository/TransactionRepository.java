package com.saveapenny.transaction.repository;

import com.saveapenny.transaction.entity.Transaction;
import com.saveapenny.transaction.entity.TransactionType;
import java.time.LocalDate;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface TransactionRepository extends JpaRepository<Transaction, UUID>, JpaSpecificationExecutor<Transaction> {

    Optional<Transaction> findByIdAndUserId(UUID id, UUID userId);

    Page<Transaction> findAllByUserId(UUID userId, Pageable pageable);

    Page<Transaction> findAllByUserIdAndType(UUID userId, TransactionType type, Pageable pageable);

    Page<Transaction> findAllByUserIdAndAccountId(UUID userId, UUID accountId, Pageable pageable);

    Page<Transaction> findAllByUserIdAndCategoryId(UUID userId, UUID categoryId, Pageable pageable);

    Page<Transaction> findAllByUserIdAndTransactionDateBetween(
            UUID userId,
            LocalDate from,
            LocalDate to,
            Pageable pageable);

    boolean existsByUserIdAndAccountId(UUID userId, UUID accountId);

    List<Transaction> findAllByUserIdAndTypeAndTransactionDateBetween(
            UUID userId,
            TransactionType type,
            LocalDate from,
            LocalDate to);

    @Query("""
            select coalesce(sum(t.amount), 0)
            from Transaction t
            where t.userId = :userId
              and t.categoryId = :categoryId
              and t.type = :type
              and t.transactionDate between :from and :to
            """)
    BigDecimal sumAmountByUserIdAndCategoryIdAndTypeAndTransactionDateBetween(
            @Param("userId") UUID userId,
            @Param("categoryId") UUID categoryId,
            @Param("type") TransactionType type,
            @Param("from") LocalDate from,
            @Param("to") LocalDate to);

    @Query("""
            select t.categoryId as categoryId,
                   t.transactionDate as transactionDate,
                   coalesce(sum(t.amount), 0) as totalAmount
            from Transaction t
            where t.userId = :userId
              and t.categoryId in :categoryIds
              and t.type = :type
              and t.transactionDate between :from and :to
            group by t.categoryId, t.transactionDate
            """)
    List<CategoryDailyExpenseTotal> sumDailyAmountByUserIdAndCategoryIdsAndTypeAndTransactionDateBetween(
            @Param("userId") UUID userId,
            @Param("categoryIds") List<UUID> categoryIds,
            @Param("type") TransactionType type,
            @Param("from") LocalDate from,
            @Param("to") LocalDate to);

    interface CategoryDailyExpenseTotal {
        UUID getCategoryId();

        LocalDate getTransactionDate();

        BigDecimal getTotalAmount();
    }
}
