package com.saveapenny.transaction.repository;

import com.saveapenny.transaction.entity.Transfer;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TransferRepository extends JpaRepository<Transfer, UUID> {

    Optional<Transfer> findByTransactionId(UUID transactionId);
}
