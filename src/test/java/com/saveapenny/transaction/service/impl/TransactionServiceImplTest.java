package com.saveapenny.transaction.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
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
import com.saveapenny.transaction.exception.InsufficientBalanceException;
import com.saveapenny.transaction.exception.InvalidTransferException;
import com.saveapenny.transaction.mapper.TransactionMapper;
import com.saveapenny.transaction.repository.TransactionRepository;
import com.saveapenny.transaction.repository.TransferRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

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

        when(accountRepository.findByIdAndUserIdAndActiveTrue(accountId, userId)).thenReturn(Optional.of(account));
        when(categoryRepository.findById(categoryId)).thenReturn(Optional.of(category));
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

        when(accountRepository.findByIdAndUserIdAndActiveTrue(accountId, userId)).thenReturn(Optional.of(account));
        when(accountRepository.findByIdAndUserIdAndActiveTrue(toId, userId)).thenReturn(Optional.of(toAccount));
        when(categoryRepository.findById(categoryId)).thenReturn(Optional.of(category));
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

        when(accountRepository.findByIdAndUserIdAndActiveTrue(accountId, userId)).thenReturn(Optional.of(account));
        when(categoryRepository.findById(categoryId)).thenReturn(Optional.of(category));

        assertThrows(InsufficientBalanceException.class, () -> transactionService.create(userId, request));
    }
}
