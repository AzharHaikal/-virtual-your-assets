package com.vyra.be_virtual_your_assets.controller;

import com.vyra.be_virtual_your_assets.constant.ApiPath;
import com.vyra.be_virtual_your_assets.constant.transaction.TransactionType;
import com.vyra.be_virtual_your_assets.dto.BaseResponse;
import com.vyra.be_virtual_your_assets.dto.chart.GetChartResponse;
import com.vyra.be_virtual_your_assets.dto.transaction.CreateTransactionRequest;
import com.vyra.be_virtual_your_assets.dto.transaction.CreateTransactionResponse;
import com.vyra.be_virtual_your_assets.dto.transaction.TransactionHistoryResponse;
import com.vyra.be_virtual_your_assets.security.model.CustomUserDetails;
import com.vyra.be_virtual_your_assets.service.TransactionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

import static com.vyra.be_virtual_your_assets.constant.ApiPath.*;

@RestController
@RequiredArgsConstructor
@RequestMapping(ApiPath.V1_TRANSACTION)
public class TransactionController {
    private final TransactionService transactionService;

    @GetMapping(GET_CHART)
    public BaseResponse<GetChartResponse> getChart(@RequestParam String period, Authentication authentication) {
        CustomUserDetails user = (CustomUserDetails) authentication.getPrincipal();
        return transactionService.getChart(user.getMemberId(), period);
    }

    @PostMapping(CREATE_TRANSACTION)
    public BaseResponse<CreateTransactionResponse> createTransaction(
            @RequestBody @Valid CreateTransactionRequest request,
            Authentication authentication
    ) {
        CustomUserDetails user = (CustomUserDetails) authentication.getPrincipal();
        return transactionService.createTransaction(user.getMemberId(), request);
    }

    @GetMapping(GET_TOP_TRANSACTION_HISTORY)
    public BaseResponse<List<TransactionHistoryResponse>> getTopTransactionHistory(Authentication authentication) {
        CustomUserDetails user = (CustomUserDetails) authentication.getPrincipal();
        return transactionService.getTopTransactionHistory(user.getMemberId());
    }

    @GetMapping(GET_TRANSACTION_HISTORY)
    public BaseResponse<Page<TransactionHistoryResponse>> getTransactionHistory(
            @RequestParam String startDate,
            @RequestParam String endDate,
            @RequestParam(required = false) TransactionType type,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            Authentication authentication
    ) {
        CustomUserDetails user = (CustomUserDetails) authentication.getPrincipal();
        return transactionService.getTransactionHistory(startDate, endDate, type, page, size, user.getMemberId()
        );
    }
}
