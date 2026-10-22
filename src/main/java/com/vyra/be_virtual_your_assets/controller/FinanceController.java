package com.vyra.be_virtual_your_assets.controller;

import com.vyra.be_virtual_your_assets.constant.ApiPath;
import com.vyra.be_virtual_your_assets.dto.BaseResponse;
import com.vyra.be_virtual_your_assets.dto.finance.FinanceSummaryResponse;
import com.vyra.be_virtual_your_assets.security.model.CustomUserDetails;
import com.vyra.be_virtual_your_assets.service.FinanceService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

import static com.vyra.be_virtual_your_assets.constant.ApiPath.FINANCE_SUMMARY;

/** Member's finance overview (Keuangan > Ringkasan). */
@RestController
@RequiredArgsConstructor
@RequestMapping(ApiPath.V1_FINANCE)
public class FinanceController {

    private final FinanceService financeService;

    /** from / to are the member's own dates (inclusive); default = the current month. */
    @GetMapping(FINANCE_SUMMARY)
    public BaseResponse<FinanceSummaryResponse> getSummary(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            Authentication authentication
    ) {
        CustomUserDetails user = (CustomUserDetails) authentication.getPrincipal();
        return financeService.getSummary(user.getMemberId(), from, to);
    }
}
