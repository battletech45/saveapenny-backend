package com.saveapenny.automation.service.impl;

import com.saveapenny.account.repository.AccountRepository;
import com.saveapenny.billing.service.BillingAccessService;
import com.saveapenny.automation.dto.CreateRecurringTransactionRequest;
import com.saveapenny.automation.dto.RecurringExecutionHistoryResponse;
import com.saveapenny.automation.dto.RecurringTransactionResponse;
import com.saveapenny.automation.dto.UpcomingRunResponse;
import com.saveapenny.automation.dto.UpdateRecurringTransactionRequest;
import com.saveapenny.automation.entity.RecurringStatus;
import com.saveapenny.automation.entity.RecurringTransaction;
import com.saveapenny.automation.exception.InvalidRecurringTransactionNextRunDateException;
import com.saveapenny.automation.exception.InvalidRecurringTransactionStatusTransitionException;
import com.saveapenny.automation.exception.InvalidRecurringTransactionTypeException;
import com.saveapenny.automation.exception.RecurringTransactionDependencyNotFoundException;
import com.saveapenny.automation.exception.RecurringTransactionNotFoundException;
import com.saveapenny.automation.mapper.RecurringExecutionHistoryMapper;
import com.saveapenny.automation.mapper.RecurringTransactionMapper;
import com.saveapenny.automation.repository.RecurringExecutionHistoryRepository;
import com.saveapenny.automation.repository.RecurringTransactionRepository;
import com.saveapenny.automation.service.RecurringTransactionService;
import com.saveapenny.category.entity.Category;
import com.saveapenny.category.repository.CategoryRepository;
import com.saveapenny.config.TimeService;
import com.saveapenny.transaction.entity.TransactionType;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class RecurringTransactionServiceImpl implements RecurringTransactionService {

    private final RecurringTransactionRepository recurringTransactionRepository;
    private final RecurringTransactionMapper recurringTransactionMapper;
    private final RecurringExecutionHistoryRepository executionHistoryRepository;
    private final RecurringExecutionHistoryMapper executionHistoryMapper;
    private final AccountRepository accountRepository;
    private final CategoryRepository categoryRepository;
    private final TimeService timeService;
    private final BillingAccessService billingAccessService;

    public RecurringTransactionServiceImpl(
            RecurringTransactionRepository recurringTransactionRepository,
            RecurringTransactionMapper recurringTransactionMapper,
            RecurringExecutionHistoryRepository executionHistoryRepository,
            RecurringExecutionHistoryMapper executionHistoryMapper,
            AccountRepository accountRepository,
            CategoryRepository categoryRepository,
            TimeService timeService,
            BillingAccessService billingAccessService) {
        this.recurringTransactionRepository = recurringTransactionRepository;
        this.recurringTransactionMapper = recurringTransactionMapper;
        this.executionHistoryRepository = executionHistoryRepository;
        this.executionHistoryMapper = executionHistoryMapper;
        this.accountRepository = accountRepository;
        this.categoryRepository = categoryRepository;
        this.timeService = timeService;
        this.billingAccessService = billingAccessService;
    }

    @Override
    public RecurringTransactionResponse create(UUID currentUserId, CreateRecurringTransactionRequest request) {
        billingAccessService.requireFeature(currentUserId, "advancedRecurring");
        validateType(request.getType());
        validateNextRunDate(request.getNextRunDate());
        ensureOwnedActiveAccount(currentUserId, request.getAccountId());
        ensureCategoryVisible(currentUserId, request.getCategoryId());

        RecurringTransaction recurringTransaction = recurringTransactionMapper.toEntity(request);
        recurringTransaction.setUserId(currentUserId);

        RecurringTransaction saved = recurringTransactionRepository.save(recurringTransaction);
        return recurringTransactionMapper.toResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<RecurringTransactionResponse> getAll(UUID currentUserId, Pageable pageable) {
        return recurringTransactionRepository.findAllByUserId(currentUserId, pageable)
                .map(recurringTransactionMapper::toResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public RecurringTransactionResponse getById(UUID currentUserId, UUID recurringTransactionId) {
        RecurringTransaction recurringTransaction = findOwnedRecurringTransaction(currentUserId, recurringTransactionId);
        return recurringTransactionMapper.toResponse(recurringTransaction);
    }

    @Override
    public RecurringTransactionResponse update(
            UUID currentUserId,
            UUID recurringTransactionId,
            UpdateRecurringTransactionRequest request) {
        validateType(request.getType());
        validateNextRunDate(request.getNextRunDate());
        ensureOwnedActiveAccount(currentUserId, request.getAccountId());
        ensureCategoryVisible(currentUserId, request.getCategoryId());

        RecurringTransaction recurringTransaction = findOwnedRecurringTransaction(currentUserId, recurringTransactionId);
        if (request.getStatus() != recurringTransaction.getStatus()) {
            throw new InvalidRecurringTransactionStatusTransitionException(
                    recurringTransactionId,
                    recurringTransaction.getStatus(),
                    request.getStatus());
        }
        recurringTransactionMapper.updateEntity(recurringTransaction, request);

        RecurringTransaction saved = recurringTransactionRepository.save(recurringTransaction);
        return recurringTransactionMapper.toResponse(saved);
    }

    @Override
    public void delete(UUID currentUserId, UUID recurringTransactionId) {
        RecurringTransaction recurringTransaction = findOwnedRecurringTransaction(currentUserId, recurringTransactionId);
        recurringTransaction.setStatus(RecurringStatus.EXPIRED);
        recurringTransactionRepository.save(recurringTransaction);
    }

    @Override
    public RecurringTransactionResponse pause(UUID currentUserId, UUID recurringTransactionId) {
        RecurringTransaction recurringTransaction = findOwnedRecurringTransaction(currentUserId, recurringTransactionId);
        if (recurringTransaction.getStatus() != RecurringStatus.ACTIVE) {
            throw new InvalidRecurringTransactionStatusTransitionException(
                    recurringTransactionId, recurringTransaction.getStatus(), RecurringStatus.PAUSED);
        }
        recurringTransaction.setStatus(RecurringStatus.PAUSED);
        RecurringTransaction saved = recurringTransactionRepository.save(recurringTransaction);
        return recurringTransactionMapper.toResponse(saved);
    }

    @Override
    public RecurringTransactionResponse resume(UUID currentUserId, UUID recurringTransactionId) {
        RecurringTransaction recurringTransaction = findOwnedRecurringTransaction(currentUserId, recurringTransactionId);
        if (recurringTransaction.getStatus() != RecurringStatus.PAUSED) {
            throw new InvalidRecurringTransactionStatusTransitionException(
                    recurringTransactionId, recurringTransaction.getStatus(), RecurringStatus.ACTIVE);
        }
        recurringTransaction.setStatus(RecurringStatus.ACTIVE);
        RecurringTransaction saved = recurringTransactionRepository.save(recurringTransaction);
        return recurringTransactionMapper.toResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<RecurringExecutionHistoryResponse> getHistory(
            UUID currentUserId, UUID recurringTransactionId, Pageable pageable) {
        findOwnedRecurringTransaction(currentUserId, recurringTransactionId);
        return executionHistoryRepository
                .findAllByRecurringTransactionIdAndUserIdOrderByExecutedAtDesc(recurringTransactionId, currentUserId, pageable)
                .map(executionHistoryMapper::toResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public List<UpcomingRunResponse> getUpcoming(UUID currentUserId, int limit) {
        List<RecurringTransaction> activeItems = recurringTransactionRepository
                .findAllByUserIdAndStatusAndNextRunDateLessThanEqual(
                        currentUserId,
                        RecurringStatus.ACTIVE,
                        timeService.today().plusMonths(6));

        List<UpcomingRunResponse> result = new ArrayList<>();
        for (RecurringTransaction item : activeItems) {
            LocalDate runDate = item.getNextRunDate();
            for (int i = 0; i < limit && result.size() < limit * 10; i++) {
                if (runDate.isBefore(timeService.today())) {
                    runDate = nextRunDate(runDate, item.getFrequency());
                    continue;
                }
                result.add(UpcomingRunResponse.builder()
                        .recurringTransactionId(item.getId())
                        .name(item.getName())
                        .amount(item.getAmount())
                        .scheduledDate(runDate)
                        .build());
                runDate = nextRunDate(runDate, item.getFrequency());
            }
        }

        result.sort((a, b) -> a.getScheduledDate().compareTo(b.getScheduledDate()));
        return result.stream().limit(limit).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<RecurringTransactionResponse> getDueRecurringTransactions(LocalDate runDate) {
        LocalDate effectiveRunDate = runDate == null ? timeService.today() : runDate;
        return recurringTransactionRepository.findAllByStatusAndNextRunDateLessThanEqual(RecurringStatus.ACTIVE, effectiveRunDate)
                .stream()
                .map(recurringTransactionMapper::toResponse)
                .toList();
    }

    private RecurringTransaction findOwnedRecurringTransaction(UUID currentUserId, UUID recurringTransactionId) {
        return recurringTransactionRepository.findById(recurringTransactionId)
                .filter(rt -> rt.getUserId().equals(currentUserId) && rt.getStatus() != RecurringStatus.EXPIRED)
                .orElseThrow(() -> new RecurringTransactionNotFoundException(recurringTransactionId));
    }

    private void ensureOwnedActiveAccount(UUID currentUserId, UUID accountId) {
        boolean exists = accountRepository.existsByIdAndUserIdAndActiveTrue(accountId, currentUserId);
        if (!exists) {
            throw new RecurringTransactionDependencyNotFoundException("account", accountId);
        }
    }

    private void ensureCategoryVisible(UUID currentUserId, UUID categoryId) {
        Category category = categoryRepository.findById(categoryId)
                .orElseThrow(() -> new RecurringTransactionDependencyNotFoundException("category", categoryId));
        if (category.getUserId() != null && !category.getUserId().equals(currentUserId)) {
            throw new RecurringTransactionDependencyNotFoundException("category", categoryId);
        }
    }

    private void validateType(TransactionType type) {
        if (type == TransactionType.TRANSFER) {
            throw new InvalidRecurringTransactionTypeException(type);
        }
    }

    private void validateNextRunDate(LocalDate nextRunDate) {
        if (nextRunDate == null || nextRunDate.isBefore(timeService.today())) {
            throw new InvalidRecurringTransactionNextRunDateException(nextRunDate);
        }
    }

    private LocalDate nextRunDate(LocalDate current, com.saveapenny.automation.entity.RecurringFrequency frequency) {
        return switch (frequency) {
            case DAILY -> current.plusDays(1);
            case WEEKLY -> current.plusWeeks(1);
            case MONTHLY -> current.plusMonths(1);
            case YEARLY -> current.plusYears(1);
        };
    }
}
