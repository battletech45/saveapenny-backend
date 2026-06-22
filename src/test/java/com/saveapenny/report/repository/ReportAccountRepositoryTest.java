package com.saveapenny.report.repository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.saveapenny.account.entity.Account;
import com.saveapenny.account.entity.AccountType;
import jakarta.persistence.EntityManager;
import java.util.List;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.test.context.TestPropertySource;

@DataJpaTest
@TestPropertySource(properties = {
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.flyway.enabled=false"
})
class ReportAccountRepositoryTest {

    @Autowired
    private ReportAccountRepository reportAccountRepository;

    @Autowired
    private EntityManager entityManager;

    private UUID userId;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();

        Account savings = Account.builder()
                .id(UUID.randomUUID())
                .userId(userId)
                .name("Savings")
                .type(AccountType.SAVINGS)
                .currency("USD")
                .balance(new BigDecimal("10000.0000"))
                .initialBalance(BigDecimal.ZERO)
                .active(true)
                .createdAt(OffsetDateTime.now())
                .updatedAt(OffsetDateTime.now())
                .build();
        entityManager.persist(savings);

        Account credit = Account.builder()
                .id(UUID.randomUUID())
                .userId(userId)
                .name("Credit Card")
                .type(AccountType.CREDIT)
                .currency("USD")
                .balance(new BigDecimal("500.0000"))
                .initialBalance(BigDecimal.ZERO)
                .active(true)
                .createdAt(OffsetDateTime.now())
                .updatedAt(OffsetDateTime.now())
                .build();
        entityManager.persist(credit);

        Account inactive = Account.builder()
                .id(UUID.randomUUID())
                .userId(userId)
                .name("Closed")
                .type(AccountType.BANK)
                .currency("USD")
                .balance(new BigDecimal("0.0000"))
                .initialBalance(BigDecimal.ZERO)
                .active(false)
                .createdAt(OffsetDateTime.now())
                .updatedAt(OffsetDateTime.now())
                .build();
        entityManager.persist(inactive);

        entityManager.flush();
    }

    @Test
    void sumAssetsByUserId_excludesLiabilityType() {
        BigDecimal sum = reportAccountRepository.sumAssetsByUserId(userId, List.of(AccountType.CREDIT));
        assertEquals(0, sum.compareTo(new BigDecimal("10000.0000")));
    }

    @Test
    void sumAssetsByUserId_excludesInactiveAccounts() {
        BigDecimal sum = reportAccountRepository.sumAssetsByUserId(userId, List.of(AccountType.CREDIT));
        assertEquals(0, sum.compareTo(new BigDecimal("10000.0000")));
    }

    @Test
    void sumLiabilitiesByUserId_returnsLiabilityBalance() {
        BigDecimal sum = reportAccountRepository.sumLiabilitiesByUserId(userId, List.of(AccountType.CREDIT));
        assertEquals(0, sum.compareTo(new BigDecimal("500.0000")));
    }
}
