package com.vyra.be_virtual_your_assets.controller;

import com.vyra.be_virtual_your_assets.constant.transaction.TransactionType;
import com.vyra.be_virtual_your_assets.dto.BaseResponse;
import com.vyra.be_virtual_your_assets.dto.common.PageResponse;
import com.vyra.be_virtual_your_assets.dto.report.IncomeExpenseReportResponse;
import com.vyra.be_virtual_your_assets.dto.report.MemberReportItemResponse;
import com.vyra.be_virtual_your_assets.dto.report.TransactionReportItemResponse;
import com.vyra.be_virtual_your_assets.service.ReportService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReportControllerTest {

    @Mock
    private ReportService reportService;

    @InjectMocks
    private ReportController reportController;

    private final LocalDate START = LocalDate.of(2026, 6, 1);
    private final LocalDate END = LocalDate.of(2026, 6, 18);

    @Test
    void getIncomeReport_shouldDelegateWithIncomeType() {
        BaseResponse<List<IncomeExpenseReportResponse>> expected = new BaseResponse<>("VYRA-RPT-INC-000", "Success", List.of());
        when(reportService.getIncomeExpenseReport(START, END, TransactionType.INCOME)).thenReturn(expected);

        BaseResponse<List<IncomeExpenseReportResponse>> result = reportController.getIncomeReport(START, END);

        assertThat(result).isSameAs(expected);
        verify(reportService).getIncomeExpenseReport(START, END, TransactionType.INCOME);
    }

    @Test
    void getExpenseReport_shouldDelegateWithExpenseType() {
        BaseResponse<List<IncomeExpenseReportResponse>> expected = new BaseResponse<>("VYRA-RPT-EXP-000", "Success", List.of());
        when(reportService.getIncomeExpenseReport(START, END, TransactionType.EXPENSE)).thenReturn(expected);

        BaseResponse<List<IncomeExpenseReportResponse>> result = reportController.getExpenseReport(START, END);

        assertThat(result).isSameAs(expected);
        verify(reportService).getIncomeExpenseReport(START, END, TransactionType.EXPENSE);
    }

    @Test
    void getTransactionReport_shouldDelegateWithAllParams() {
        BaseResponse<PageResponse<TransactionReportItemResponse>> expected = new BaseResponse<>("VYRA-RPT-TRX-000", "Success", null);
        when(reportService.getTransactionReport(START, END, TransactionType.INCOME, null, "REF", 0, 10)).thenReturn(expected);

        BaseResponse<PageResponse<TransactionReportItemResponse>> result =
                reportController.getTransactionReport(START, END, TransactionType.INCOME, null, "REF", 0, 10);

        assertThat(result).isSameAs(expected);
        verify(reportService).getTransactionReport(START, END, TransactionType.INCOME, null, "REF", 0, 10);
    }

    @Test
    void getMemberReport_shouldDelegateToService() {
        BaseResponse<PageResponse<MemberReportItemResponse>> expected = new BaseResponse<>("VYRA-RPT-MBR-000", "Success", null);
        when(reportService.getMemberReport(0, 10)).thenReturn(expected);

        BaseResponse<PageResponse<MemberReportItemResponse>> result = reportController.getMemberReport(0, 10);

        assertThat(result).isSameAs(expected);
        verify(reportService).getMemberReport(0, 10);
    }
}
