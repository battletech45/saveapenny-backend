package com.saveapenny.account.repository;

import com.saveapenny.account.entity.Account;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AccountRepository extends JpaRepository<Account, UUID> {

    Page<Account> findAllByUserIdAndActiveTrue(UUID userId, Pageable pageable);

    List<Account> findAllByUserIdAndActiveTrue(UUID userId);

    Optional<Account> findByIdAndUserIdAndActiveTrue(UUID id, UUID userId);

    boolean existsByIdAndUserIdAndActiveTrue(UUID id, UUID userId);

    boolean existsByUserIdAndNameIgnoreCaseAndActiveTrue(UUID userId, String name);

    boolean existsByUserIdAndNameIgnoreCaseAndActiveTrueAndIdNot(UUID userId, String name, UUID id);
}
