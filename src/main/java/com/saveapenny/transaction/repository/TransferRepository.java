package com.saveapenny.transaction.repository;

import com.saveapenny.transaction.entity.Transfer;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface TransferRepository extends JpaRepository<Transfer, UUID> {

    Optional<Transfer> findByTransactionId(UUID transactionId);

    @Query("""
            select (count(t) > 0)
            from Transfer t
            where t.fromAccountId = :accountId or t.toAccountId = :accountId
            """)
    boolean existsByAccountId(@Param("accountId") UUID accountId);
}
