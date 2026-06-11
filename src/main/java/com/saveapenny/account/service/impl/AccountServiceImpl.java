package com.saveapenny.account.service.impl;

import com.saveapenny.account.dto.AccountResponse;
import com.saveapenny.account.dto.CreateAccountRequest;
import com.saveapenny.account.dto.UpdateAccountRequest;
import com.saveapenny.account.entity.Account;
import com.saveapenny.account.exception.AccountMutationNotAllowedException;
import com.saveapenny.account.exception.AccountNameAlreadyExistsException;
import com.saveapenny.account.exception.AccountNotFoundException;
import com.saveapenny.account.mapper.AccountMapper;
import com.saveapenny.account.repository.AccountRepository;
import com.saveapenny.account.service.AccountService;
import com.saveapenny.transaction.repository.TransactionRepository;
import com.saveapenny.transaction.repository.TransferRepository;
import java.math.BigDecimal;
import java.util.Locale;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class AccountServiceImpl implements AccountService {

    private final AccountRepository accountRepository;
    private final AccountMapper accountMapper;
    private final TransactionRepository transactionRepository;
    private final TransferRepository transferRepository;

    public AccountServiceImpl(
            AccountRepository accountRepository,
            AccountMapper accountMapper,
            TransactionRepository transactionRepository,
            TransferRepository transferRepository) {
        this.accountRepository = accountRepository;
        this.accountMapper = accountMapper;
        this.transactionRepository = transactionRepository;
        this.transferRepository = transferRepository;
    }

    @Override
    public AccountResponse create(UUID currentUserId, CreateAccountRequest request) {
        String normalizedName = normalizeName(request.getName());
        if (accountRepository.existsByUserIdAndNameIgnoreCase(currentUserId, normalizedName)) {
            throw new AccountNameAlreadyExistsException(normalizedName);
        }

        Account account = accountMapper.toEntity(request);
        account.setUserId(currentUserId);
        account.setName(normalizedName);
        account.setCurrency(normalizeCurrency(request.getCurrency()));

        Account saved = accountRepository.save(account);
        return accountMapper.toResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<AccountResponse> getAll(UUID currentUserId, Pageable pageable) {
        return accountRepository.findAllByUserIdAndActiveTrue(currentUserId, pageable)
                .map(accountMapper::toResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public AccountResponse getById(UUID currentUserId, UUID accountId) {
        Account account = findOwnedActiveAccount(currentUserId, accountId);
        return accountMapper.toResponse(account);
    }

    @Override
    public AccountResponse update(UUID currentUserId, UUID accountId, UpdateAccountRequest request) {
        Account account = findOwnedActiveAccountForMutation(currentUserId, accountId);

        String normalizedName = normalizeName(request.getName());
        if (accountRepository.existsByUserIdAndNameIgnoreCaseAndIdNot(currentUserId, normalizedName, accountId)) {
            throw new AccountNameAlreadyExistsException(normalizedName);
        }

        String normalizedCurrency = normalizeCurrency(request.getCurrency());
        boolean accountHasBeenUsed = hasAccountBeenUsed(account);
        if (accountHasBeenUsed
                && account.getType() != request.getType()) {
            throw new AccountMutationNotAllowedException(accountId, "type");
        }
        if (accountHasBeenUsed
                && !account.getCurrency().equals(normalizedCurrency)) {
            throw new AccountMutationNotAllowedException(accountId, "currency");
        }

        accountMapper.updateEntity(account, request);
        account.setName(normalizedName);
        account.setCurrency(normalizedCurrency);

        Account saved = accountRepository.save(account);
        return accountMapper.toResponse(saved);
    }

    @Override
    public void delete(UUID currentUserId, UUID accountId) {
        Account account = findOwnedActiveAccountForMutation(currentUserId, accountId);
        account.setActive(false);
        accountRepository.save(account);
    }

    private Account findOwnedActiveAccount(UUID currentUserId, UUID accountId) {
        return accountRepository.findByIdAndUserIdAndActiveTrue(accountId, currentUserId)
                .orElseThrow(() -> new AccountNotFoundException(accountId));
    }

    private Account findOwnedActiveAccountForMutation(UUID currentUserId, UUID accountId) {
        return accountRepository.findByIdAndUserIdAndActiveTrueWithLock(accountId, currentUserId)
                .orElseThrow(() -> new AccountNotFoundException(accountId));
    }

    private String normalizeName(String name) {
        return name == null ? null : name.trim();
    }

    private String normalizeCurrency(String currency) {
        return currency == null ? null : currency.trim().toUpperCase(Locale.ROOT);
    }

    private boolean hasAccountBeenUsed(Account account) {
        return account.getBalance().compareTo(BigDecimal.ZERO) != 0
                || account.getInitialBalance().compareTo(BigDecimal.ZERO) != 0
                || transactionRepository.existsByUserIdAndAccountId(account.getUserId(), account.getId())
                || transferRepository.existsByAccountId(account.getId());
    }
}
