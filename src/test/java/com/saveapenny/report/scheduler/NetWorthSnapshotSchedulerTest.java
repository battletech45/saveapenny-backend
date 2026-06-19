package com.saveapenny.report.scheduler;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.saveapenny.account.entity.AccountType;
import com.saveapenny.config.TimeService;
import com.saveapenny.report.repository.NetWorthSnapshotRepository;
import com.saveapenny.report.repository.ReportAccountRepository;
import com.saveapenny.user.repository.UserRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.SimpleTransactionStatus;

@ExtendWith(MockitoExtension.class)
class NetWorthSnapshotSchedulerTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private ReportAccountRepository reportAccountRepository;
    @Mock
    private NetWorthSnapshotRepository netWorthSnapshotRepository;
    @Mock
    private PlatformTransactionManager transactionManager;
    @Mock
    private TimeService timeService;

    private NetWorthSnapshotScheduler scheduler;

    private final LocalDate fixedToday = LocalDate.of(2026, 6, 19);
    private final LocalDate yesterday = fixedToday.minusDays(1);

    @BeforeEach
    void setUp() {
        when(timeService.today()).thenReturn(fixedToday);
        TransactionStatus transactionStatus = new SimpleTransactionStatus();
        when(transactionManager.getTransaction(any())).thenReturn(transactionStatus);
        doNothing().when(transactionManager).commit(transactionStatus);
        scheduler = new NetWorthSnapshotScheduler(userRepository, reportAccountRepository, netWorthSnapshotRepository, transactionManager, timeService);
    }

    @Test
    void computeDailySnapshots_paginatesAndSavesPerUser() {
        UUID user1 = UUID.randomUUID();
        UUID user2 = UUID.randomUUID();

        when(userRepository.findAllUserIds(PageRequest.of(0, 100)))
                .thenReturn(new PageImpl<>(List.of(user1), PageRequest.of(0, 100), 101));
        when(userRepository.findAllUserIds(PageRequest.of(1, 100)))
                .thenReturn(new PageImpl<>(List.of(user2), PageRequest.of(1, 100), 101));
        when(netWorthSnapshotRepository.findByUserIdAndSnapshotDate(user1, yesterday)).thenReturn(Optional.empty());
        when(netWorthSnapshotRepository.findByUserIdAndSnapshotDate(user2, yesterday)).thenReturn(Optional.empty());
        when(reportAccountRepository.sumAssetsByUserId(any(), any())).thenReturn(new BigDecimal("100.00"));
        when(reportAccountRepository.sumLiabilitiesByUserId(any(), any())).thenReturn(new BigDecimal("25.00"));

        scheduler.computeDailySnapshots();

        verify(reportAccountRepository).sumAssetsByUserId(user1, List.of(AccountType.CREDIT));
        verify(reportAccountRepository).sumAssetsByUserId(user2, List.of(AccountType.CREDIT));
        verify(netWorthSnapshotRepository, times(2)).save(any());
    }

    @Test
    void computeDailySnapshots_skipsExistingSnapshot() {
        UUID userId = UUID.randomUUID();

        when(userRepository.findAllUserIds(PageRequest.of(0, 100)))
                .thenReturn(new PageImpl<>(List.of(userId), PageRequest.of(0, 100), 1));
        when(netWorthSnapshotRepository.findByUserIdAndSnapshotDate(userId, yesterday))
                .thenReturn(Optional.of(com.saveapenny.report.entity.NetWorthSnapshot.builder().userId(userId).snapshotDate(yesterday).build()));

        scheduler.computeDailySnapshots();

        verify(reportAccountRepository, never()).sumAssetsByUserId(any(), any());
        verify(netWorthSnapshotRepository, never()).save(any());
    }
}
