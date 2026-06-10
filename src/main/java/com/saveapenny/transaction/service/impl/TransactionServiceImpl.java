package com.saveapenny.transaction.service.impl;

import com.saveapenny.account.entity.Account;
import com.saveapenny.account.repository.AccountRepository;
import com.saveapenny.category.entity.Category;
import com.saveapenny.category.repository.CategoryRepository;
import com.saveapenny.transaction.dto.CreateTransactionRequest;
import com.saveapenny.transaction.dto.CreateTransferRequest;
import com.saveapenny.transaction.dto.TransactionResponse;
import com.saveapenny.transaction.dto.TransferResponse;
import com.saveapenny.transaction.dto.UpdateTransactionRequest;
import com.saveapenny.transaction.entity.Transaction;
import com.saveapenny.transaction.entity.TransactionType;
import com.saveapenny.transaction.entity.Transfer;
import com.saveapenny.transaction.exception.InsufficientBalanceException;
import com.saveapenny.transaction.exception.InvalidTransactionCurrencyException;
import com.saveapenny.transaction.exception.InvalidTransferException;
import com.saveapenny.transaction.exception.TransactionNotFoundException;
import com.saveapenny.transaction.mapper.TransactionMapper;
import com.saveapenny.transaction.repository.TransactionRepository;
import com.saveapenny.transaction.repository.TransferRepository;
import com.saveapenny.transaction.service.TransactionService;
import jakarta.persistence.criteria.Predicate;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Locale;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@Transactional
public class TransactionServiceImpl implements TransactionService {

    private final TransactionRepository transactionRepository;
    private final TransferRepository transferRepository;
    private final AccountRepository accountRepository;
    private final CategoryRepository categoryRepository;
    private final TransactionMapper transactionMapper;

    public TransactionServiceImpl(
            TransactionRepository transactionRepository,
            TransferRepository transferRepository,
            AccountRepository accountRepository,
            CategoryRepository categoryRepository,
            TransactionMapper transactionMapper) {
        this.transactionRepository = transactionRepository;
        this.transferRepository = transferRepository;
        this.accountRepository = accountRepository;
        this.categoryRepository = categoryRepository;
        this.transactionMapper = transactionMapper;
    }

    @Override
    public TransactionResponse create(UUID currentUserId, CreateTransactionRequest request) {
        if (request.getType() == TransactionType.TRANSFER) {
            throw new InvalidTransferException("Use transfer endpoint for TRANSFER type transactions.");
        }

        Account account = findOwnedActiveAccount(currentUserId, request.getAccountId());
        ensureCategoryVisible(currentUserId, request.getCategoryId());
        String normalizedCurrency = normalizeCurrency(request.getCurrency());
        ensureTransactionCurrencyMatchesAccount(account, normalizedCurrency);

        BigDecimal amount = request.getAmount();
        applyTransactionImpact(account, request.getType(), amount, true);

        Transaction transaction = transactionMapper.toEntity(request);
        transaction.setUserId(currentUserId);
        transaction.setCurrency(normalizedCurrency);

        Account savedAccount = accountRepository.save(account);
        Transaction savedTransaction = transactionRepository.save(transaction);

        return transactionMapper.toResponse(savedTransaction);
    }

