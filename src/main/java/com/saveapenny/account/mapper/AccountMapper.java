package com.saveapenny.account.mapper;

import com.saveapenny.account.dto.AccountResponse;
import com.saveapenny.account.dto.CreateAccountRequest;
import com.saveapenny.account.dto.UpdateAccountRequest;
import com.saveapenny.account.entity.Account;
import java.math.BigDecimal;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring")
public interface AccountMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "userId", ignore = true)
    @Mapping(target = "balance", expression = "java(defaultBalance(request.getInitialBalance()))")
    @Mapping(target = "active", constant = "true")
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    Account toEntity(CreateAccountRequest request);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "userId", ignore = true)
    @Mapping(target = "initialBalance", ignore = true)
    @Mapping(target = "balance", ignore = true)
    @Mapping(target = "active", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    void updateEntity(@MappingTarget Account account, UpdateAccountRequest request);

    AccountResponse toResponse(Account account);

    default BigDecimal defaultBalance(BigDecimal initialBalance) {
        return initialBalance == null ? BigDecimal.ZERO : initialBalance;
    }
}
