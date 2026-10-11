package com.vyra.be_virtual_your_assets.controller;

import com.vyra.be_virtual_your_assets.constant.ApiPath;
import com.vyra.be_virtual_your_assets.dto.BaseResponse;
import com.vyra.be_virtual_your_assets.dto.dashboard.*;
import com.vyra.be_virtual_your_assets.service.DashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

import static com.vyra.be_virtual_your_assets.constant.ApiPath.*;

/**
 * Powers the portal's main analytics dashboard: KPI summary cards,
 * income/expense charts, category distribution, recent transactions,
 * and the latest registered members.
 */
@RestController
@RequiredArgsConstructor
@RequestMapping(ApiPath.V1_ADMIN_DASHBOARD)
public class DashboardController {

    private final DashboardService dashboardService;

    @GetMapping(DASHBOARD_SUMMARY)
    public BaseResponse<DashboardSummaryResponse> getSummary() {
        return dashboardService.getSummary();
    }

    @GetMapping(DASHBOARD_FINANCIAL_CHART)
    public BaseResponse<List<FinancialChartItemResponse>> getFinancialChart(
            @RequestParam(defaultValue = "6") int months
    ) {
        return dashboardService.getFinancialChart(months);
    }

    @GetMapping(DASHBOARD_CATEGORY_DISTRIBUTION)
    public BaseResponse<List<CategoryDistributionResponse>> getCategoryDistribution() {
        return dashboardService.getCategoryDistribution();
    }

    @GetMapping(DASHBOARD_RECENT_TRANSACTIONS)
    public BaseResponse<List<RecentTransactionResponse>> getRecentTransactions() {
        return dashboardService.getRecentTransactions();
    }

    @GetMapping(DASHBOARD_LATEST_MEMBERS)
    public BaseResponse<List<LatestMemberResponse>> getLatestMembers() {
        return dashboardService.getLatestMembers();
    }
}
