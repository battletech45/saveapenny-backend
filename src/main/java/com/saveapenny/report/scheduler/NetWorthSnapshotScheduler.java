package com.saveapenny.report.scheduler;

import com.saveapenny.config.TimeService;
import com.saveapenny.report.entity.NetWorthSnapshot;
import com.saveapenny.report.repository.NetWorthSnapshotRepository;
import com.saveapenny.report.repository.ReportAccountRepository;
import com.saveapenny.account.entity.AccountType;
import com.saveapenny.user.repository.UserRepository;
import java.util.List;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

@Component
public class NetWorthSnapshotScheduler {

    private static final Logger log = LoggerFactory.getLogger(NetWorthSnapshotScheduler.class);

    private final UserRepository userRepository;
    private final ReportAccountRepository reportAccountRepository;
    private final NetWorthSnapshotRepository netWorthSnapshotRepository;
    private final TransactionTemplate requiresNewTransactionTemplate;
    private final TimeService timeService;

    public NetWorthSnapshotScheduler(
            UserRepository userRepository,
            ReportAccountRepository reportAccountRepository,
            NetWorthSnapshotRepository netWorthSnapshotRepository,
            PlatformTransactionManager transactionManager,
            TimeService timeService) {
        this.userRepository = userRepository;
        this.reportAccountRepository = reportAccountRepository;
        this.netWorthSnapshotRepository = netWorthSnapshotRepository;
        this.timeService = timeService;
        this.requiresNewTransactionTemplate = new TransactionTemplate(transactionManager);
        this.requiresNewTransactionTemplate.setPropagationBehavior(org.springframework.transaction.TransactionDefinition.PROPAGATION_REQUIRES_NEW);
    }

    @Scheduled(cron = "${report.net-worth.snapshot-cron:0 0 2 * * *}")
    public void computeDailySnapshots() {
        LocalDate yesterday = timeService.today().minusDays(1);
        int pageNumber = 0;
        Page<UUID> page;
        do {
            page = userRepository.findAllUserIds(PageRequest.of(pageNumber, 100));
            for (UUID userId : page.getContent()) {
                try {
                    requiresNewTransactionTemplate.executeWithoutResult(status -> computeSnapshotForUser(userId, yesterday));
                } catch (RuntimeException ex) {
                    log.warn("Failed to compute net worth snapshot for user {} on {}: {}", userId, yesterday, ex.getMessage());
                }
            }
            pageNumber++;
        } while (page.hasNext());
    }

    private void computeSnapshotForUser(UUID userId, LocalDate snapshotDate) {
        if (netWorthSnapshotRepository.findByUserIdAndSnapshotDate(userId, snapshotDate).isPresent()) {
            return;
        }

        BigDecimal totalAssets = nullSafeAmount(reportAccountRepository.sumAssetsByUserId(userId, List.of(AccountType.CREDIT)));
        BigDecimal totalLiabilities = nullSafeAmount(reportAccountRepository.sumLiabilitiesByUserId(userId, List.of(AccountType.CREDIT)));
        BigDecimal netWorth = totalAssets.subtract(totalLiabilities);

        NetWorthSnapshot snapshot = NetWorthSnapshot.builder()
                .userId(userId)
                .snapshotDate(snapshotDate)
                .totalAssets(totalAssets)
                .totalLiabilities(totalLiabilities)
                .netWorth(netWorth)
                .build();
        netWorthSnapshotRepository.save(snapshot);
    }

    private BigDecimal nullSafeAmount(BigDecimal amount) {
        return amount == null ? BigDecimal.ZERO : amount;
    }
}
