package com.saveapenny.account.dto;

import com.saveapenny.account.entity.AccountType;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;
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
public class AccountResponse {

    private UUID id;
    private String name;
    private AccountType type;
    private String currency;
    private BigDecimal balance;
    private BigDecimal initialBalance;
    private Boolean active;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
}
