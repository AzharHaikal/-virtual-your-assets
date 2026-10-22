package com.vyra.be_virtual_your_assets.service;

import com.vyra.be_virtual_your_assets.constant.ActiveStatus;
import com.vyra.be_virtual_your_assets.constant.AdminPermission;
import com.vyra.be_virtual_your_assets.constant.ErrorConstant;
import com.vyra.be_virtual_your_assets.constant.transaction.TransactionStatus;
import com.vyra.be_virtual_your_assets.constant.transaction.TransactionType;
import com.vyra.be_virtual_your_assets.dto.BaseResponse;
import com.vyra.be_virtual_your_assets.dto.dashboard.*;
import com.vyra.be_virtual_your_assets.entity.Member;
import com.vyra.be_virtual_your_assets.entity.Transaction;
import com.vyra.be_virtual_your_assets.exception.ForbiddenException;
import com.vyra.be_virtual_your_assets.repository.MemberRepository;
import com.vyra.be_virtual_your_assets.repository.TransactionRepository;
import com.vyra.be_virtual_your_assets.repository.WalletStatementRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DashboardServiceTest {

    @Mock private MemberRepository memberRepository;
    @Mock private TransactionRepository transactionRepository;
    @Mock private WalletStatementRepository walletStatementRepository;
    @Mock private AdminAuthorizationService adminAuthorizationService;

    @InjectMocks
    private DashboardService dashboardService;

    @BeforeEach
    void setUp() {
        lenient().doNothing().when(adminAuthorizationService).requirePermission(AdminPermission.DASHBOARD_VIEW);
    }

    // =========================================================================
    // getSummary
    // =========================================================================
    @Test
    void getSummary_shouldAggregateAllKpisCorrectly() {
        when(walletStatementRepository.findAllActiveBalances())
                .thenReturn(List.of(new BigDecimal("100000"), new BigDecimal("250000")));
        when(walletStatementRepository.sumTotalIncome()).thenReturn(new BigDecimal("500000"));
        when(walletStatementRepository.sumTotalExpense()).thenReturn(new BigDecimal("150000"));
        when(memberRepository.count()).thenReturn(42L);
        when(memberRepository.countByStatus(ActiveStatus.ACTIVE)).thenReturn(38L);
        when(memberRepository.countByCreatedAtBetween(any(), any())).thenReturn(3L);
        when(transactionRepository.countByDeletedAtIsNull()).thenReturn(120L);

        BaseResponse<DashboardSummaryResponse> result = dashboardService.getSummary();

        assertThat(result.getResponseStatus()).isEqualTo(ErrorConstant.DASHBOARD_SUMMARY_SUCCESS.getCode());
        DashboardSummaryResponse data = result.getData();
        assertThat(data.getTotalBalance()).isEqualTo(new BigDecimal("350000"));
        assertThat(data.getTotalIncome()).isEqualTo(new BigDecimal("500000"));
        assertThat(data.getTotalExpense()).isEqualTo(new BigDecimal("150000"));
        assertThat(data.getTotalMembers()).isEqualTo(42L);
        assertThat(data.getActiveMembers()).isEqualTo(38L);
        assertThat(data.getNewMembersToday()).isEqualTo(3L);
        assertThat(data.getTotalTransactions()).isEqualTo(120L);
    }

    @Test
    void getSummary_emptyWalletStatements_shouldReturnZeroBalance() {
        when(walletStatementRepository.findAllActiveBalances()).thenReturn(List.of());
        when(walletStatementRepository.sumTotalIncome()).thenReturn(BigDecimal.ZERO);
        when(walletStatementRepository.sumTotalExpense()).thenReturn(BigDecimal.ZERO);
        when(memberRepository.count()).thenReturn(0L);
        when(memberRepository.countByStatus(ActiveStatus.ACTIVE)).thenReturn(0L);
        when(memberRepository.countByCreatedAtBetween(any(), any())).thenReturn(0L);
        when(transactionRepository.countByDeletedAtIsNull()).thenReturn(0L);

        BaseResponse<DashboardSummaryResponse> result = dashboardService.getSummary();

        assertThat(result.getData().getTotalBalance()).isEqualTo(BigDecimal.ZERO);
    }

    @Test
    void getSummary_noPermission_shouldThrowForbiddenException() {
        doThrow(new ForbiddenException(ErrorConstant.ADMIN_UNAUTHORIZED))
                .when(adminAuthorizationService).requirePermission(AdminPermission.DASHBOARD_VIEW);

        assertThrows(ForbiddenException.class, dashboardService::getSummary);
        verify(walletStatementRepository, never()).findAllActiveBalances();
    }

    // =========================================================================
    // getFinancialChart
    // =========================================================================
    @Test
    void getFinancialChart_shouldMapNativeQueryRowsToChartItems() {
        Object[] row1 = {Timestamp.valueOf(LocalDateTime.of(2026, 1, 1, 0, 0)), new BigDecimal("1000"), new BigDecimal("400")};
        Object[] row2 = {Timestamp.valueOf(LocalDateTime.of(2026, 2, 1, 0, 0)), new BigDecimal("2000"), new BigDecimal("800")};

        when(transactionRepository.getPlatformMonthlyChart(any())).thenReturn(List.of(row1, row2));

        BaseResponse<List<FinancialChartItemResponse>> result = dashboardService.getFinancialChart(6);

        assertThat(result.getResponseStatus()).isEqualTo(ErrorConstant.DASHBOARD_CHART_SUCCESS.getCode());
        assertThat(result.getData()).hasSize(2);
        assertThat(result.getData().get(0).getLabel()).isEqualTo("2026-01");
        assertThat(result.getData().get(0).getIncome()).isEqualTo(new BigDecimal("1000"));
        assertThat(result.getData().get(1).getExpense()).isEqualTo(new BigDecimal("800"));
    }

    @Test
    void getFinancialChart_zeroOrNegativeMonths_shouldDefaultToSixMonths() {
        when(transactionRepository.getPlatformMonthlyChart(any())).thenReturn(List.of());

        dashboardService.getFinancialChart(0);
        dashboardService.getFinancialChart(-3);

        verify(transactionRepository, times(2)).getPlatformMonthlyChart(any());
    }

    @Test
    void getFinancialChart_nullAmountValues_shouldDefaultToZero() {
        Object[] row = {Timestamp.valueOf(LocalDateTime.of(2026, 3, 1, 0, 0)), null, null};

        when(transactionRepository.getPlatformMonthlyChart(any())).thenReturn(Collections.singletonList(row));

        BaseResponse<List<FinancialChartItemResponse>> result = dashboardService.getFinancialChart(3);

        assertThat(result.getData().get(0).getIncome()).isEqualTo(BigDecimal.ZERO);
        assertThat(result.getData().get(0).getExpense()).isEqualTo(BigDecimal.ZERO);
    }

    // =========================================================================
    // getCategoryDistribution
    // =========================================================================
    @Test
    void getCategoryDistribution_shouldMapRowsToResponses() {
        Object[] row1 = {"FOOD_AND_BEVERAGE", new BigDecimal("75000")};
        Object[] row2 = {"TRANSPORTATION", new BigDecimal("30000")};

        when(transactionRepository.getCategoryDistribution()).thenReturn(List.of(row1, row2));

        BaseResponse<List<CategoryDistributionResponse>> result = dashboardService.getCategoryDistribution();

        assertThat(result.getResponseStatus()).isEqualTo(ErrorConstant.DASHBOARD_DISTRIBUTION_SUCCESS.getCode());
        assertThat(result.getData()).hasSize(2);
        assertThat(result.getData().get(0).getCategory()).isEqualTo("FOOD_AND_BEVERAGE");
        assertThat(result.getData().get(0).getTotalAmount()).isEqualTo(new BigDecimal("75000"));
    }

    // =========================================================================
    // getRecentTransactions
    // =========================================================================
    @Test
    void getRecentTransactions_shouldMapTransactionsToResponses() {
        Transaction transaction = buildTransaction();
        when(transactionRepository.findRecentTransactions()).thenReturn(List.of(transaction));

        BaseResponse<List<RecentTransactionResponse>> result = dashboardService.getRecentTransactions();

        assertThat(result.getResponseStatus()).isEqualTo(ErrorConstant.DASHBOARD_RECENT_TRANSACTIONS_SUCCESS.getCode());
        assertThat(result.getData()).hasSize(1);
        RecentTransactionResponse item = result.getData().get(0);
        assertThat(item.getTransactionId()).isEqualTo("trx-001");
        assertThat(item.getMemberEmail()).isEqualTo("budi@example.com");
        assertThat(item.getType()).isEqualTo(TransactionType.INCOME);
    }

    @Test
    void getRecentTransactions_withoutTransactionAccess_shouldThrowForbiddenAndNotQuery() {
        doThrow(new ForbiddenException(ErrorConstant.MEMBER_TRANSACTION_ACCESS_DISABLED))
                .when(adminAuthorizationService).requireMemberTransactionAccess();

        assertThrows(ForbiddenException.class, () -> dashboardService.getRecentTransactions());

        verify(transactionRepository, never()).findRecentTransactions();
    }

    // =========================================================================
    // getLatestMembers
    // =========================================================================
    @Test
    void getLatestMembers_shouldMapMembersToResponses() {
        Member member = new Member();
        member.setId("member-001");
        member.setFirstName("Budi");
        member.setLastName("Santoso");
        member.setEmail("budi@example.com");
        member.setPhoneNumber("628123456789");
        member.setStatus(ActiveStatus.ACTIVE);
        member.setCreatedAt(LocalDateTime.now());

        when(memberRepository.findTop5ByOrderByCreatedAtDesc()).thenReturn(List.of(member));

        BaseResponse<List<LatestMemberResponse>> result = dashboardService.getLatestMembers();

        assertThat(result.getResponseStatus()).isEqualTo(ErrorConstant.DASHBOARD_LATEST_MEMBERS_SUCCESS.getCode());
        assertThat(result.getData().get(0).getFullName()).isEqualTo("Budi Santoso");
    }

    // =========================================================================
    // helpers
    // =========================================================================
    private Transaction buildTransaction() {
        Transaction transaction = new Transaction();
        transaction.setId("trx-001");
        transaction.setReferenceNumber("REF-001");
        transaction.setUserEmail("budi@example.com");
        transaction.setUserPhoneNumber("628123456789");
        transaction.setTransactionCategory("SALARY");
        transaction.setTransactionType(TransactionType.INCOME);
        transaction.setStatus(TransactionStatus.SUCCESS);
        transaction.setAmount(new BigDecimal("1000000"));
        transaction.setTransactionDate(LocalDateTime.now());
        return transaction;
    }


    @Test
    void getFinancialChart_shouldFormatTimestampLabelsAndCoerceNumericTypes() {
        when(transactionRepository.getPlatformMonthlyChart(any())).thenReturn(List.of(
                new Object[]{java.sql.Timestamp.valueOf("2026-05-01 00:00:00"), new BigDecimal("1000"), 250L},
                new Object[]{"2026-06", null, new BigDecimal("40")}
        ));

        BaseResponse<List<FinancialChartItemResponse>> result = dashboardService.getFinancialChart(3);

        assertThat(result.getData()).hasSize(2);
        assertThat(result.getData().get(0).getLabel()).isEqualTo("2026-05");
        assertThat(result.getData().get(0).getIncome()).isEqualTo(new BigDecimal("1000"));
        assertThat(result.getData().get(0).getExpense()).isEqualByComparingTo("250");
        assertThat(result.getData().get(1).getLabel()).isEqualTo("2026-06");
        assertThat(result.getData().get(1).getIncome()).isEqualTo(BigDecimal.ZERO);
    }
}
