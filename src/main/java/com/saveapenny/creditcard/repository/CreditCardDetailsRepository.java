package com.saveapenny.creditcard.repository;

import com.saveapenny.creditcard.entity.CreditCardDetails;
import jakarta.persistence.LockModeType;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CreditCardDetailsRepository extends JpaRepository<CreditCardDetails, UUID> {

    Optional<CreditCardDetails> findByAccountId(UUID accountId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT d FROM CreditCardDetails d WHERE d.accountId = :accountId")
    Optional<CreditCardDetails> findByAccountIdWithLock(@Param("accountId") UUID accountId);

    List<CreditCardDetails> findAllByNextStatementDate(LocalDate nextStatementDate);
}
