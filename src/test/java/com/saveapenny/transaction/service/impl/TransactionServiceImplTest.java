package com.saveapenny.transaction.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.inOrder;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.saveapenny.account.entity.Account;
import com.saveapenny.account.repository.AccountRepository;
import com.saveapenny.category.entity.Category;
import com.saveapenny.category.entity.CategoryType;
import com.saveapenny.category.repository.CategoryRepository;
import com.saveapenny.transaction.dto.CreateTransactionRequest;
import com.saveapenny.transaction.dto.CreateTransferRequest;
import com.saveapenny.transaction.dto.TransactionResponse;
import com.saveapenny.transaction.dto.TransferResponse;
import com.saveapenny.transaction.entity.Transaction;
import com.saveapenny.transaction.entity.TransactionType;
import com.saveapenny.transaction.entity.Transfer;
import com.saveapenny.transaction.dto.UpdateTransactionRequest;
import com.saveapenny.transaction.exception.InsufficientBalanceException;
import com.saveapenny.transaction.exception.InvalidTransactionCurrencyException;
import com.saveapenny.transaction.exception.InvalidTransferException;
import com.saveapenny.transaction.exception.TransactionNotFoundException;
import com.saveapenny.transaction.mapper.TransactionMapper;
import com.saveapenny.transaction.repository.TransactionRepository;
import com.saveapenny.transaction.repository.TransferRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
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
import org.springframework.data.jpa.domain.Specification;
import org.mockito.InOrder;

@ExtendWith(MockitoExtension.class)
class TransactionServiceImplTest {

    @Mock
    private TransactionRepository transactionRepository;
    @Mock
    private TransferRepository transferRepository;
    @Mock
    private AccountRepository accountRepository;
    @Mock
    private CategoryRepository categoryRepository;
    @Mock
    private TransactionMapper transactionMapper;

    @InjectMocks
    private TransactionServiceImpl transactionService;

    private UUID userId;
    private UUID accountId;
    private UUID categoryId;
    private Account account;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        accountId = UUID.randomUUID();
        categoryId = UUID.randomUUID();

