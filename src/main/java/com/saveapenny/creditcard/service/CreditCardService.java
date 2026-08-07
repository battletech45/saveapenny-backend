package com.saveapenny.creditcard.service;

import com.saveapenny.account.entity.Account;
import com.saveapenny.creditcard.dto.CreditCardDetailsRequest;
import com.saveapenny.creditcard.dto.CreditCardPaymentRequest;
import com.saveapenny.creditcard.dto.CreditCardPaymentResponse;
import com.saveapenny.creditcard.dto.CreditCardStatementResponse;
import com.saveapenny.creditcard.dto.CreditCardSummaryResponse;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface CreditCardService {

    void createDetails(Account account, CreditCardDetailsRequest request);

    CreditCardSummaryResponse updateDetails(UUID currentUserId, UUID accountId, CreditCardDetailsRequest request);

    CreditCardSummaryResponse getSummary(Account account);

    Page<CreditCardStatementResponse> listStatements(UUID currentUserId, UUID accountId, Pageable pageable);

    CreditCardPaymentResponse makePayment(UUID currentUserId, UUID accountId, CreditCardPaymentRequest request);
}
