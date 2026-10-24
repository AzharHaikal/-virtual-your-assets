package com.vyra.be_virtual_your_assets.controller;

import com.vyra.be_virtual_your_assets.constant.ApiPath;
import com.vyra.be_virtual_your_assets.constant.transaction.TransactionType;
import com.vyra.be_virtual_your_assets.dto.BaseResponse;
import com.vyra.be_virtual_your_assets.dto.common.PageResponse;
import com.vyra.be_virtual_your_assets.dto.export.ExportFile;
import com.vyra.be_virtual_your_assets.dto.transaction.TransactionHistoryResponse;
import com.vyra.be_virtual_your_assets.dto.transaction.TransactionMutationResponse;
import com.vyra.be_virtual_your_assets.dto.transaction.UpdateTransactionRequest;
import com.vyra.be_virtual_your_assets.security.model.CustomUserDetails;
import com.vyra.be_virtual_your_assets.service.FinanceService;
import com.vyra.be_virtual_your_assets.service.TransactionExportService;
import com.vyra.be_virtual_your_assets.service.TransactionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

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
    private final FinanceService financeService;
    private final TransactionExportService transactionExportService;

    /**
     * Member's transactions with optional filters: type, categoryId, from / to (local dates, inclusive),
     * q (description or reference), sort = date|amount[,asc|desc] (default date,desc).
     */
    @GetMapping
    public BaseResponse<PageResponse<TransactionHistoryResponse>> listTransactions(
            @RequestParam(required = false) TransactionType type,
            @RequestParam(required = false) String categoryId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(required = false) String q,
            @RequestParam(required = false) String sort,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            Authentication authentication
    ) {
        CustomUserDetails user = (CustomUserDetails) authentication.getPrincipal();
        return financeService.listTransactions(user.getMemberId(), type, categoryId, from, to, q, sort, page, size);
    }

    /** CSV of the SUCCESS transactions of [from, to] (local dates; default the current month), at most 366 days. */
    @GetMapping(ApiPath.TRANSACTIONS_EXPORT)
    public ResponseEntity<byte[]> exportTransactions(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            Authentication authentication
    ) {
        CustomUserDetails user = (CustomUserDetails) authentication.getPrincipal();
        return download(transactionExportService.exportCsv(user.getMemberId(), from, to));
    }

    static ResponseEntity<byte[]> download(ExportFile file) {
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.attachment().filename(file.fileName()).build().toString())
                .contentType(MediaType.parseMediaType(file.contentType()))
                .body(file.content());
    }

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
