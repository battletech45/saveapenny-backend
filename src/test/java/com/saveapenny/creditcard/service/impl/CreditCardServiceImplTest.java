package com.saveapenny.creditcard.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.saveapenny.account.entity.Account;
import com.saveapenny.account.entity.AccountType;
import com.saveapenny.account.exception.AccountNotFoundException;
import com.saveapenny.account.repository.AccountRepository;
import com.saveapenny.config.TimeService;
import com.saveapenny.creditcard.dto.CreditCardDetailsRequest;
import com.saveapenny.creditcard.dto.CreditCardPaymentRequest;
import com.saveapenny.creditcard.dto.CreditCardPaymentResponse;
import com.saveapenny.creditcard.dto.PaymentType;
import com.saveapenny.creditcard.entity.CreditCardDetails;
import com.saveapenny.creditcard.entity.CreditCardStatement;
import com.saveapenny.creditcard.entity.StatementStatus;
import com.saveapenny.creditcard.exception.InvalidCreditCardDetailsException;
import com.saveapenny.creditcard.exception.InvalidCreditCardPaymentException;
import com.saveapenny.creditcard.repository.CreditCardDetailsRepository;
import com.saveapenny.creditcard.repository.CreditCardStatementRepository;
import com.saveapenny.transaction.exception.InsufficientBalanceException;
import com.saveapenny.transaction.repository.TransactionRepository;
import com.saveapenny.transaction.repository.TransferRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CreditCardServiceImplTest {

    @Mock
    private CreditCardDetailsRepository creditCardDetailsRepository;
    @Mock
    private CreditCardStatementRepository creditCardStatementRepository;
    @Mock
    private AccountRepository accountRepository;
    @Mock
    private TransactionRepository transactionRepository;
    @Mock
    private TransferRepository transferRepository;
    @Mock
    private TimeService timeService;

    private CreditCardServiceImpl creditCardService;

    private final UUID userId = UUID.randomUUID();
    private final UUID creditAccountId = UUID.randomUUID();
    private final UUID sourceAccountId = UUID.randomUUID();
    private final LocalDate today = LocalDate.of(2026, 6, 19);

    @BeforeEach
    void setUp() {
        creditCardService = new CreditCardServiceImpl(
                creditCardDetailsRepository,
                creditCardStatementRepository,
                accountRepository,
                transactionRepository,
                transferRepository,
                timeService);
    }

    @Test
    void createDetails_rejectsMissingFields() {
        Account account = creditAccount(BigDecimal.ZERO);
        InvalidCreditCardDetailsException ex = assertThrows(InvalidCreditCardDetailsException.class,
                () -> creditCardService.createDetails(account, CreditCardDetailsRequest.builder().build()));
        assertEquals("creditLimit, apr and statementDay are required for CREDIT accounts.", ex.getMessage());
    }

    @Test
    void createDetails_rejectsInitialBalanceAboveLimit() {
        Account account = creditAccount(new BigDecimal("600"));
        CreditCardDetailsRequest request = CreditCardDetailsRequest.builder()
                .creditLimit(new BigDecimal("500"))
                .apr(new BigDecimal("19.99"))
                .statementDay(15)
                .build();

        assertThrows(InvalidCreditCardDetailsException.class, () -> creditCardService.createDetails(account, request));
    }

    @Test
    void createDetails_savesDetailsWithComputedNextStatementDate() {
        lenient().when(timeService.today()).thenReturn(today);
        Account account = creditAccount(BigDecimal.ZERO);
        CreditCardDetailsRequest request = CreditCardDetailsRequest.builder()
                .creditLimit(new BigDecimal("2000"))
                .apr(new BigDecimal("19.99"))
                .statementDay(25)
                .build();

        creditCardService.createDetails(account, request);

        var captor = org.mockito.ArgumentCaptor.forClass(CreditCardDetails.class);
        verify(creditCardDetailsRepository).save(captor.capture());
        assertEquals(LocalDate.of(2026, 6, 25), captor.getValue().getNextStatementDate());
    }

    @Test
    void makePayment_fullBalance_movesFundsAndClosesStatement() {
        Account creditAccount = creditAccount(new BigDecimal("500"));
        Account sourceAccount = bankAccount(new BigDecimal("1000"));
        when(accountRepository.findByIdAndUserIdAndActiveTrueWithLock(creditAccountId, userId))
                .thenReturn(Optional.of(creditAccount));
        when(accountRepository.findByIdAndUserIdAndActiveTrueWithLock(sourceAccountId, userId))
                .thenReturn(Optional.of(sourceAccount));
        when(creditCardStatementRepository.findByAccountIdAndStatus(creditAccountId, StatementStatus.OPEN))
                .thenReturn(Optional.empty());
        lenient().when(timeService.today()).thenReturn(today);
        when(transactionRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        CreditCardPaymentRequest request = CreditCardPaymentRequest.builder()
                .sourceAccountId(sourceAccountId)
                .paymentType(PaymentType.FULL_BALANCE)
                .build();

        CreditCardPaymentResponse response = creditCardService.makePayment(userId, creditAccountId, request);

        assertEquals(new BigDecimal("500"), response.getAmountPaid());
        assertEquals(0, response.getRemainingBalance().compareTo(BigDecimal.ZERO));
        assertEquals(new BigDecimal("500"), sourceAccount.getBalance());
        assertEquals(0, creditAccount.getBalance().compareTo(BigDecimal.ZERO));
    }

    @Test
    void makePayment_minimumDue_usesOpenStatementAndMayLeaveStatementOpen() {
        Account creditAccount = creditAccount(new BigDecimal("1000"));
        Account sourceAccount = bankAccount(new BigDecimal("1000"));
        CreditCardStatement openStatement = CreditCardStatement.builder()
                .id(UUID.randomUUID())
                .accountId(creditAccountId)
                .minimumPaymentDue(new BigDecimal("50"))
                .amountPaid(BigDecimal.ZERO)
                .status(StatementStatus.OPEN)
                .build();

        when(accountRepository.findByIdAndUserIdAndActiveTrueWithLock(creditAccountId, userId))
                .thenReturn(Optional.of(creditAccount));
        when(accountRepository.findByIdAndUserIdAndActiveTrueWithLock(sourceAccountId, userId))
                .thenReturn(Optional.of(sourceAccount));
        when(creditCardStatementRepository.findByAccountIdAndStatus(creditAccountId, StatementStatus.OPEN))
                .thenReturn(Optional.of(openStatement));
        lenient().when(timeService.today()).thenReturn(today);
        when(transactionRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        CreditCardPaymentRequest request = CreditCardPaymentRequest.builder()
                .sourceAccountId(sourceAccountId)
                .paymentType(PaymentType.MINIMUM_DUE)
                .build();

        CreditCardPaymentResponse response = creditCardService.makePayment(userId, creditAccountId, request);

        assertEquals(new BigDecimal("50"), response.getAmountPaid());
        assertEquals(StatementStatus.PAID, openStatement.getStatus());
        assertEquals(new BigDecimal("50"), openStatement.getAmountPaid());
    }

    @Test
    void makePayment_customAmountExceedingBalance_isRejected() {
        Account creditAccount = creditAccount(new BigDecimal("100"));
        Account sourceAccount = bankAccount(new BigDecimal("1000"));
        when(accountRepository.findByIdAndUserIdAndActiveTrueWithLock(creditAccountId, userId))
                .thenReturn(Optional.of(creditAccount));
        when(accountRepository.findByIdAndUserIdAndActiveTrueWithLock(sourceAccountId, userId))
                .thenReturn(Optional.of(sourceAccount));
        when(creditCardStatementRepository.findByAccountIdAndStatus(creditAccountId, StatementStatus.OPEN))
                .thenReturn(Optional.empty());

        CreditCardPaymentRequest request = CreditCardPaymentRequest.builder()
                .sourceAccountId(sourceAccountId)
                .paymentType(PaymentType.CUSTOM)
                .amount(new BigDecimal("200"))
                .build();

        assertThrows(InvalidCreditCardPaymentException.class,
                () -> creditCardService.makePayment(userId, creditAccountId, request));
    }

    @Test
    void makePayment_insufficientSourceFunds_throws() {
        Account creditAccount = creditAccount(new BigDecimal("500"));
        Account sourceAccount = bankAccount(new BigDecimal("100"));
        when(accountRepository.findByIdAndUserIdAndActiveTrueWithLock(creditAccountId, userId))
                .thenReturn(Optional.of(creditAccount));
        when(accountRepository.findByIdAndUserIdAndActiveTrueWithLock(sourceAccountId, userId))
                .thenReturn(Optional.of(sourceAccount));
        when(creditCardStatementRepository.findByAccountIdAndStatus(creditAccountId, StatementStatus.OPEN))
                .thenReturn(Optional.empty());

        CreditCardPaymentRequest request = CreditCardPaymentRequest.builder()
                .sourceAccountId(sourceAccountId)
                .paymentType(PaymentType.FULL_BALANCE)
                .build();

        assertThrows(InsufficientBalanceException.class,
                () -> creditCardService.makePayment(userId, creditAccountId, request));
        verify(transactionRepository, never()).save(any());
    }

    @Test
    void makePayment_sourceAccountNotFound_throws() {
        Account creditAccount = creditAccount(new BigDecimal("500"));
        when(accountRepository.findByIdAndUserIdAndActiveTrueWithLock(creditAccountId, userId))
                .thenReturn(Optional.of(creditAccount));
        when(accountRepository.findByIdAndUserIdAndActiveTrueWithLock(sourceAccountId, userId))
                .thenReturn(Optional.empty());

        CreditCardPaymentRequest request = CreditCardPaymentRequest.builder()
                .sourceAccountId(sourceAccountId)
                .paymentType(PaymentType.FULL_BALANCE)
                .build();

        assertThrows(AccountNotFoundException.class,
                () -> creditCardService.makePayment(userId, creditAccountId, request));
    }

    private Account creditAccount(BigDecimal balance) {
        return Account.builder()
                .id(creditAccountId)
                .userId(userId)
                .name("Visa")
                .type(AccountType.CREDIT)
                .currency("USD")
                .balance(balance)
                .initialBalance(balance)
                .active(true)
                .build();
    }

    private Account bankAccount(BigDecimal balance) {
        return Account.builder()
                .id(sourceAccountId)
                .userId(userId)
                .name("Checking")
                .type(AccountType.BANK)
                .currency("USD")
                .balance(balance)
                .initialBalance(balance)
                .active(true)
                .build();
    }
}
