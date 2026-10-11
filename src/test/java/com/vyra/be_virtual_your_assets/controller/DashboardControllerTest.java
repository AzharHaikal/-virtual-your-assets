package com.vyra.be_virtual_your_assets.controller;

import com.vyra.be_virtual_your_assets.dto.BaseResponse;
import com.vyra.be_virtual_your_assets.dto.dashboard.*;
import com.vyra.be_virtual_your_assets.service.DashboardService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DashboardControllerTest {

    @Mock
    private DashboardService dashboardService;

    @InjectMocks
    private DashboardController dashboardController;

    @Test
    void getSummary_shouldDelegateToService() {
        BaseResponse<DashboardSummaryResponse> expected = new BaseResponse<>("VYRA-DSH-SUM-000", "Success", new DashboardSummaryResponse());
        when(dashboardService.getSummary()).thenReturn(expected);

        BaseResponse<DashboardSummaryResponse> result = dashboardController.getSummary();

        assertThat(result).isSameAs(expected);
        verify(dashboardService).getSummary();
    }

    @Test
    void getFinancialChart_shouldDelegateToServiceWithMonthsParam() {
        BaseResponse<List<FinancialChartItemResponse>> expected = new BaseResponse<>("VYRA-DSH-CHT-000", "Success", List.of());
        when(dashboardService.getFinancialChart(12)).thenReturn(expected);

        BaseResponse<List<FinancialChartItemResponse>> result = dashboardController.getFinancialChart(12);

        assertThat(result).isSameAs(expected);
        verify(dashboardService).getFinancialChart(12);
    }

    @Test
    void getCategoryDistribution_shouldDelegateToService() {
        BaseResponse<List<CategoryDistributionResponse>> expected = new BaseResponse<>("VYRA-DSH-DST-000", "Success", List.of());
        when(dashboardService.getCategoryDistribution()).thenReturn(expected);

        BaseResponse<List<CategoryDistributionResponse>> result = dashboardController.getCategoryDistribution();

        assertThat(result).isSameAs(expected);
        verify(dashboardService).getCategoryDistribution();
    }

    @Test
    void getRecentTransactions_shouldDelegateToService() {
        BaseResponse<List<RecentTransactionResponse>> expected = new BaseResponse<>("VYRA-DSH-TRX-000", "Success", List.of());
        when(dashboardService.getRecentTransactions()).thenReturn(expected);

        BaseResponse<List<RecentTransactionResponse>> result = dashboardController.getRecentTransactions();

        assertThat(result).isSameAs(expected);
        verify(dashboardService).getRecentTransactions();
    }

    @Test
    void getLatestMembers_shouldDelegateToService() {
        BaseResponse<List<LatestMemberResponse>> expected = new BaseResponse<>("VYRA-DSH-MBR-000", "Success", List.of());
        when(dashboardService.getLatestMembers()).thenReturn(expected);

        BaseResponse<List<LatestMemberResponse>> result = dashboardController.getLatestMembers();

        assertThat(result).isSameAs(expected);
        verify(dashboardService).getLatestMembers();
    }
}
