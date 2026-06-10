package com.saveapenny.account.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.saveapenny.account.dto.AccountResponse;
import com.saveapenny.account.dto.CreateAccountRequest;
import com.saveapenny.account.dto.UpdateAccountRequest;
import com.saveapenny.account.entity.Account;
import com.saveapenny.account.entity.AccountType;
import com.saveapenny.account.exception.AccountNameAlreadyExistsException;
import com.saveapenny.account.exception.AccountMutationNotAllowedException;
import com.saveapenny.account.exception.AccountNotFoundException;
import com.saveapenny.account.mapper.AccountMapper;
import com.saveapenny.account.repository.AccountRepository;
import com.saveapenny.transaction.repository.TransactionRepository;
import com.saveapenny.transaction.repository.TransferRepository;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

@ExtendWith(MockitoExtension.class)
class AccountServiceImplTest {

    @Mock
    private AccountRepository accountRepository;

    @Mock
    private AccountMapper accountMapper;

    @Mock
    private TransactionRepository transactionRepository;

    @Mock
    private TransferRepository transferRepository;

    @InjectMocks
    private AccountServiceImpl accountService;

    private UUID userId;
    private UUID accountId;
    private Account account;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        accountId = UUID.randomUUID();
        account = Account.builder()
                .id(accountId)
                .userId(userId)
                .name("Wallet")
                .type(AccountType.CASH)
                .currency("USD")
                .balance(new BigDecimal("100.0000"))
                .initialBalance(new BigDecimal("100.0000"))
                .active(true)
                .createdAt(OffsetDateTime.now().minusDays(1))
                .updatedAt(OffsetDateTime.now())
                .build();
    }

    @Test
    void create_returnsResponse_whenValid() {
        CreateAccountRequest request = CreateAccountRequest.builder()
                .name(" Wallet ")
                .type(AccountType.CASH)
                .currency("usd")
                .initialBalance(new BigDecimal("100.0000"))
                .build();
        Account mapped = Account.builder()
                .name(" Wallet ")
                .type(AccountType.CASH)
                .currency("usd")
                .initialBalance(new BigDecimal("100.0000"))
                .balance(new BigDecimal("100.0000"))
                .active(true)
                .build();
        AccountResponse response = AccountResponse.builder().id(accountId).name("Wallet").build();

        when(accountRepository.existsByUserIdAndNameIgnoreCase(userId, "Wallet")).thenReturn(false);
        when(accountMapper.toEntity(request)).thenReturn(mapped);
        when(accountRepository.save(mapped)).thenReturn(account);
        when(accountMapper.toResponse(account)).thenReturn(response);

        AccountResponse result = accountService.create(userId, request);

        assertEquals(accountId, result.getId());
        assertEquals(userId, mapped.getUserId());
        assertEquals("Wallet", mapped.getName());
        assertEquals("USD", mapped.getCurrency());
    }

    @Test
    void create_throws_whenNameExists() {
        CreateAccountRequest request = CreateAccountRequest.builder()
                .name("Wallet")
                .type(AccountType.CASH)
                .currency("USD")
                .initialBalance(BigDecimal.ZERO)
                .build();

        when(accountRepository.existsByUserIdAndNameIgnoreCase(userId, "Wallet")).thenReturn(true);

        assertThrows(AccountNameAlreadyExistsException.class, () -> accountService.create(userId, request));
        verify(accountRepository, never()).save(any(Account.class));
    }

    @Test
    void getAll_returnsPagedResponses() {
        PageRequest pageable = PageRequest.of(0, 10);
        Page<Account> page = new PageImpl<>(java.util.List.of(account), pageable, 1);
        AccountResponse response = AccountResponse.builder().id(accountId).name("Wallet").build();

        when(accountRepository.findAllByUserIdAndActiveTrue(userId, pageable)).thenReturn(page);
        when(accountMapper.toResponse(account)).thenReturn(response);

        Page<AccountResponse> result = accountService.getAll(userId, pageable);

        assertEquals(1, result.getTotalElements());
        assertEquals("Wallet", result.getContent().get(0).getName());
    }

    @Test
    void getById_throws_whenNotFound() {
        when(accountRepository.findByIdAndUserIdAndActiveTrue(accountId, userId)).thenReturn(Optional.empty());

        assertThrows(AccountNotFoundException.class, () -> accountService.getById(userId, accountId));
    }

    @Test
    void update_appliesNameChange_whenValid() {
        UpdateAccountRequest request = UpdateAccountRequest.builder()
                .name(" Main Wallet ")
                .type(AccountType.CASH)
                .currency("usd")
                .build();
        AccountResponse response = AccountResponse.builder().id(accountId).name("Main Wallet").currency("USD").build();

        when(accountRepository.findByIdAndUserIdAndActiveTrue(accountId, userId)).thenReturn(Optional.of(account));
        when(accountRepository.existsByUserIdAndNameIgnoreCaseAndIdNot(userId, "Main Wallet", accountId))
                .thenReturn(false);
        when(accountRepository.save(account)).thenReturn(account);
        when(accountMapper.toResponse(account)).thenReturn(response);

        AccountResponse result = accountService.update(userId, accountId, request);

        assertEquals("Main Wallet", result.getName());
        assertEquals("Main Wallet", account.getName());
        assertEquals("USD", account.getCurrency());
    }

    @Test
    void update_throws_whenChangingTypeAfterAccountHasBalance() {
        UpdateAccountRequest request = UpdateAccountRequest.builder()
                .name("Wallet")
                .type(AccountType.BANK)
                .currency("USD")
                .build();

        when(accountRepository.findByIdAndUserIdAndActiveTrue(accountId, userId)).thenReturn(Optional.of(account));
        when(accountRepository.existsByUserIdAndNameIgnoreCaseAndIdNot(userId, "Wallet", accountId)).thenReturn(false);

        assertThrows(AccountMutationNotAllowedException.class, () -> accountService.update(userId, accountId, request));
    }

    @Test
    void update_throws_whenChangingCurrencyAfterAccountHasBalance() {
        UpdateAccountRequest request = UpdateAccountRequest.builder()
                .name("Wallet")
                .type(AccountType.CASH)
                .currency("EUR")
                .build();

        when(accountRepository.findByIdAndUserIdAndActiveTrue(accountId, userId)).thenReturn(Optional.of(account));
        when(accountRepository.existsByUserIdAndNameIgnoreCaseAndIdNot(userId, "Wallet", accountId)).thenReturn(false);

        assertThrows(AccountMutationNotAllowedException.class, () -> accountService.update(userId, accountId, request));
    }

    @Test
    void create_throws_whenNameExistsOnInactiveAccount() {
        CreateAccountRequest request = CreateAccountRequest.builder()
                .name("Wallet")
                .type(AccountType.CASH)
                .currency("USD")
                .initialBalance(BigDecimal.ZERO)
                .build();

        when(accountRepository.existsByUserIdAndNameIgnoreCase(userId, "Wallet")).thenReturn(true);

        assertThrows(AccountNameAlreadyExistsException.class, () -> accountService.create(userId, request));
    }

    @Test
    void delete_softDeletesAccount() {
        when(accountRepository.findByIdAndUserIdAndActiveTrue(accountId, userId)).thenReturn(Optional.of(account));

        accountService.delete(userId, accountId);

        assertFalse(account.getActive());
        verify(accountRepository).save(account);
    }
}
