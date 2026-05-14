package com.vyra.virtual_your_assets.controller;

import com.vyra.virtual_your_assets.constant.ApiPath;
import com.vyra.virtual_your_assets.dto.BaseResponse;
import com.vyra.virtual_your_assets.dto.transaction.CreateTransactionRequest;
import com.vyra.virtual_your_assets.dto.transaction.CreateTransactionResponse;
import com.vyra.virtual_your_assets.dto.transaction.TransactionHistoryResponse;
import com.vyra.virtual_your_assets.security.model.CustomUserDetails;
import com.vyra.virtual_your_assets.service.TransactionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

import static com.vyra.virtual_your_assets.constant.ApiPath.CREATE_TRANSACTION;
import static com.vyra.virtual_your_assets.constant.ApiPath.GET_HISTORY_TRANSACTION;

@RestController
@RequiredArgsConstructor
@RequestMapping(ApiPath.V1_TRANSACTION)
public class TransactionController {
    private final TransactionService transactionService;

    @PostMapping(CREATE_TRANSACTION)
    public BaseResponse<CreateTransactionResponse> createWallet(
            Authentication authentication,
            @RequestBody @Valid CreateTransactionRequest request
    ) {
        CustomUserDetails user = (CustomUserDetails) authentication.getPrincipal();
        return transactionService.createTransaction(user.getMemberId(), request);
    }

    @GetMapping(GET_HISTORY_TRANSACTION)
    public BaseResponse<List<TransactionHistoryResponse>> getHistory(Authentication authentication) {
        CustomUserDetails user = (CustomUserDetails) authentication.getPrincipal();
        return transactionService.getHistory(user.getMemberId());
    }
}
