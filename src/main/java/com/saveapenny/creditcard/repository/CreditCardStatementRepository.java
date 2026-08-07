package com.saveapenny.creditcard.repository;

import com.saveapenny.creditcard.entity.CreditCardStatement;
import com.saveapenny.creditcard.entity.StatementStatus;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CreditCardStatementRepository extends JpaRepository<CreditCardStatement, UUID> {

    Page<CreditCardStatement> findAllByAccountIdOrderByStatementDateDesc(UUID accountId, Pageable pageable);

    Optional<CreditCardStatement> findFirstByAccountIdOrderByStatementDateDesc(UUID accountId);

    Optional<CreditCardStatement> findByAccountIdAndStatus(UUID accountId, StatementStatus status);

    List<CreditCardStatement> findAllByStatusAndDueDate(StatementStatus status, LocalDate dueDate);
}
