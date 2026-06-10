package com.saveapenny.transaction.repository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.saveapenny.transaction.entity.Transfer;
import jakarta.persistence.EntityManager;
import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.TestPropertySource;

@DataJpaTest
@TestPropertySource(properties = {
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.flyway.enabled=false"
})
class TransferRepositoryTest {

    @Autowired
    private TransferRepository transferRepository;

    @Autowired
    private EntityManager entityManager;

    private UUID transactionId;
    private Transfer transfer;

    @BeforeEach
    void setUp() {
        transactionId = UUID.randomUUID();
        transfer = Transfer.builder()
                .id(UUID.randomUUID())
                .transactionId(transactionId)
                .fromAccountId(UUID.randomUUID())
                .toAccountId(UUID.randomUUID())
                .amount(new BigDecimal("500.0000"))
                .build();
        transferRepository.save(transfer);
        entityManager.flush();
    }

    @Test
    void findByTransactionId_returnsTransfer() {
        Optional<Transfer> found = transferRepository.findByTransactionId(transactionId);
        assertTrue(found.isPresent());
        assertEquals(transfer.getId(), found.get().getId());
    }

    @Test
    void findByTransactionId_returnsEmpty_whenNotFound() {
        assertTrue(transferRepository.findByTransactionId(UUID.randomUUID()).isEmpty());
    }
}
