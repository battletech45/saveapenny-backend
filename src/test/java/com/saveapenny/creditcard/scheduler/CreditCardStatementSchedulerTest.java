package com.saveapenny.creditcard.scheduler;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;

import com.saveapenny.account.entity.Account;
import com.saveapenny.account.entity.AccountType;
import com.saveapenny.account.repository.AccountRepository;
import com.saveapenny.config.TimeService;
import com.saveapenny.creditcard.entity.CreditCardDetails;
import com.saveapenny.creditcard.entity.CreditCardStatement;
import com.saveapenny.creditcard.entity.StatementStatus;
import com.saveapenny.creditcard.repository.CreditCardDetailsRepository;
import com.saveapenny.creditcard.repository.CreditCardStatementRepository;
import com.saveapenny.transaction.repository.TransactionRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.SimpleTransactionStatus;

@ExtendWith(MockitoExtension.class)
class CreditCardStatementSchedulerTest {

    @Mock
    private CreditCardDetailsRepository creditCardDetailsRepository;
    @Mock
    private CreditCardStatementRepository creditCardStatementRepository;
    @Mock
    private AccountRepository accountRepository;
    @Mock
    private TransactionRepository transactionRepository;
    @Mock
    private TimeService timeService;
    @Mock
    private PlatformTransactionManager transactionManager;

    private CreditCardStatementScheduler scheduler;

    private final LocalDate today = LocalDate.of(2026, 6, 19);
    private final UUID accountId = UUID.randomUUID();
    private final UUID userId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        when(timeService.today()).thenReturn(today);
        TransactionStatus transactionStatus = new SimpleTransactionStatus();
        when(transactionManager.getTransaction(any())).thenReturn(transactionStatus);
        doNothing().when(transactionManager).commit(transactionStatus);
        scheduler = new CreditCardStatementScheduler(
                creditCardDetailsRepository,
                creditCardStatementRepository,
                accountRepository,
                transactionRepository,
                timeService,
                transactionManager);
    }

    @Test
    void run_closesStatement_noCarriedBalance_noInterest() {
        when(creditCardStatementRepository.findAllByStatusAndDueDate(StatementStatus.OPEN, today))
                .thenReturn(List.of());
        CreditCardDetails details = CreditCardDetails.builder()
                .accountId(accountId)
                .creditLimit(new BigDecimal("1000"))
                .apr(new BigDecimal("24.00"))
                .statementDay(19)
                .gracePeriodDays(21)
                .nextStatementDate(today)
                .build();
        when(creditCardDetailsRepository.findAllByNextStatementDate(today)).thenReturn(List.of(details));
        when(creditCardDetailsRepository.findByAccountIdWithLock(accountId)).thenReturn(Optional.of(details));

        Account account = Account.builder()
                .id(accountId)
                .userId(userId)
                .type(AccountType.CREDIT)
                .currency("USD")
                .balance(new BigDecimal("200.0000"))
                .initialBalance(BigDecimal.ZERO)
                .build();
        when(accountRepository.findById(accountId)).thenReturn(Optional.of(account));
        when(creditCardStatementRepository.findFirstByAccountIdOrderByStatementDateDesc(accountId))
                .thenReturn(Optional.empty());

        scheduler.run();

        ArgumentCaptor<CreditCardStatement> captor = ArgumentCaptor.forClass(CreditCardStatement.class);
        org.mockito.Mockito.verify(creditCardStatementRepository).save(captor.capture());
        CreditCardStatement saved = captor.getValue();
        assertEquals(new BigDecimal("200.0000"), saved.getNewBalance());
        assertEquals(0, saved.getInterestCharged().compareTo(BigDecimal.ZERO));
        assertEquals(today.plusDays(21), saved.getDueDate());
        assertEquals(StatementStatus.OPEN, saved.getStatus());
        org.mockito.Mockito.verify(transactionRepository, org.mockito.Mockito.never()).save(any());

        ArgumentCaptor<CreditCardDetails> detailsCaptor = ArgumentCaptor.forClass(CreditCardDetails.class);
        org.mockito.Mockito.verify(creditCardDetailsRepository).save(detailsCaptor.capture());
        assertEquals(today.plusMonths(1), detailsCaptor.getValue().getNextStatementDate());
    }

    @Test
    void run_chargesInterest_whenPreviousBalanceCarried() {
        when(creditCardStatementRepository.findAllByStatusAndDueDate(StatementStatus.OPEN, today))
                .thenReturn(List.of());
        CreditCardDetails details = CreditCardDetails.builder()
                .accountId(accountId)
                .creditLimit(new BigDecimal("1000"))
                .apr(new BigDecimal("24.00"))
                .statementDay(19)
                .gracePeriodDays(21)
                .nextStatementDate(today)
                .build();
        when(creditCardDetailsRepository.findAllByNextStatementDate(today)).thenReturn(List.of(details));
        when(creditCardDetailsRepository.findByAccountIdWithLock(accountId)).thenReturn(Optional.of(details));

        Account account = Account.builder()
                .id(accountId)
                .userId(userId)
                .type(AccountType.CREDIT)
                .currency("USD")
                .balance(new BigDecimal("300.0000"))
                .initialBalance(BigDecimal.ZERO)
                .build();
        when(accountRepository.findById(accountId)).thenReturn(Optional.of(account));

        CreditCardStatement previous = CreditCardStatement.builder()
                .id(UUID.randomUUID())
                .accountId(accountId)
                .newBalance(new BigDecimal("300.0000"))
                .amountPaid(BigDecimal.ZERO)
                .minimumPaymentDue(new BigDecimal("25.0000"))
                .status(StatementStatus.MISSED)
                .build();
        when(creditCardStatementRepository.findFirstByAccountIdOrderByStatementDateDesc(accountId))
                .thenReturn(Optional.of(previous));

        scheduler.run();

        // 300 * (24 / 1200) = 6.00 interest
        assertEquals(new BigDecimal("306.0000"), account.getBalance());
        org.mockito.Mockito.verify(transactionRepository).save(any());

        ArgumentCaptor<CreditCardStatement> captor = ArgumentCaptor.forClass(CreditCardStatement.class);
        org.mockito.Mockito.verify(creditCardStatementRepository).save(captor.capture());
        assertEquals(0, captor.getValue().getInterestCharged().compareTo(new BigDecimal("6.0000")));
    }

    @Test
    void run_marksMissed_whenDueStatementUnderpaid() {
        CreditCardStatement due = CreditCardStatement.builder()
                .id(UUID.randomUUID())
                .accountId(accountId)
                .status(StatementStatus.OPEN)
                .minimumPaymentDue(new BigDecimal("25.0000"))
                .amountPaid(new BigDecimal("10.0000"))
                .build();
        when(creditCardStatementRepository.findAllByStatusAndDueDate(StatementStatus.OPEN, today))
                .thenReturn(List.of(due));
        when(creditCardStatementRepository.findById(due.getId())).thenReturn(Optional.of(due));
        when(creditCardDetailsRepository.findAllByNextStatementDate(today)).thenReturn(List.of());

        scheduler.run();

        assertEquals(StatementStatus.MISSED, due.getStatus());
    }
}
