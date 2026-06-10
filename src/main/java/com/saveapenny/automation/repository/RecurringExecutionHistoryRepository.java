package com.saveapenny.automation.repository;

import com.saveapenny.automation.entity.RecurringExecutionHistory;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RecurringExecutionHistoryRepository extends JpaRepository<RecurringExecutionHistory, UUID> {

    Page<RecurringExecutionHistory> findAllByRecurringTransactionIdAndUserIdOrderByExecutedAtDesc(
            UUID recurringTransactionId, UUID userId, Pageable pageable);

    List<RecurringExecutionHistory> findAllByRecurringTransactionIdAndScheduledDate(
            UUID recurringTransactionId, java.time.LocalDate scheduledDate);
}
