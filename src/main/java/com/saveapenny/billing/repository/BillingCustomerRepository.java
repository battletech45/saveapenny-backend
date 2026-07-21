package com.saveapenny.billing.repository;

import com.saveapenny.billing.entity.BillingCustomer;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BillingCustomerRepository extends JpaRepository<BillingCustomer, UUID> {

    Optional<BillingCustomer> findByRevenuecatAppUserId(String revenuecatAppUserId);
}
