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
import org.springframework.data.domain.Page;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

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
    public BaseResponse<Page<TransactionHistoryResponse>> getTransactionHistory(
            @RequestParam String startDate,
            @RequestParam String endDate,
            @RequestParam(required = false) String type,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            Authentication authentication
    ) {
        CustomUserDetails user = (CustomUserDetails) authentication.getPrincipal();
        return transactionService.getTransactionHistory(startDate, endDate, type, page, size, user.getMemberId()
        );
    }
}
