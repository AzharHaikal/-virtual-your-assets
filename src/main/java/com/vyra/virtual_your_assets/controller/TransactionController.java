package com.vyra.virtual_your_assets.controller;

import com.vyra.virtual_your_assets.constant.ApiPath;
import com.vyra.virtual_your_assets.dto.BaseResponse;
import com.vyra.virtual_your_assets.dto.transaction.CreateTransactionRequest;
import com.vyra.virtual_your_assets.dto.transaction.CreateTransactionResponse;
import com.vyra.virtual_your_assets.service.TransactionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import static com.vyra.virtual_your_assets.constant.ApiPath.CREATE_TRANSACTION;

@RestController
@RequiredArgsConstructor
@RequestMapping(ApiPath.V1_TRANSACTION)
public class TransactionController {
    private final TransactionService transactionService;

    @PostMapping(CREATE_TRANSACTION)
    public BaseResponse<CreateTransactionResponse> createWallet(@Valid CreateTransactionRequest request) {
        return transactionService.createTransaction(request);
    }
}
