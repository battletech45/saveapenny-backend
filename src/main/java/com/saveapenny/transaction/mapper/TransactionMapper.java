package com.saveapenny.transaction.mapper;

import com.saveapenny.transaction.dto.CreateTransactionRequest;
import com.saveapenny.transaction.dto.TransactionResponse;
import com.saveapenny.transaction.dto.TransferResponse;
import com.saveapenny.transaction.dto.UpdateTransactionRequest;
import com.saveapenny.transaction.entity.Transaction;
import com.saveapenny.transaction.entity.Transfer;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring")
public interface TransactionMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "userId", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    Transaction toEntity(CreateTransactionRequest request);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "userId", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    void updateEntity(@MappingTarget Transaction transaction, UpdateTransactionRequest request);

    TransactionResponse toResponse(Transaction transaction);

    @Mapping(target = "transactionId", source = "transaction.id")
    @Mapping(target = "fromAccountId", source = "transfer.fromAccountId")
    @Mapping(target = "toAccountId", source = "transfer.toAccountId")
    @Mapping(target = "categoryId", source = "transaction.categoryId")
    @Mapping(target = "amount", source = "transfer.amount")
    @Mapping(target = "currency", source = "transaction.currency")
    @Mapping(target = "description", source = "transaction.description")
    @Mapping(target = "transactionDate", source = "transaction.transactionDate")
    @Mapping(target = "createdAt", source = "transaction.createdAt")
    TransferResponse toTransferResponse(Transaction transaction, Transfer transfer);
}
