package com.saveapenny.creditcard.service.impl;

import com.saveapenny.account.entity.Account;
import com.saveapenny.account.entity.AccountType;
import com.saveapenny.account.exception.AccountNotFoundException;
import com.saveapenny.account.repository.AccountRepository;
import com.saveapenny.config.TimeService;
import com.saveapenny.creditcard.dto.CreditCardDetailsRequest;
import com.saveapenny.creditcard.dto.CreditCardPaymentRequest;
import com.saveapenny.creditcard.dto.CreditCardPaymentResponse;
import com.saveapenny.creditcard.dto.CreditCardStatementResponse;
import com.saveapenny.creditcard.dto.CreditCardSummaryResponse;
import com.saveapenny.creditcard.dto.PaymentType;
import com.saveapenny.creditcard.entity.CreditCardDetails;
import com.saveapenny.creditcard.entity.CreditCardStatement;
import com.saveapenny.creditcard.entity.StatementStatus;
import com.saveapenny.creditcard.exception.CreditCardDetailsNotFoundException;
import com.saveapenny.creditcard.exception.InvalidCreditCardDetailsException;
import com.saveapenny.creditcard.exception.InvalidCreditCardPaymentException;
import com.saveapenny.creditcard.repository.CreditCardDetailsRepository;
import com.saveapenny.creditcard.repository.CreditCardStatementRepository;
import com.saveapenny.creditcard.support.CreditCardCategories;
import com.saveapenny.transaction.entity.Transaction;
import com.saveapenny.transaction.entity.TransactionType;
import com.saveapenny.transaction.entity.Transfer;
import com.saveapenny.transaction.exception.InsufficientBalanceException;
import com.saveapenny.transaction.exception.InvalidTransactionCurrencyException;
import com.saveapenny.transaction.repository.TransactionRepository;
import com.saveapenny.transaction.repository.TransferRepository;
import com.saveapenny.creditcard.service.CreditCardService;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class CreditCardServiceImpl implements CreditCardService {

    private final CreditCardDetailsRepository creditCardDetailsRepository;
    private final CreditCardStatementRepository creditCardStatementRepository;
    private final AccountRepository accountRepository;
    private final TransactionRepository transactionRepository;
    private final TransferRepository transferRepository;
    private final TimeService timeService;

    public CreditCardServiceImpl(
            CreditCardDetailsRepository creditCardDetailsRepository,
            CreditCardStatementRepository creditCardStatementRepository,
            AccountRepository accountRepository,
            TransactionRepository transactionRepository,
            TransferRepository transferRepository,
            TimeService timeService) {
        this.creditCardDetailsRepository = creditCardDetailsRepository;
        this.creditCardStatementRepository = creditCardStatementRepository;
        this.accountRepository = accountRepository;
        this.transactionRepository = transactionRepository;
        this.transferRepository = transferRepository;
        this.timeService = timeService;
    }

    @Override
    public void createDetails(Account account, CreditCardDetailsRequest request) {
        if (request == null || request.getCreditLimit() == null || request.getApr() == null
                || request.getStatementDay() == null) {
            throw new InvalidCreditCardDetailsException(
                    "creditLimit, apr and statementDay are required for CREDIT accounts.");
        }
        if (account.getBalance().compareTo(request.getCreditLimit()) > 0) {
            throw new InvalidCreditCardDetailsException("Initial balance cannot exceed the credit limit.");
        }

        LocalDate today = timeService.today();
        CreditCardDetails details = CreditCardDetails.builder()
                .accountId(account.getId())
                .creditLimit(request.getCreditLimit())
                .apr(request.getApr())
                .statementDay(request.getStatementDay())
                .nextStatementDate(nextOccurrence(today, request.getStatementDay()))
                .build();
        creditCardDetailsRepository.save(details);
    }

    @Override
    public CreditCardSummaryResponse updateDetails(UUID currentUserId, UUID accountId, CreditCardDetailsRequest request) {
        Account account = findOwnedCreditAccount(currentUserId, accountId);
        CreditCardDetails details = findDetails(accountId);

        if (account.getBalance().compareTo(request.getCreditLimit()) > 0) {
            throw new InvalidCreditCardDetailsException("Credit limit cannot be lower than the current balance.");
        }

        details.setCreditLimit(request.getCreditLimit());
        details.setApr(request.getApr());
        if (!request.getStatementDay().equals(details.getStatementDay())) {
            details.setStatementDay(request.getStatementDay());
            details.setNextStatementDate(nextOccurrence(timeService.today(), request.getStatementDay()));
        }
        creditCardDetailsRepository.save(details);

        return buildSummary(account, details);
    }

    @Override
    @Transactional(readOnly = true)
    public CreditCardSummaryResponse getSummary(Account account) {
        CreditCardDetails details = findDetails(account.getId());
        return buildSummary(account, details);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<CreditCardStatementResponse> listStatements(UUID currentUserId, UUID accountId, Pageable pageable) {
        findOwnedCreditAccount(currentUserId, accountId);
        return creditCardStatementRepository.findAllByAccountIdOrderByStatementDateDesc(accountId, pageable)
                .map(this::toStatementResponse);
    }

    @Override
    public CreditCardPaymentResponse makePayment(UUID currentUserId, UUID accountId, CreditCardPaymentRequest request) {
        Account creditAccount = accountRepository.findByIdAndUserIdAndActiveTrueWithLock(accountId, currentUserId)
                .orElseThrow(() -> new AccountNotFoundException(accountId));
        if (creditAccount.getType() != AccountType.CREDIT) {
            throw new InvalidCreditCardPaymentException("Account is not a credit card: " + accountId);
        }
        if (request.getSourceAccountId().equals(accountId)) {
            throw new InvalidCreditCardPaymentException("Source account must be different from the credit card account.");
        }

        Account sourceAccount = accountRepository
                .findByIdAndUserIdAndActiveTrueWithLock(request.getSourceAccountId(), currentUserId)
                .orElseThrow(() -> new AccountNotFoundException(request.getSourceAccountId()));
        if (sourceAccount.getType() == AccountType.CREDIT) {
            throw new InvalidCreditCardPaymentException("Cannot pay a credit card from another credit card.");
        }
        if (!sourceAccount.getCurrency().equals(creditAccount.getCurrency())) {
            throw new InvalidTransactionCurrencyException(
                    sourceAccount.getId(), sourceAccount.getCurrency(), creditAccount.getCurrency());
        }
        if (creditAccount.getBalance().compareTo(BigDecimal.ZERO) <= 0) {
            throw new InvalidCreditCardPaymentException("There is no outstanding balance to pay.");
        }

        var openStatement = creditCardStatementRepository.findByAccountIdAndStatus(accountId, StatementStatus.OPEN);
        BigDecimal amount = resolvePaymentAmount(request, creditAccount, openStatement.orElse(null));

        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new InvalidCreditCardPaymentException("Payment amount must be positive.");
        }
        if (amount.compareTo(creditAccount.getBalance()) > 0) {
            throw new InvalidCreditCardPaymentException("Payment amount exceeds the outstanding balance.");
        }
        if (sourceAccount.getBalance().compareTo(amount) < 0) {
            throw new InsufficientBalanceException(sourceAccount.getId());
        }

        sourceAccount.setBalance(sourceAccount.getBalance().subtract(amount));
        creditAccount.setBalance(creditAccount.getBalance().subtract(amount));
        accountRepository.save(sourceAccount);
        accountRepository.save(creditAccount);

        Transaction transaction = Transaction.builder()
                .userId(currentUserId)
                .accountId(sourceAccount.getId())
                .categoryId(CreditCardCategories.CREDIT_CARD_PAYMENT)
                .type(TransactionType.TRANSFER)
                .amount(amount)
                .currency(sourceAccount.getCurrency())
                .description("Credit card payment")
                .transactionDate(timeService.today())
                .build();
        Transaction savedTransaction = transactionRepository.save(transaction);

        Transfer transfer = Transfer.builder()
                .transactionId(savedTransaction.getId())
                .fromAccountId(sourceAccount.getId())
                .toAccountId(creditAccount.getId())
                .amount(amount)
                .build();
        transferRepository.save(transfer);

        openStatement.ifPresent(statement -> {
            statement.setAmountPaid(statement.getAmountPaid().add(amount));
            if (creditAccount.getBalance().compareTo(BigDecimal.ZERO) == 0
                    || statement.getAmountPaid().compareTo(statement.getMinimumPaymentDue()) >= 0) {
                statement.setStatus(StatementStatus.PAID);
            }
            creditCardStatementRepository.save(statement);
        });

        return CreditCardPaymentResponse.builder()
                .transactionId(savedTransaction.getId())
                .creditAccountId(creditAccount.getId())
                .sourceAccountId(sourceAccount.getId())
                .amountPaid(amount)
                .remainingBalance(creditAccount.getBalance())
                .paidAt(savedTransaction.getCreatedAt())
                .build();
    }

    private BigDecimal resolvePaymentAmount(
            CreditCardPaymentRequest request, Account creditAccount, CreditCardStatement openStatement) {
        return switch (request.getPaymentType()) {
            case FULL_BALANCE -> creditAccount.getBalance();
            case MINIMUM_DUE -> openStatement == null
                    ? BigDecimal.ZERO
                    : openStatement.getMinimumPaymentDue().subtract(openStatement.getAmountPaid())
                            .max(BigDecimal.ZERO);
            case CUSTOM -> {
                if (request.getAmount() == null) {
                    throw new InvalidCreditCardPaymentException("amount is required for CUSTOM payments.");
                }
                yield request.getAmount();
            }
        };
    }

    private CreditCardSummaryResponse buildSummary(Account account, CreditCardDetails details) {
        var openStatement = creditCardStatementRepository
                .findByAccountIdAndStatus(account.getId(), StatementStatus.OPEN);

        CreditCardSummaryResponse.CreditCardSummaryResponseBuilder builder = CreditCardSummaryResponse.builder()
                .creditLimit(details.getCreditLimit())
                .apr(details.getApr())
                .statementDay(details.getStatementDay())
                .gracePeriodDays(details.getGracePeriodDays())
                .availableCredit(details.getCreditLimit().subtract(account.getBalance()));

        openStatement.ifPresent(statement -> builder
                .currentStatementBalance(statement.getNewBalance())
                .minimumPaymentDue(statement.getMinimumPaymentDue().subtract(statement.getAmountPaid()).max(BigDecimal.ZERO))
                .statementDate(statement.getStatementDate())
                .paymentDueDate(statement.getDueDate())
                .statementStatus(statement.getStatus()));

        return builder.build();
    }

    private CreditCardStatementResponse toStatementResponse(CreditCardStatement statement) {
        return CreditCardStatementResponse.builder()
                .id(statement.getId())
                .accountId(statement.getAccountId())
                .statementDate(statement.getStatementDate())
                .dueDate(statement.getDueDate())
                .previousBalance(statement.getPreviousBalance())
                .newBalance(statement.getNewBalance())
                .interestCharged(statement.getInterestCharged())
                .minimumPaymentDue(statement.getMinimumPaymentDue())
                .amountPaid(statement.getAmountPaid())
                .status(statement.getStatus())
                .build();
    }

    private Account findOwnedCreditAccount(UUID currentUserId, UUID accountId) {
        Account account = accountRepository.findByIdAndUserIdAndActiveTrue(accountId, currentUserId)
                .orElseThrow(() -> new AccountNotFoundException(accountId));
        if (account.getType() != AccountType.CREDIT) {
            throw new InvalidCreditCardDetailsException("Account is not a credit card: " + accountId);
        }
        return account;
    }

    private CreditCardDetails findDetails(UUID accountId) {
        return creditCardDetailsRepository.findByAccountId(accountId)
                .orElseThrow(() -> new CreditCardDetailsNotFoundException(accountId));
    }

    private LocalDate nextOccurrence(LocalDate from, int statementDay) {
        LocalDate candidate = from.withDayOfMonth(statementDay);
        return candidate.isBefore(from) ? candidate.plusMonths(1) : candidate;
    }
}
