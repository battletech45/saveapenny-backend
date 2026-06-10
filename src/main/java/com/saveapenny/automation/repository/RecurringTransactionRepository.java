package com.saveapenny.automation.repository;

import com.saveapenny.automation.entity.RecurringStatus;
import com.saveapenny.automation.entity.RecurringTransaction;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RecurringTransactionRepository extends JpaRepository<RecurringTransaction, UUID> {

    Optional<RecurringTransaction> findByIdAndUserIdAndStatus(UUID id, UUID userId, RecurringStatus status);

    Page<RecurringTransaction> findAllByUserIdAndStatus(UUID userId, RecurringStatus status, Pageable pageable);

    List<RecurringTransaction> findAllByStatusAndNextRunDateLessThanEqual(RecurringStatus status, LocalDate runDate);
}
