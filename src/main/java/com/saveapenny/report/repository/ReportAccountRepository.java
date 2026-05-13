package com.saveapenny.report.repository;

import com.saveapenny.account.entity.Account;
import com.saveapenny.account.entity.AccountType;
import java.math.BigDecimal;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ReportAccountRepository extends JpaRepository<Account, UUID> {

    @Query("""
            select coalesce(sum(a.balance), 0)
            from Account a
            where a.userId = :userId
              and a.active = true
              and a.type <> :liabilityType
            """)
    BigDecimal sumAssetsByUserId(@Param("userId") UUID userId, @Param("liabilityType") AccountType liabilityType);

    @Query("""
            select coalesce(sum(a.balance), 0)
            from Account a
            where a.userId = :userId
              and a.active = true
              and a.type = :liabilityType
            """)
    BigDecimal sumLiabilitiesByUserId(@Param("userId") UUID userId, @Param("liabilityType") AccountType liabilityType);
}
