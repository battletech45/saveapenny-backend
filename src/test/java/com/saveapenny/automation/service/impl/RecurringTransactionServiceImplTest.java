package com.saveapenny.automation.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.saveapenny.account.repository.AccountRepository;
import com.saveapenny.automation.dto.CreateRecurringTransactionRequest;
import com.saveapenny.automation.dto.RecurringTransactionResponse;
import com.saveapenny.automation.dto.UpdateRecurringTransactionRequest;
import com.saveapenny.automation.entity.RecurringFrequency;
import com.saveapenny.automation.entity.RecurringTransaction;
import com.saveapenny.automation.exception.InvalidRecurringTransactionNextRunDateException;
import com.saveapenny.automation.exception.InvalidRecurringTransactionTypeException;
import com.saveapenny.automation.exception.RecurringTransactionNotFoundException;
import com.saveapenny.automation.mapper.RecurringTransactionMapper;
import com.saveapenny.automation.repository.RecurringTransactionRepository;
import com.saveapenny.category.entity.Category;
import com.saveapenny.category.repository.CategoryRepository;
import com.saveapenny.transaction.entity.TransactionType;
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
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

@ExtendWith(MockitoExtension.class)
class RecurringTransactionServiceImplTest {

    @Mock
    private RecurringTransactionRepository recurringTransactionRepository;
    @Mock
    private RecurringTransactionMapper recurringTransactionMapper;
    @Mock
    private AccountRepository accountRepository;
    @Mock
    private CategoryRepository categoryRepository;

    @InjectMocks
    private RecurringTransactionServiceImpl recurringTransactionService;

