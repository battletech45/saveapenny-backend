package com.saveapenny.report.repository;

import com.saveapenny.transaction.entity.Transaction;
import com.saveapenny.transaction.entity.TransactionType;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ReportTransactionRepository extends JpaRepository<Transaction, UUID> {

    @Query("""
            select coalesce(sum(t.amount), 0)
            from Transaction t
            where t.userId = :userId
              and t.type = :type
              and t.transactionDate between :from and :to
            """)
    BigDecimal sumAmountByUserIdAndTypeAndTransactionDateBetween(
            @Param("userId") UUID userId,
            @Param("type") TransactionType type,
            @Param("from") LocalDate from,
            @Param("to") LocalDate to);

    @Query("""
            select c.id as categoryId,
                   c.name as categoryName,
                   coalesce(sum(t.amount), 0) as totalAmount
            from Transaction t
            join Category c on c.id = t.categoryId
            where t.userId = :userId
              and t.type = com.saveapenny.transaction.entity.TransactionType.EXPENSE
              and t.transactionDate between :from and :to
            group by c.id, c.name
            order by totalAmount desc
            """)
    List<CategorySpendingView> findCategorySpendingByUserIdAndTransactionDateBetween(
            @Param("userId") UUID userId,
            @Param("from") LocalDate from,
            @Param("to") LocalDate to);

    @Query("""
            select t.transactionDate as date,
                   coalesce(sum(case when t.type = com.saveapenny.transaction.entity.TransactionType.INCOME then t.amount else 0 end), 0) as incomeAmount,
                   coalesce(sum(case when t.type = com.saveapenny.transaction.entity.TransactionType.EXPENSE then t.amount else 0 end), 0) as expenseAmount,
                   coalesce(sum(case when t.type = com.saveapenny.transaction.entity.TransactionType.INCOME then t.amount else -t.amount end), 0) as netAmount
            from Transaction t
            where t.userId = :userId
              and t.type in (
                com.saveapenny.transaction.entity.TransactionType.INCOME,
                com.saveapenny.transaction.entity.TransactionType.EXPENSE
              )
              and t.transactionDate between :from and :to
            group by t.transactionDate
            order by t.transactionDate asc
            """)
    List<CashFlowPointView> findCashFlowByUserIdAndTransactionDateBetween(
            @Param("userId") UUID userId,
            @Param("from") LocalDate from,
            @Param("to") LocalDate to);
}
