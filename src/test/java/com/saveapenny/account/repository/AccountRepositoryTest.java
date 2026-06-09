package com.saveapenny.account.repository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.saveapenny.account.entity.Account;
import com.saveapenny.account.entity.AccountType;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.TestPropertySource;

@DataJpaTest
@TestPropertySource(properties = {
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.flyway.enabled=false"
})
class AccountRepositoryTest {

    @Autowired
    private AccountRepository accountRepository;

    private UUID userId;
    private Account entity;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        entity = Account.builder()
                .id(UUID.randomUUID())
                .userId(userId)
                .name("Wallet")
                .type(AccountType.CASH)
                .currency("USD")
                .balance(new BigDecimal("100.0000"))
                .initialBalance(new BigDecimal("100.0000"))
                .active(true)
                .createdAt(OffsetDateTime.now())
                .updatedAt(OffsetDateTime.now())
                .build();
        accountRepository.save(entity);
    }

    @Test
    void findAllByUserIdAndActiveTrue_returnsUserAccounts() {
        Page<Account> page = accountRepository.findAllByUserIdAndActiveTrue(userId, PageRequest.of(0, 20));
        assertEquals(1, page.getTotalElements());
        assertEquals(entity.getId(), page.getContent().getFirst().getId());
    }

    @Test
    void findAllByUserIdAndActiveTrue_excludesOtherUsers() {
        Page<Account> page = accountRepository.findAllByUserIdAndActiveTrue(UUID.randomUUID(), PageRequest.of(0, 20));
        assertTrue(page.isEmpty());
    }

    @Test
    void findAllByUserIdAndActiveTrue_excludesInactiveAccounts() {
        entity.setActive(false);
        accountRepository.save(entity);

        Page<Account> page = accountRepository.findAllByUserIdAndActiveTrue(userId, PageRequest.of(0, 20));
        assertTrue(page.isEmpty());
    }

    @Test
    void findByIdAndUserIdAndActiveTrue_returnsAccount() {
        Optional<Account> found = accountRepository.findByIdAndUserIdAndActiveTrue(entity.getId(), userId);
        assertTrue(found.isPresent());
        assertEquals(entity.getId(), found.get().getId());
    }

    @Test
    void findByIdAndUserIdAndActiveTrue_returnsEmptyForWrongUser() {
        Optional<Account> found = accountRepository.findByIdAndUserIdAndActiveTrue(
                entity.getId(), UUID.randomUUID());
        assertTrue(found.isEmpty());
    }

    @Test
    void findByIdAndUserIdAndActiveTrue_returnsEmptyForInactive() {
        entity.setActive(false);
        accountRepository.save(entity);

        Optional<Account> found = accountRepository.findByIdAndUserIdAndActiveTrue(entity.getId(), userId);
        assertTrue(found.isEmpty());
    }

    @Test
    void existsByIdAndUserIdAndActiveTrue_returnsTrueForActive() {
        assertTrue(accountRepository.existsByIdAndUserIdAndActiveTrue(entity.getId(), userId));
    }

    @Test
    void existsByIdAndUserIdAndActiveTrue_returnsFalseForInactive() {
        entity.setActive(false);
        accountRepository.save(entity);

        assertFalse(accountRepository.existsByIdAndUserIdAndActiveTrue(entity.getId(), userId));
    }

    @Test
    void existsByUserIdAndNameIgnoreCaseAndActiveTrue_findsByName() {
        assertTrue(accountRepository.existsByUserIdAndNameIgnoreCaseAndActiveTrue(userId, "wallet"));
    }

    @Test
    void findAllByUserIdAndActiveTrue_listReturnsAll() {
        var list = accountRepository.findAllByUserIdAndActiveTrue(userId);
        assertEquals(1, list.size());
    }
}
