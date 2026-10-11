package com.vyra.be_virtual_your_assets.controller;

import com.vyra.be_virtual_your_assets.constant.ApiPath;
import com.vyra.be_virtual_your_assets.constant.transaction.TransactionStatus;
import com.vyra.be_virtual_your_assets.constant.transaction.TransactionType;
import com.vyra.be_virtual_your_assets.dto.BaseResponse;
import com.vyra.be_virtual_your_assets.dto.common.PageResponse;
import com.vyra.be_virtual_your_assets.dto.report.IncomeExpenseReportResponse;
import com.vyra.be_virtual_your_assets.dto.report.MemberReportItemResponse;
import com.vyra.be_virtual_your_assets.dto.report.TransactionReportItemResponse;
import com.vyra.be_virtual_your_assets.service.ReportService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

import static com.vyra.be_virtual_your_assets.constant.ApiPath.*;

/**
 * Reporting endpoints for the portal: income/expense summaries by period,
 * a filterable transaction report, and a member growth/financial report.
 */
@RestController
@RequiredArgsConstructor
@RequestMapping(ApiPath.V1_ADMIN_REPORT)
public class ReportController {

    private final ReportService reportService;

    @GetMapping(REPORT_INCOME)
    public BaseResponse<List<IncomeExpenseReportResponse>> getIncomeReport(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate
    ) {
        return reportService.getIncomeExpenseReport(startDate, endDate, TransactionType.INCOME);
    }

    @GetMapping(REPORT_EXPENSE)
    public BaseResponse<List<IncomeExpenseReportResponse>> getExpenseReport(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate
    ) {
        return reportService.getIncomeExpenseReport(startDate, endDate, TransactionType.EXPENSE);
    }

    @GetMapping(REPORT_TRANSACTION)
    public BaseResponse<PageResponse<TransactionReportItemResponse>> getTransactionReport(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(required = false) TransactionType type,
            @RequestParam(required = false) TransactionStatus status,
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        return reportService.getTransactionReport(startDate, endDate, type, status, keyword, page, size);
    }

    @GetMapping(REPORT_MEMBER)
    public BaseResponse<PageResponse<MemberReportItemResponse>> getMemberReport(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        return reportService.getMemberReport(page, size);
    }
}
