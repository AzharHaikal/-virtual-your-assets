package com.vyra.be_virtual_your_assets.controller;

import com.vyra.be_virtual_your_assets.constant.ApiPath;
import com.vyra.be_virtual_your_assets.dto.BaseResponse;
import com.vyra.be_virtual_your_assets.dto.transaction.TransactionMutationResponse;
import com.vyra.be_virtual_your_assets.dto.transaction.UpdateTransactionRequest;
import com.vyra.be_virtual_your_assets.security.model.CustomUserDetails;
import com.vyra.be_virtual_your_assets.service.TransactionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import static com.vyra.be_virtual_your_assets.constant.ApiPath.TRANSACTION_ID;

/**
 * Member's own transactions, REST style (V2). The V1 endpoints under /api/v1/transaction stay for older apps.
 * A transaction of another member answers 404, exactly like a missing one.
 */
@RestController
@RequiredArgsConstructor
@RequestMapping(ApiPath.V1_TRANSACTIONS)
public class MemberTransactionController {

    private final TransactionService transactionService;

    @PatchMapping(TRANSACTION_ID)
    public BaseResponse<TransactionMutationResponse> updateTransaction(
            @PathVariable String transactionId,
            @Valid @RequestBody UpdateTransactionRequest request,
            Authentication authentication
    ) {
        CustomUserDetails user = (CustomUserDetails) authentication.getPrincipal();
        return transactionService.updateOwnTransaction(user.getMemberId(), transactionId, request);
    }

    @DeleteMapping(TRANSACTION_ID)
    public BaseResponse<TransactionMutationResponse> deleteTransaction(
            @PathVariable String transactionId,
            Authentication authentication
    ) {
        CustomUserDetails user = (CustomUserDetails) authentication.getPrincipal();
        return transactionService.deleteOwnTransaction(user.getMemberId(), transactionId);
    }
}