    @Override
    public TransferResponse createTransfer(UUID currentUserId, CreateTransferRequest request) {
        if (request.getFromAccountId().equals(request.getToAccountId())) {
            throw new InvalidTransferException("From and to accounts must be different.");
        }

        Account fromAccount = findOwnedActiveAccount(currentUserId, request.getFromAccountId());
        Account toAccount = findOwnedActiveAccount(currentUserId, request.getToAccountId());
        ensureCategoryVisible(currentUserId, request.getCategoryId());

        String normalizedCurrency = normalizeCurrency(request.getCurrency());
        if (!normalizedCurrency.equals(fromAccount.getCurrency()) || !normalizedCurrency.equals(toAccount.getCurrency())) {
            throw new InvalidTransferException("Transfer currency must match both account currencies.");
        }

        if (fromAccount.getBalance().compareTo(request.getAmount()) < 0) {
            throw new InsufficientBalanceException(fromAccount.getId());
        }

        fromAccount.setBalance(fromAccount.getBalance().subtract(request.getAmount()));
        toAccount.setBalance(toAccount.getBalance().add(request.getAmount()));
        accountRepository.save(fromAccount);
        accountRepository.save(toAccount);

        Transaction transferTransaction = Transaction.builder()
                .userId(currentUserId)
                .accountId(request.getFromAccountId())
                .categoryId(request.getCategoryId())
                .type(TransactionType.TRANSFER)
                .amount(request.getAmount())
                .currency(normalizedCurrency)
                .description(request.getDescription())
                .transactionDate(request.getTransactionDate())
                .build();
        Transaction savedTransaction = transactionRepository.save(transferTransaction);

        Transfer transfer = Transfer.builder()
                .transactionId(savedTransaction.getId())
                .fromAccountId(request.getFromAccountId())
                .toAccountId(request.getToAccountId())
                .amount(request.getAmount())
                .build();
        Transfer savedTransfer = transferRepository.save(transfer);

        return transactionMapper.toTransferResponse(savedTransaction, savedTransfer);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<TransactionResponse> getAll(
            UUID currentUserId,
            LocalDate from,
            LocalDate to,
            TransactionType type,
            UUID accountId,
            UUID categoryId,
            BigDecimal minAmount,
            BigDecimal maxAmount,
            String keyword,
            Pageable pageable) {
        String normalizedKeyword = StringUtils.hasText(keyword) ? keyword.trim() : null;
        String keywordPattern = normalizedKeyword == null ? null : "%" + normalizedKeyword.toLowerCase(Locale.ROOT) + "%";
        Specification<Transaction> specification = (root, query, criteriaBuilder) -> {
            var predicates = new ArrayList<Predicate>();
            predicates.add(criteriaBuilder.equal(root.get("userId"), currentUserId));

            if (from != null) {
                predicates.add(criteriaBuilder.greaterThanOrEqualTo(root.get("transactionDate"), from));
            }
            if (to != null) {
                predicates.add(criteriaBuilder.lessThanOrEqualTo(root.get("transactionDate"), to));
            }
            if (type != null) {
                predicates.add(criteriaBuilder.equal(root.get("type"), type));
            }
            if (accountId != null) {
                predicates.add(criteriaBuilder.equal(root.get("accountId"), accountId));
            }
            if (categoryId != null) {
                predicates.add(criteriaBuilder.equal(root.get("categoryId"), categoryId));
            }
            if (minAmount != null) {
                predicates.add(criteriaBuilder.greaterThanOrEqualTo(root.get("amount"), minAmount));
            }
            if (maxAmount != null) {
                predicates.add(criteriaBuilder.lessThanOrEqualTo(root.get("amount"), maxAmount));
            }
            if (keywordPattern != null) {
                predicates.add(criteriaBuilder.like(
                        criteriaBuilder.lower(criteriaBuilder.coalesce(root.get("description"), "")),
                        keywordPattern));
            }

            return criteriaBuilder.and(predicates.toArray(Predicate[]::new));
        };

        Page<Transaction> page = transactionRepository.findAll(specification, pageable);
        return page.map(transactionMapper::toResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public TransactionResponse getById(UUID currentUserId, UUID transactionId) {
        Transaction transaction = findOwnedTransaction(currentUserId, transactionId);
        return transactionMapper.toResponse(transaction);
    }

    @Override
    public TransactionResponse update(UUID currentUserId, UUID transactionId, UpdateTransactionRequest request) {
        Transaction existing = findOwnedTransaction(currentUserId, transactionId);
        if (existing.getType() == TransactionType.TRANSFER) {
            throw new InvalidTransferException("Transfer transactions cannot be updated directly.");
        }
        if (request.getType() == TransactionType.TRANSFER) {
            throw new InvalidTransferException("Use transfer endpoint for TRANSFER type transactions.");
        }

        Account oldAccount = findOwnedActiveAccount(currentUserId, existing.getAccountId());
        Account newAccount = findOwnedActiveAccount(currentUserId, request.getAccountId());
        ensureCategoryVisible(currentUserId, request.getCategoryId());
        String normalizedCurrency = normalizeCurrency(request.getCurrency());
        ensureTransactionCurrencyMatchesAccount(newAccount, normalizedCurrency);

        applyTransactionImpact(oldAccount, existing.getType(), existing.getAmount(), false);
        accountRepository.save(oldAccount);

        applyTransactionImpact(newAccount, request.getType(), request.getAmount(), true);
        accountRepository.save(newAccount);

        transactionMapper.updateEntity(existing, request);
        existing.setCurrency(normalizedCurrency);
        Transaction saved = transactionRepository.save(existing);
        return transactionMapper.toResponse(saved);
    }

    @Override
    public void delete(UUID currentUserId, UUID transactionId) {
        Transaction transaction = findOwnedTransaction(currentUserId, transactionId);

        if (transaction.getType() == TransactionType.TRANSFER) {
            Transfer transfer = transferRepository.findByTransactionId(transaction.getId())
                    .orElseThrow(() -> new InvalidTransferException("Transfer details not found for transaction."));

            Account fromAccount = findOwnedActiveAccount(currentUserId, transfer.getFromAccountId());
            Account toAccount = findOwnedActiveAccount(currentUserId, transfer.getToAccountId());

            if (toAccount.getBalance().compareTo(transfer.getAmount()) < 0) {
                throw new InsufficientBalanceException(toAccount.getId());
            }

            fromAccount.setBalance(fromAccount.getBalance().add(transfer.getAmount()));
            toAccount.setBalance(toAccount.getBalance().subtract(transfer.getAmount()));
            accountRepository.save(fromAccount);
            accountRepository.save(toAccount);
            transferRepository.delete(transfer);
        } else {
            Account account = findOwnedActiveAccount(currentUserId, transaction.getAccountId());
            applyTransactionImpact(account, transaction.getType(), transaction.getAmount(), false);
            accountRepository.save(account);
        }

        transactionRepository.delete(transaction);
    }

    private Transaction findOwnedTransaction(UUID currentUserId, UUID transactionId) {
        return transactionRepository.findByIdAndUserId(transactionId, currentUserId)
                .orElseThrow(() -> new TransactionNotFoundException(transactionId));
    }

    private Account findOwnedActiveAccount(UUID currentUserId, UUID accountId) {
        return accountRepository.findByIdAndUserIdAndActiveTrueWithLock(accountId, currentUserId)
                .orElseThrow(() -> new InvalidTransferException("Account not found or inactive: " + accountId));
    }

    private void ensureCategoryVisible(UUID currentUserId, UUID categoryId) {
        Category category = categoryRepository.findById(categoryId)
                .orElseThrow(() -> new InvalidTransferException("Category not found: " + categoryId));
        if (category.getUserId() != null && !category.getUserId().equals(currentUserId)) {
            throw new InvalidTransferException("Category is not visible for current user: " + categoryId);
        }
    }

    private String normalizeCurrency(String currency) {
        return currency == null ? null : currency.trim().toUpperCase(Locale.ROOT);
    }

    private void ensureTransactionCurrencyMatchesAccount(Account account, String normalizedCurrency) {
        if (!account.getCurrency().equals(normalizedCurrency)) {
            throw new InvalidTransactionCurrencyException(account.getId(), account.getCurrency(), normalizedCurrency);
        }
    }

    private void applyTransactionImpact(Account account, TransactionType type, BigDecimal amount, boolean apply) {
        BigDecimal factor = apply ? BigDecimal.ONE : BigDecimal.ONE.negate();
        if (type == TransactionType.INCOME) {
            account.setBalance(account.getBalance().add(amount.multiply(factor)));
        } else if (type == TransactionType.EXPENSE) {
            BigDecimal delta = amount.multiply(factor);
            BigDecimal newBalance = account.getBalance().subtract(delta);
            if (apply && newBalance.compareTo(BigDecimal.ZERO) < 0) {
                throw new InsufficientBalanceException(account.getId());
            }
            account.setBalance(newBalance);
        }
    }
}
