package com.saveapenny.billing.repository;

import com.saveapenny.billing.entity.BillingEntitlement;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BillingEntitlementRepository extends JpaRepository<BillingEntitlement, UUID> {}
