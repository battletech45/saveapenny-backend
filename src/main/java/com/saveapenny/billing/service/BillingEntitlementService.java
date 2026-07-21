package com.saveapenny.billing.service;

import com.saveapenny.billing.dto.EntitlementResponse;
import java.util.UUID;

public interface BillingEntitlementService {

    EntitlementResponse getEntitlement(UUID userId);

    EntitlementResponse sync(UUID userId);
}
