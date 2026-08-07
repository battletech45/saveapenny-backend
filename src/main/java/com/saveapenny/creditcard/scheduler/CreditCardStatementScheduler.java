package com.saveapenny.creditcard.scheduler;

import com.saveapenny.account.entity.Account;
import com.saveapenny.account.repository.AccountRepository;
import com.saveapenny.config.TimeService;
import com.saveapenny.creditcard.entity.CreditCardDetails;
import com.saveapenny.creditcard.entity.CreditCardStatement;
import com.saveapenny.creditcard.entity.StatementStatus;
import com.saveapenny.creditcard.repository.CreditCardDetailsRepository;
import com.saveapenny.creditcard.repository.CreditCardStatementRepository;
import com.saveapenny.creditcard.support.CreditCardCategories;
import com.saveapenny.transaction.entity.Transaction;
import com.saveapenny.transaction.entity.TransactionType;
import com.saveapenny.transaction.repository.TransactionRepository;
import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

@Component
@Slf4j
public class CreditCardStatementScheduler {

    private static final BigDecimal MINIMUM_PAYMENT_FLOOR = new BigDecimal("25.00");
    private static final BigDecimal MINIMUM_PAYMENT_PERCENT = new BigDecimal("0.02");
    private static final MathContext MATH_CONTEXT = new MathContext(10, RoundingMode.HALF_UP);

    private final CreditCardDetailsRepository creditCardDetailsRepository;
    private final CreditCardStatementRepository creditCardStatementRepository;
    private final AccountRepository accountRepository;
    private final TransactionRepository transactionRepository;
    private final TimeService timeService;
    private final TransactionTemplate requiresNewTransactionTemplate;

    public CreditCardStatementScheduler(
            CreditCardDetailsRepository creditCardDetailsRepository,
            CreditCardStatementRepository creditCardStatementRepository,
            AccountRepository accountRepository,
            TransactionRepository transactionRepository,
            TimeService timeService,
            PlatformTransactionManager transactionManager) {
        this.creditCardDetailsRepository = creditCardDetailsRepository;
        this.creditCardStatementRepository = creditCardStatementRepository;
        this.accountRepository = accountRepository;
        this.transactionRepository = transactionRepository;
        this.timeService = timeService;
        this.requiresNewTransactionTemplate = new TransactionTemplate(transactionManager);
        this.requiresNewTransactionTemplate.setPropagationBehavior(
                org.springframework.transaction.TransactionDefinition.PROPAGATION_REQUIRES_NEW);
    }

    @Scheduled(cron = "${credit-card.statement.cron:0 0 3 * * *}")
    public void run() {
        LocalDate today = timeService.today();
        evaluateDueStatements(today);
        closeStatements(today);
    }

    private void evaluateDueStatements(LocalDate today) {
        List<CreditCardStatement> dueStatements =
                creditCardStatementRepository.findAllByStatusAndDueDate(StatementStatus.OPEN, today);
        for (CreditCardStatement statement : dueStatements) {
            try {
                requiresNewTransactionTemplate.executeWithoutResult(status -> evaluateDueStatement(statement.getId()));
            } catch (RuntimeException ex) {
                log.warn("Failed to evaluate due credit card statement {}: {}", statement.getId(), ex.getMessage());
            }
        }
    }

    private void evaluateDueStatement(UUID statementId) {
        creditCardStatementRepository.findById(statementId).ifPresent(statement -> {
            if (statement.getStatus() != StatementStatus.OPEN) {
                return;
            }
            statement.setStatus(statement.getAmountPaid().compareTo(statement.getMinimumPaymentDue()) >= 0
                    ? StatementStatus.PAID
                    : StatementStatus.MISSED);
            creditCardStatementRepository.save(statement);
        });
    }

    private void closeStatements(LocalDate today) {
        List<CreditCardDetails> dueForClosing = creditCardDetailsRepository.findAllByNextStatementDate(today);
        for (CreditCardDetails details : dueForClosing) {
            try {
                requiresNewTransactionTemplate.executeWithoutResult(status -> closeStatement(details.getAccountId(), today));
            } catch (RuntimeException ex) {
                log.warn("Failed to close credit card statement for account {}: {}", details.getAccountId(), ex.getMessage());
            }
        }
    }

    private void closeStatement(UUID accountId, LocalDate today) {
        CreditCardDetails details = creditCardDetailsRepository.findByAccountIdWithLock(accountId).orElse(null);
        if (details == null || !today.equals(details.getNextStatementDate())) {
            return;
        }
        Account account = accountRepository.findById(accountId).orElse(null);
        if (account == null) {
            return;
        }

        Optional<CreditCardStatement> previous =
                creditCardStatementRepository.findFirstByAccountIdOrderByStatementDateDesc(accountId);
        BigDecimal previousBalance = previous.map(CreditCardStatement::getNewBalance).orElse(account.getInitialBalance());

        BigDecimal carriedBalance = previous
                .map(p -> p.getNewBalance().subtract(p.getAmountPaid()).max(BigDecimal.ZERO))
                .orElse(BigDecimal.ZERO);
        BigDecimal interest = BigDecimal.ZERO;
        if (carriedBalance.compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal monthlyRate = details.getApr()
                    .divide(BigDecimal.valueOf(1200), MATH_CONTEXT);
            interest = carriedBalance.multiply(monthlyRate, MATH_CONTEXT).setScale(4, RoundingMode.HALF_UP);
        }

        if (interest.compareTo(BigDecimal.ZERO) > 0) {
            account.setBalance(account.getBalance().add(interest));
            accountRepository.save(account);

            Transaction interestTransaction = Transaction.builder()
                    .userId(account.getUserId())
                    .accountId(account.getId())
                    .categoryId(CreditCardCategories.INTEREST_AND_FEES)
                    .type(TransactionType.EXPENSE)
                    .amount(interest)
                    .currency(account.getCurrency())
                    .description("Credit card interest charge")
                    .transactionDate(today)
                    .build();
            transactionRepository.save(interestTransaction);
        }

        BigDecimal newBalance = account.getBalance();
        BigDecimal pastDue = previous
                .filter(p -> p.getStatus() == StatementStatus.MISSED)
                .map(p -> p.getMinimumPaymentDue().subtract(p.getAmountPaid()).max(BigDecimal.ZERO))
                .orElse(BigDecimal.ZERO);
        BigDecimal minimumPaymentDue = computeMinimumPaymentDue(newBalance, pastDue);
        LocalDate dueDate = today.plusDays(details.getGracePeriodDays());

        CreditCardStatement statement = CreditCardStatement.builder()
                .accountId(accountId)
                .userId(account.getUserId())
                .statementDate(today)
                .dueDate(dueDate)
                .previousBalance(previousBalance)
                .newBalance(newBalance)
                .interestCharged(interest)
                .minimumPaymentDue(minimumPaymentDue)
                .status(StatementStatus.OPEN)
                .build();
        creditCardStatementRepository.save(statement);

        details.setLastStatementDate(today);
        details.setNextStatementDate(today.plusMonths(1));
        creditCardDetailsRepository.save(details);
    }

    private BigDecimal computeMinimumPaymentDue(BigDecimal newBalance, BigDecimal pastDue) {
        if (newBalance.compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ZERO;
        }
        BigDecimal baseMinimum = MINIMUM_PAYMENT_FLOOR.max(newBalance.multiply(MINIMUM_PAYMENT_PERCENT, MATH_CONTEXT));
        return baseMinimum.add(pastDue).min(newBalance).setScale(4, RoundingMode.HALF_UP);
    }
}
