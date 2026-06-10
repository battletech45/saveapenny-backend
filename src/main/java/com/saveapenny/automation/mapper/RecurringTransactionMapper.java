package com.saveapenny.automation.mapper;

import com.saveapenny.automation.dto.CreateRecurringTransactionRequest;
import com.saveapenny.automation.dto.RecurringTransactionResponse;
import com.saveapenny.automation.dto.UpdateRecurringTransactionRequest;
import com.saveapenny.automation.entity.RecurringTransaction;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring")
public interface RecurringTransactionMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "userId", ignore = true)
    @Mapping(target = "status", constant = "ACTIVE")
    @Mapping(target = "lastRunAt", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    RecurringTransaction toEntity(CreateRecurringTransactionRequest request);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "userId", ignore = true)
    @Mapping(target = "lastRunAt", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    void updateEntity(@MappingTarget RecurringTransaction recurringTransaction, UpdateRecurringTransactionRequest request);

    RecurringTransactionResponse toResponse(RecurringTransaction recurringTransaction);
}
