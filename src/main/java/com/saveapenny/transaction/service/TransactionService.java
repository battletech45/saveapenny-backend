package com.saveapenny.transaction.service;

import com.saveapenny.transaction.dto.CreateTransactionRequest;
import com.saveapenny.transaction.dto.CreateTransferRequest;
import com.saveapenny.transaction.dto.TransactionResponse;
import com.saveapenny.transaction.dto.TransferResponse;
import com.saveapenny.transaction.dto.UpdateTransactionRequest;
import com.saveapenny.transaction.entity.TransactionType;
import java.time.LocalDate;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface TransactionService {

    TransactionResponse create(UUID currentUserId, CreateTransactionRequest request);

    TransferResponse createTransfer(UUID currentUserId, CreateTransferRequest request);

    Page<TransactionResponse> getAll(
            UUID currentUserId,
            LocalDate from,
            LocalDate to,
            TransactionType type,
            UUID accountId,
            UUID categoryId,
            Pageable pageable);

    TransactionResponse getById(UUID currentUserId, UUID transactionId);

    TransactionResponse update(UUID currentUserId, UUID transactionId, UpdateTransactionRequest request);

    void delete(UUID currentUserId, UUID transactionId);
}
