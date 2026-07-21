package com.saveapenny.billing.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.OffsetDateTime;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EntitlementResponse {

    private String plan;
    private String status;

    @Getter(AccessLevel.NONE)
    private boolean active;
    private boolean willRenew;
    private OffsetDateTime expiresAt;
    private OffsetDateTime trialEndsAt;
    private FeatureAccessResponse features;
    private EntitlementLimitsResponse limits;

    @JsonProperty("isActive")
    public boolean isActive() {
        return active;
    }
}