    private UUID userId;
    private UUID recurringId;
    private UUID accountId;
    private UUID categoryId;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        recurringId = UUID.randomUUID();
        accountId = UUID.randomUUID();
        categoryId = UUID.randomUUID();
    }

    @Test
    void create_returnsResponse_whenValid() {
        CreateRecurringTransactionRequest request = CreateRecurringTransactionRequest.builder()
                .accountId(accountId)
                .categoryId(categoryId)
                .type(TransactionType.EXPENSE)
                .amount(new BigDecimal("100.0000"))
                .frequency(RecurringFrequency.MONTHLY)
                .nextRunDate(LocalDate.now().plusDays(1))
                .build();

        RecurringTransaction entity = RecurringTransaction.builder().build();
        RecurringTransaction saved = RecurringTransaction.builder().id(recurringId).userId(userId).build();
        RecurringTransactionResponse response = RecurringTransactionResponse.builder().id(recurringId).build();

        when(accountRepository.existsByIdAndUserIdAndActiveTrue(accountId, userId)).thenReturn(true);
        when(categoryRepository.findById(categoryId)).thenReturn(Optional.of(Category.builder().id(categoryId).userId(userId).build()));
        when(recurringTransactionMapper.toEntity(request)).thenReturn(entity);
        when(recurringTransactionRepository.save(entity)).thenReturn(saved);
        when(recurringTransactionMapper.toResponse(saved)).thenReturn(response);

        RecurringTransactionResponse result = recurringTransactionService.create(userId, request);

        assertEquals(recurringId, result.getId());
        assertEquals(userId, entity.getUserId());
    }

    @Test
    void create_throws_whenTypeTransfer() {
        CreateRecurringTransactionRequest request = CreateRecurringTransactionRequest.builder()
                .accountId(accountId)
                .categoryId(categoryId)
                .type(TransactionType.TRANSFER)
                .amount(new BigDecimal("10.0000"))
                .frequency(RecurringFrequency.WEEKLY)
                .nextRunDate(LocalDate.now().plusDays(1))
                .build();

        assertThrows(InvalidRecurringTransactionTypeException.class, () -> recurringTransactionService.create(userId, request));
        verify(recurringTransactionRepository, never()).save(any());
    }

    @Test
    void create_throws_whenNextRunDatePast() {
        CreateRecurringTransactionRequest request = CreateRecurringTransactionRequest.builder()
                .accountId(accountId)
                .categoryId(categoryId)
                .type(TransactionType.EXPENSE)
                .amount(new BigDecimal("10.0000"))
                .frequency(RecurringFrequency.WEEKLY)
                .nextRunDate(LocalDate.now().minusDays(1))
                .build();

        assertThrows(InvalidRecurringTransactionNextRunDateException.class, () -> recurringTransactionService.create(userId, request));
    }

    @Test
    void getById_returnsResponse_whenFound() {
        RecurringTransaction entity = RecurringTransaction.builder().id(recurringId).userId(userId).build();
        RecurringTransactionResponse response = RecurringTransactionResponse.builder().id(recurringId).build();

        when(recurringTransactionRepository.findByIdAndUserIdAndActiveTrue(recurringId, userId)).thenReturn(Optional.of(entity));
        when(recurringTransactionMapper.toResponse(entity)).thenReturn(response);

        RecurringTransactionResponse result = recurringTransactionService.getById(userId, recurringId);

        assertEquals(recurringId, result.getId());
    }

    @Test
    void getById_throws_whenNotFound() {
        when(recurringTransactionRepository.findByIdAndUserIdAndActiveTrue(recurringId, userId)).thenReturn(Optional.empty());
        assertThrows(RecurringTransactionNotFoundException.class, () -> recurringTransactionService.getById(userId, recurringId));
    }

    @Test
    void getAll_returnsPage() {
        RecurringTransaction row = RecurringTransaction.builder().id(recurringId).build();
        when(recurringTransactionRepository.findAllByUserIdAndActiveTrue(userId, PageRequest.of(0, 20)))
                .thenReturn(new PageImpl<>(List.of(row)));
        when(recurringTransactionMapper.toResponse(row)).thenReturn(RecurringTransactionResponse.builder().id(recurringId).build());

        var result = recurringTransactionService.getAll(userId, PageRequest.of(0, 20));
        assertEquals(1, result.getTotalElements());
    }

    @Test
    void delete_setsActiveFalse() {
        RecurringTransaction recurringTransaction = RecurringTransaction.builder().id(recurringId).active(true).build();
        when(recurringTransactionRepository.findByIdAndUserIdAndActiveTrue(recurringId, userId))
                .thenReturn(Optional.of(recurringTransaction));

        recurringTransactionService.delete(userId, recurringId);

        assertEquals(false, recurringTransaction.getActive());
        verify(recurringTransactionRepository).save(recurringTransaction);
    }

    @Test
    void getDueRecurringTransactions_returnsMappedList() {
        LocalDate runDate = LocalDate.now();
        RecurringTransaction due = RecurringTransaction.builder().id(recurringId).build();
        when(recurringTransactionRepository.findAllByActiveTrueAndNextRunDateLessThanEqual(runDate)).thenReturn(List.of(due));
        when(recurringTransactionMapper.toResponse(due)).thenReturn(RecurringTransactionResponse.builder().id(recurringId).build());

        List<RecurringTransactionResponse> result = recurringTransactionService.getDueRecurringTransactions(runDate);

        assertEquals(1, result.size());
        assertEquals(recurringId, result.get(0).getId());
    }

    @Test
    void update_returnsResponse_whenValid() {
        UpdateRecurringTransactionRequest request = UpdateRecurringTransactionRequest.builder()
                .accountId(accountId)
                .categoryId(categoryId)
                .type(TransactionType.INCOME)
                .amount(new BigDecimal("50.0000"))
                .frequency(RecurringFrequency.DAILY)
                .nextRunDate(LocalDate.now().plusDays(1))
                .active(true)
                .build();
        RecurringTransaction recurringTransaction = RecurringTransaction.builder().id(recurringId).userId(userId).build();
        RecurringTransactionResponse response = RecurringTransactionResponse.builder().id(recurringId).build();

        when(accountRepository.existsByIdAndUserIdAndActiveTrue(accountId, userId)).thenReturn(true);
        when(categoryRepository.findById(categoryId)).thenReturn(Optional.of(Category.builder().id(categoryId).userId(userId).build()));
        when(recurringTransactionRepository.findByIdAndUserIdAndActiveTrue(recurringId, userId)).thenReturn(Optional.of(recurringTransaction));
        when(recurringTransactionRepository.save(recurringTransaction)).thenReturn(recurringTransaction);
        when(recurringTransactionMapper.toResponse(recurringTransaction)).thenReturn(response);

        RecurringTransactionResponse result = recurringTransactionService.update(userId, recurringId, request);
        assertEquals(recurringId, result.getId());
        verify(recurringTransactionMapper).updateEntity(recurringTransaction, request);
    }
}
