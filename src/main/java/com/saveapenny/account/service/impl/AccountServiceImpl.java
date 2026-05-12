package com.saveapenny.account.service.impl;

import com.saveapenny.account.dto.AccountResponse;
import com.saveapenny.account.dto.CreateAccountRequest;
import com.saveapenny.account.dto.UpdateAccountRequest;
import com.saveapenny.account.entity.Account;
import com.saveapenny.account.exception.AccountNameAlreadyExistsException;
import com.saveapenny.account.exception.AccountNotFoundException;
import com.saveapenny.account.mapper.AccountMapper;
import com.saveapenny.account.repository.AccountRepository;
import com.saveapenny.account.service.AccountService;
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

    public AccountServiceImpl(AccountRepository accountRepository, AccountMapper accountMapper) {
        this.accountRepository = accountRepository;
        this.accountMapper = accountMapper;
    }

    @Override
    public AccountResponse create(UUID currentUserId, CreateAccountRequest request) {
        String normalizedName = normalizeName(request.getName());
        if (accountRepository.existsByUserIdAndNameIgnoreCaseAndActiveTrue(currentUserId, normalizedName)) {
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
        Account account = findOwnedActiveAccount(currentUserId, accountId);

        String normalizedName = normalizeName(request.getName());
        if (accountRepository.existsByUserIdAndNameIgnoreCaseAndActiveTrueAndIdNot(currentUserId, normalizedName, accountId)) {
            throw new AccountNameAlreadyExistsException(normalizedName);
        }

        accountMapper.updateEntity(account, request);
        account.setName(normalizedName);
        account.setCurrency(normalizeCurrency(request.getCurrency()));

        Account saved = accountRepository.save(account);
        return accountMapper.toResponse(saved);
    }

    @Override
    public void delete(UUID currentUserId, UUID accountId) {
        Account account = findOwnedActiveAccount(currentUserId, accountId);
        account.setActive(false);
        accountRepository.save(account);
    }

    private Account findOwnedActiveAccount(UUID currentUserId, UUID accountId) {
        return accountRepository.findByIdAndUserIdAndActiveTrue(accountId, currentUserId)
                .orElseThrow(() -> new AccountNotFoundException(accountId));
    }

    private String normalizeName(String name) {
        return name == null ? null : name.trim();
    }

    private String normalizeCurrency(String currency) {
        return currency == null ? null : currency.trim().toUpperCase(Locale.ROOT);
    }
}
