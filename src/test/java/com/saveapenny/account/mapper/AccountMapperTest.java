package com.saveapenny.account.mapper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import com.saveapenny.account.dto.AccountResponse;
import com.saveapenny.account.dto.CreateAccountRequest;
import com.saveapenny.account.dto.UpdateAccountRequest;
import com.saveapenny.account.entity.Account;
import com.saveapenny.account.entity.AccountType;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

class AccountMapperTest {

    private final AccountMapper accountMapper = Mappers.getMapper(AccountMapper.class);

    @Test
    void toEntity_mapsCreateRequest() {
        CreateAccountRequest request = CreateAccountRequest.builder()
                .name("Wallet")
                .type(AccountType.CASH)
                .currency("USD")
                .initialBalance(new BigDecimal("500.0000"))
                .build();

        Account entity = accountMapper.toEntity(request);

        assertNull(entity.getId());
        assertNull(entity.getUserId());
        assertEquals("Wallet", entity.getName());
        assertEquals(AccountType.CASH, entity.getType());
        assertEquals("USD", entity.getCurrency());
        assertEquals(0, new BigDecimal("500.0000").compareTo(entity.getBalance()));
        assertNull(entity.getCreatedAt());
        assertNull(entity.getUpdatedAt());
    }

    @Test
    void toEntity_defaultsBalanceToZero_whenInitialBalanceNull() {
        CreateAccountRequest request = CreateAccountRequest.builder()
                .name("Wallet")
                .type(AccountType.CASH)
                .currency("USD")
                .initialBalance(null)
                .build();

        Account entity = accountMapper.toEntity(request);

        assertEquals(BigDecimal.ZERO, entity.getBalance());
    }

    @Test
    void updateEntity_mapsOnlyNameTypeCurrency() {
        Account account = Account.builder()
                .id(UUID.randomUUID())
                .userId(UUID.randomUUID())
                .name("Old Name")
                .type(AccountType.SAVINGS)
                .currency("TRY")
                .balance(new BigDecimal("1000.0000"))
                .initialBalance(new BigDecimal("500.0000"))
                .active(true)
                .createdAt(OffsetDateTime.now().minusDays(1))
                .updatedAt(OffsetDateTime.now().minusDays(1))
                .build();

        UpdateAccountRequest request = UpdateAccountRequest.builder()
                .name("New Name")
                .type(AccountType.BANK)
                .currency("EUR")
                .build();

        accountMapper.updateEntity(account, request);

        assertEquals("New Name", account.getName());
        assertEquals(AccountType.BANK, account.getType());
        assertEquals("EUR", account.getCurrency());
        assertNotNull(account.getBalance());
        assertNotNull(account.getInitialBalance());
        assertNotNull(account.getActive());
    }

    @Test
    void toResponse_mapsAllFields() {
        UUID id = UUID.randomUUID();
        OffsetDateTime now = OffsetDateTime.now();
        Account entity = Account.builder()
                .id(id)
                .userId(UUID.randomUUID())
                .name("Test")
                .type(AccountType.INVESTMENT)
                .currency("GBP")
                .balance(new BigDecimal("2000.0000"))
                .initialBalance(new BigDecimal("1000.0000"))
                .active(true)
                .createdAt(now)
                .updatedAt(now)
                .build();

        AccountResponse response = accountMapper.toResponse(entity);

        assertEquals(id, response.getId());
        assertEquals("Test", response.getName());
        assertEquals(AccountType.INVESTMENT, response.getType());
        assertEquals("GBP", response.getCurrency());
        assertEquals(0, new BigDecimal("2000.0000").compareTo(response.getBalance()));
        assertEquals(0, new BigDecimal("1000.0000").compareTo(response.getInitialBalance()));
        assertEquals(true, response.getActive());
        assertEquals(now, response.getCreatedAt());
        assertEquals(now, response.getUpdatedAt());
    }
}