        account = Account.builder()
                .id(accountId)
                .userId(userId)
                .currency("USD")
                .balance(new BigDecimal("1000.0000"))
                .active(true)
                .build();
    }

    @Test
    void createExpense_updatesBalanceAndSavesTransaction() {
        CreateTransactionRequest request = CreateTransactionRequest.builder()
                .accountId(accountId)
                .categoryId(categoryId)
                .type(TransactionType.EXPENSE)
                .amount(new BigDecimal("100.0000"))
                .currency("usd")
                .description("Groceries")
                .transactionDate(LocalDate.now())
                .build();
        Category category = Category.builder().id(categoryId).userId(userId).type(CategoryType.EXPENSE).build();
        Transaction mapped = Transaction.builder().amount(request.getAmount()).type(TransactionType.EXPENSE).build();
        Transaction saved = Transaction.builder().id(UUID.randomUUID()).userId(userId).build();
        TransactionResponse response = TransactionResponse.builder().id(saved.getId()).build();

        when(accountRepository.findByIdAndUserIdAndActiveTrueWithLock(accountId, userId)).thenReturn(Optional.of(account));
        when(categoryRepository.findByIdForUpdate(categoryId)).thenReturn(Optional.of(category));
        when(transactionMapper.toEntity(request)).thenReturn(mapped);
        when(accountRepository.save(account)).thenReturn(account);
        when(transactionRepository.save(mapped)).thenReturn(saved);
        when(transactionMapper.toResponse(saved)).thenReturn(response);

        TransactionResponse result = transactionService.create(userId, request);

        assertEquals(response.getId(), result.getId());
        assertEquals(new BigDecimal("900.0000"), account.getBalance());
        assertEquals("USD", mapped.getCurrency());
        verify(accountRepository).save(account);
        verify(transactionRepository).save(mapped);
    }

    @Test
    void create_throws_whenTransactionCurrencyDoesNotMatchAccountCurrency() {
        CreateTransactionRequest request = CreateTransactionRequest.builder()
                .accountId(accountId)
                .categoryId(categoryId)
                .type(TransactionType.EXPENSE)
                .amount(new BigDecimal("25.0000"))
                .currency("EUR")
                .transactionDate(LocalDate.now())
                .build();
        Category category = Category.builder().id(categoryId).userId(userId).type(CategoryType.EXPENSE).build();

        when(accountRepository.findByIdAndUserIdAndActiveTrueWithLock(accountId, userId)).thenReturn(Optional.of(account));
        when(categoryRepository.findByIdForUpdate(categoryId)).thenReturn(Optional.of(category));

        assertThrows(InvalidTransactionCurrencyException.class, () -> transactionService.create(userId, request));
    }

    @Test
    void createTransfer_throws_whenSameAccount() {
        CreateTransferRequest request = CreateTransferRequest.builder()
                .fromAccountId(accountId)
                .toAccountId(accountId)
                .categoryId(categoryId)
                .amount(new BigDecimal("50.0000"))
                .currency("USD")
                .transactionDate(LocalDate.now())
                .build();

        assertThrows(InvalidTransferException.class, () -> transactionService.createTransfer(userId, request));
    }

    @Test
    void createTransfer_movesBalances_whenValid() {
        UUID toId = UUID.randomUUID();
        Account toAccount = Account.builder()
                .id(toId)
                .userId(userId)
                .currency("USD")
                .balance(new BigDecimal("200.0000"))
                .active(true)
                .build();

        CreateTransferRequest request = CreateTransferRequest.builder()
                .fromAccountId(accountId)
                .toAccountId(toId)
                .categoryId(categoryId)
                .amount(new BigDecimal("50.0000"))
                .currency("USD")
                .description("Savings transfer")
                .transactionDate(LocalDate.now())
                .build();

        Category category = Category.builder().id(categoryId).userId(userId).type(CategoryType.EXPENSE).build();
        Transaction savedTxn = Transaction.builder().id(UUID.randomUUID()).categoryId(categoryId).build();
        Transfer savedTransfer = Transfer.builder().transactionId(savedTxn.getId()).fromAccountId(accountId).toAccountId(toId)
                .amount(new BigDecimal("50.0000")).build();
        TransferResponse response = TransferResponse.builder().transactionId(savedTxn.getId()).build();

        when(accountRepository.findByIdAndUserIdAndActiveTrueWithLock(accountId, userId)).thenReturn(Optional.of(account));
        when(accountRepository.findByIdAndUserIdAndActiveTrueWithLock(toId, userId)).thenReturn(Optional.of(toAccount));
        when(categoryRepository.findByIdForUpdate(categoryId)).thenReturn(Optional.of(category));
        when(transactionRepository.save(any(Transaction.class))).thenReturn(savedTxn);
        when(transferRepository.save(any(Transfer.class))).thenReturn(savedTransfer);
        when(transactionMapper.toTransferResponse(savedTxn, savedTransfer)).thenReturn(response);

        TransferResponse result = transactionService.createTransfer(userId, request);

        assertEquals(savedTxn.getId(), result.getTransactionId());
        assertEquals(new BigDecimal("950.0000"), account.getBalance());
        assertEquals(new BigDecimal("250.0000"), toAccount.getBalance());
    }

    @Test
    void createExpense_throws_whenInsufficientBalance() {
        account.setBalance(new BigDecimal("10.0000"));
        CreateTransactionRequest request = CreateTransactionRequest.builder()
                .accountId(accountId)
                .categoryId(categoryId)
                .type(TransactionType.EXPENSE)
                .amount(new BigDecimal("50.0000"))
                .currency("USD")
                .transactionDate(LocalDate.now())
                .build();
        Category category = Category.builder().id(categoryId).userId(userId).type(CategoryType.EXPENSE).build();

        when(accountRepository.findByIdAndUserIdAndActiveTrueWithLock(accountId, userId)).thenReturn(Optional.of(account));
        when(categoryRepository.findByIdForUpdate(categoryId)).thenReturn(Optional.of(category));

        assertThrows(InsufficientBalanceException.class, () -> transactionService.create(userId, request));
    }

    @Test
    void getAll_usesCombinedFiltersSearch() {
        LocalDate from = LocalDate.of(2026, 5, 1);
        LocalDate to = LocalDate.of(2026, 5, 31);
        BigDecimal minAmount = new BigDecimal("10.00");
        BigDecimal maxAmount = new BigDecimal("50.00");
        String keyword = " groceries ";
        PageRequest pageable = PageRequest.of(0, 20);

        Transaction transaction = Transaction.builder()
                .id(UUID.randomUUID())
                .userId(userId)
                .accountId(accountId)
                .categoryId(categoryId)
                .type(TransactionType.EXPENSE)
                .amount(new BigDecimal("25.0000"))
                .currency("USD")
                .transactionDate(LocalDate.of(2026, 5, 10))
                .build();
        TransactionResponse response = TransactionResponse.builder().id(transaction.getId()).build();

        when(transactionRepository.findAll(any(Specification.class), eq(pageable)))
                .thenReturn(new PageImpl<>(List.of(transaction), pageable, 1));
        when(transactionMapper.toResponse(transaction)).thenReturn(response);

        var result = transactionService.getAll(
                userId,
                from,
                to,
                TransactionType.EXPENSE,
                accountId,
                categoryId,
                minAmount,
                maxAmount,
                keyword,
                pageable);

        assertEquals(1, result.getTotalElements());
        assertEquals(transaction.getId(), result.getContent().get(0).getId());
        verify(transactionRepository).findAll(any(Specification.class), eq(pageable));
    }

    @Test
    void createIncome_addsToBalance() {
        CreateTransactionRequest request = CreateTransactionRequest.builder()
                .accountId(accountId)
                .categoryId(categoryId)
                .type(TransactionType.INCOME)
                .amount(new BigDecimal("500.0000"))
                .currency("USD")
                .transactionDate(LocalDate.now())
                .build();
        Category category = Category.builder().id(categoryId).userId(userId).type(CategoryType.INCOME).build();
        Transaction mapped = Transaction.builder().amount(request.getAmount()).type(TransactionType.INCOME).build();
        Transaction saved = Transaction.builder().id(UUID.randomUUID()).userId(userId).build();
        TransactionResponse response = TransactionResponse.builder().id(saved.getId()).build();

        when(accountRepository.findByIdAndUserIdAndActiveTrueWithLock(accountId, userId)).thenReturn(Optional.of(account));
        when(categoryRepository.findByIdForUpdate(categoryId)).thenReturn(Optional.of(category));
        when(transactionMapper.toEntity(request)).thenReturn(mapped);
        when(accountRepository.save(account)).thenReturn(account);
        when(transactionRepository.save(mapped)).thenReturn(saved);
        when(transactionMapper.toResponse(saved)).thenReturn(response);

        TransactionResponse result = transactionService.create(userId, request);

        assertEquals(response.getId(), result.getId());
        assertEquals(new BigDecimal("1500.0000"), account.getBalance());
    }

    @Test
    void createTransfer_throws_whenCurrencyMismatch() {
        UUID toId = UUID.randomUUID();
        Account toAccount = Account.builder()
                .id(toId)
                .userId(userId)
                .currency("EUR")
                .balance(new BigDecimal("200.0000"))
                .active(true)
                .build();

        CreateTransferRequest request = CreateTransferRequest.builder()
                .fromAccountId(accountId)
                .toAccountId(toId)
                .categoryId(categoryId)
                .amount(new BigDecimal("50.0000"))
                .currency("USD")
                .transactionDate(LocalDate.now())
                .build();

        when(accountRepository.findByIdAndUserIdAndActiveTrueWithLock(accountId, userId)).thenReturn(Optional.of(account));
        when(accountRepository.findByIdAndUserIdAndActiveTrueWithLock(toId, userId)).thenReturn(Optional.of(toAccount));
        when(categoryRepository.findByIdForUpdate(categoryId)).thenReturn(Optional.of(Category.builder().id(categoryId).userId(userId).build()));

        assertThrows(InvalidTransferException.class, () -> transactionService.createTransfer(userId, request));
    }

    @Test
    void getById_returnsResponse_whenFound() {
        UUID txId = UUID.randomUUID();
        Transaction transaction = Transaction.builder().id(txId).userId(userId).build();
        TransactionResponse response = TransactionResponse.builder().id(txId).build();

        when(transactionRepository.findByIdAndUserId(txId, userId)).thenReturn(Optional.of(transaction));
        when(transactionMapper.toResponse(transaction)).thenReturn(response);

        TransactionResponse result = transactionService.getById(userId, txId);

        assertNotNull(result);
        assertEquals(txId, result.getId());
    }

    @Test
    void getById_throws_whenNotFound() {
        UUID txId = UUID.randomUUID();
        when(transactionRepository.findByIdAndUserId(txId, userId)).thenReturn(Optional.empty());

        assertThrows(TransactionNotFoundException.class, () -> transactionService.getById(userId, txId));
    }

    @Test
    void update_reversesOldAndAppliesNewImpact() {
        UUID txId = UUID.randomUUID();
        Transaction existing = Transaction.builder()
                .id(txId)
                .userId(userId)
                .accountId(accountId)
                .type(TransactionType.EXPENSE)
                .amount(new BigDecimal("100.0000"))
                .currency("USD")
                .build();

        UpdateTransactionRequest request = UpdateTransactionRequest.builder()
                .accountId(accountId)
                .categoryId(categoryId)
                .type(TransactionType.INCOME)
                .amount(new BigDecimal("200.0000"))
                .currency("USD")
                .transactionDate(LocalDate.now())
                .build();

        Category category = Category.builder().id(categoryId).userId(userId).type(CategoryType.INCOME).build();
        TransactionResponse response = TransactionResponse.builder().id(txId).build();

        when(transactionRepository.findByIdAndUserId(txId, userId)).thenReturn(Optional.of(existing));
        when(accountRepository.findByIdAndUserIdAndActiveTrueWithLock(accountId, userId)).thenReturn(Optional.of(account));
        when(categoryRepository.findByIdForUpdate(categoryId)).thenReturn(Optional.of(category));
        when(transactionRepository.save(existing)).thenReturn(existing);
        when(transactionMapper.toResponse(existing)).thenReturn(response);

        TransactionResponse result = transactionService.update(userId, txId, request);

        assertNotNull(result);
        assertEquals(new BigDecimal("1300.0000"), account.getBalance());
        verify(transactionMapper).updateEntity(existing, request);
    }

    @Test
    void update_locksAccountsInDeterministicOrder_whenSwappingAccounts() {
        UUID txId = UUID.randomUUID();
        UUID lowerId = UUID.fromString("00000000-0000-0000-0000-000000000001");
        UUID higherId = UUID.fromString("00000000-0000-0000-0000-000000000002");
        Account lowerAccount = Account.builder().id(lowerId).userId(userId).currency("USD").balance(new BigDecimal("100.0000")).active(true).build();
        Account higherAccount = Account.builder().id(higherId).userId(userId).currency("USD").balance(new BigDecimal("100.0000")).active(true).build();
        Transaction existing = Transaction.builder()
                .id(txId)
                .userId(userId)
                .accountId(higherId)
                .type(TransactionType.EXPENSE)
                .amount(new BigDecimal("10.0000"))
                .currency("USD")
                .build();
        UpdateTransactionRequest request = UpdateTransactionRequest.builder()
                .accountId(lowerId)
                .categoryId(categoryId)
                .type(TransactionType.EXPENSE)
                .amount(new BigDecimal("10.0000"))
                .currency("USD")
                .transactionDate(LocalDate.now())
                .build();
        Category category = Category.builder().id(categoryId).userId(userId).type(CategoryType.EXPENSE).build();

        when(transactionRepository.findByIdAndUserId(txId, userId)).thenReturn(Optional.of(existing));
        when(accountRepository.findByIdAndUserIdAndActiveTrueWithLock(lowerId, userId)).thenReturn(Optional.of(lowerAccount));
        when(accountRepository.findByIdAndUserIdAndActiveTrueWithLock(higherId, userId)).thenReturn(Optional.of(higherAccount));
        when(categoryRepository.findByIdForUpdate(categoryId)).thenReturn(Optional.of(category));
        when(transactionRepository.save(existing)).thenReturn(existing);
        when(transactionMapper.toResponse(existing)).thenReturn(TransactionResponse.builder().id(txId).build());

        transactionService.update(userId, txId, request);

        InOrder inOrder = inOrder(accountRepository);
        inOrder.verify(accountRepository).findByIdAndUserIdAndActiveTrueWithLock(lowerId, userId);
        inOrder.verify(accountRepository).findByIdAndUserIdAndActiveTrueWithLock(higherId, userId);
    }

    @Test
    void delete_reversesExpenseImpact() {
        UUID txId = UUID.randomUUID();
        Transaction transaction = Transaction.builder()
                .id(txId)
                .userId(userId)
                .accountId(accountId)
                .type(TransactionType.EXPENSE)
                .amount(new BigDecimal("100.0000"))
                .build();

        when(transactionRepository.findByIdAndUserId(txId, userId)).thenReturn(Optional.of(transaction));
        when(accountRepository.findByIdAndUserIdAndActiveTrueWithLock(accountId, userId)).thenReturn(Optional.of(account));

        transactionService.delete(userId, txId);

        assertEquals(new BigDecimal("1100.0000"), account.getBalance());
        verify(transactionRepository).delete(transaction);
    }

    @Test
    void delete_reversesTransferImpact() {
        UUID txId = UUID.randomUUID();
        UUID toId = UUID.randomUUID();
        Account toAccount = Account.builder()
                .id(toId)
                .userId(userId)
                .currency("USD")
                .balance(new BigDecimal("200.0000"))
                .active(true)
                .build();

        Transaction transferTxn = Transaction.builder()
                .id(txId)
                .userId(userId)
                .accountId(accountId)
                .type(TransactionType.TRANSFER)
                .amount(new BigDecimal("50.0000"))
                .build();

        Transfer transfer = Transfer.builder()
                .transactionId(txId)
                .fromAccountId(accountId)
                .toAccountId(toId)
                .amount(new BigDecimal("50.0000"))
                .build();

        when(transactionRepository.findByIdAndUserId(txId, userId)).thenReturn(Optional.of(transferTxn));
        when(transferRepository.findByTransactionId(txId)).thenReturn(Optional.of(transfer));
        when(accountRepository.findByIdAndUserIdAndActiveTrueWithLock(accountId, userId)).thenReturn(Optional.of(account));
        when(accountRepository.findByIdAndUserIdAndActiveTrueWithLock(toId, userId)).thenReturn(Optional.of(toAccount));

        transactionService.delete(userId, txId);

        assertEquals(new BigDecimal("1050.0000"), account.getBalance());
        assertEquals(new BigDecimal("150.0000"), toAccount.getBalance());
        verify(transferRepository).delete(transfer);
        verify(transactionRepository).delete(transferTxn);
    }

    @Test
    void getAll_normalizesBlankKeywordToNull() {
        PageRequest pageable = PageRequest.of(0, 20);

        when(transactionRepository.findAll(any(Specification.class), eq(pageable)))
                .thenReturn(Page.empty(pageable));

        var result = transactionService.getAll(
                userId,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                "   ",
                pageable);

        assertEquals(0, result.getTotalElements());
        verify(transactionRepository).findAll(any(Specification.class), eq(pageable));
    }

    @Test
    void update_throws_whenTransactionCurrencyDoesNotMatchAccountCurrency() {
        UUID txId = UUID.randomUUID();
        Transaction existing = Transaction.builder()
                .id(txId)
                .userId(userId)
                .accountId(accountId)
                .type(TransactionType.EXPENSE)
                .amount(new BigDecimal("100.0000"))
                .currency("USD")
                .build();
        UpdateTransactionRequest request = UpdateTransactionRequest.builder()
                .accountId(accountId)
                .categoryId(categoryId)
                .type(TransactionType.EXPENSE)
                .amount(new BigDecimal("80.0000"))
                .currency("EUR")
                .transactionDate(LocalDate.now())
                .build();
        Category category = Category.builder().id(categoryId).userId(userId).type(CategoryType.EXPENSE).build();

        when(transactionRepository.findByIdAndUserId(txId, userId)).thenReturn(Optional.of(existing));
        when(accountRepository.findByIdAndUserIdAndActiveTrueWithLock(accountId, userId)).thenReturn(Optional.of(account));
        when(categoryRepository.findByIdForUpdate(categoryId)).thenReturn(Optional.of(category));

        assertThrows(InvalidTransactionCurrencyException.class, () -> transactionService.update(userId, txId, request));
    }
}
