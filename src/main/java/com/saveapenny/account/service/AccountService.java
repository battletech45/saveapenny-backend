package com.saveapenny.account.service;

import com.saveapenny.account.dto.AccountResponse;
import com.saveapenny.account.dto.CreateAccountRequest;
import com.saveapenny.account.dto.UpdateAccountRequest;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface AccountService {

    AccountResponse create(UUID currentUserId, CreateAccountRequest request);

    Page<AccountResponse> getAll(UUID currentUserId, Pageable pageable);

    AccountResponse getById(UUID currentUserId, UUID accountId);

    AccountResponse update(UUID currentUserId, UUID accountId, UpdateAccountRequest request);

    void delete(UUID currentUserId, UUID accountId);
}
