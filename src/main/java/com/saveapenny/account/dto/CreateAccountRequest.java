package com.saveapenny.account.dto;

import com.saveapenny.account.entity.AccountType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
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
public class CreateAccountRequest {

    @NotBlank
    @Size(max = 100)
    private String name;

    @NotNull
    private AccountType type;

    @NotBlank
    @Pattern(regexp = "^[A-Z]{3}$", message = "must be a 3-letter ISO currency code")
    private String currency;

    @NotNull
    private BigDecimal initialBalance;
}
