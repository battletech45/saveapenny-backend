package com.saveapenny.transaction.repository;

import com.saveapenny.transaction.entity.Transaction;
import com.saveapenny.transaction.entity.TransactionType;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TransactionRepository extends JpaRepository<Transaction, UUID> {

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
}
