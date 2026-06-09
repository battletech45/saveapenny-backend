package com.saveapenny.account.repository;

import com.saveapenny.account.entity.Account;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import jakarta.persistence.LockModeType;

public interface AccountRepository extends JpaRepository<Account, UUID> {

    Page<Account> findAllByUserIdAndActiveTrue(UUID userId, Pageable pageable);

    List<Account> findAllByUserIdAndActiveTrue(UUID userId);

    Optional<Account> findByIdAndUserIdAndActiveTrue(UUID id, UUID userId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT a FROM Account a WHERE a.id = :id AND a.userId = :userId AND a.active = true")
    Optional<Account> findByIdAndUserIdAndActiveTrueWithLock(@Param("id") UUID id, @Param("userId") UUID userId);

    boolean existsByIdAndUserIdAndActiveTrue(UUID id, UUID userId);

    boolean existsByUserIdAndNameIgnoreCaseAndActiveTrue(UUID userId, String name);

    boolean existsByUserIdAndNameIgnoreCaseAndActiveTrueAndIdNot(UUID userId, String name, UUID id);
}
