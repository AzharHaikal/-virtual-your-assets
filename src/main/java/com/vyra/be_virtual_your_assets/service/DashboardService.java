package com.vyra.be_virtual_your_assets.service;

import com.vyra.be_virtual_your_assets.constant.ActiveStatus;
import com.vyra.be_virtual_your_assets.constant.AdminPermission;
import com.vyra.be_virtual_your_assets.constant.ErrorConstant;
import com.vyra.be_virtual_your_assets.dto.BaseResponse;
import com.vyra.be_virtual_your_assets.dto.dashboard.*;
import com.vyra.be_virtual_your_assets.entity.Member;
import com.vyra.be_virtual_your_assets.entity.Transaction;
import com.vyra.be_virtual_your_assets.repository.MemberRepository;
import com.vyra.be_virtual_your_assets.repository.TransactionRepository;
import com.vyra.be_virtual_your_assets.repository.WalletStatementRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Aggregates platform-wide analytics for the portal dashboard. Reads from
 * Member, Transaction, and WalletStatement, but never writes to them.
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class DashboardService {

    private final MemberRepository memberRepository;
    private final TransactionRepository transactionRepository;
    private final WalletStatementRepository walletStatementRepository;
    private final AdminAuthorizationService adminAuthorizationService;

    public BaseResponse<DashboardSummaryResponse> getSummary() {
        adminAuthorizationService.requirePermission(AdminPermission.DASHBOARD_VIEW);

        DashboardSummaryResponse response = new DashboardSummaryResponse();

        // balance is encrypted at the DB level, so it must be summed in Java after decryption.
        BigDecimal totalBalance = walletStatementRepository.findAllActiveBalances()
                .stream()
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        response.setTotalBalance(totalBalance);
        response.setTotalIncome(walletStatementRepository.sumTotalIncome());
        response.setTotalExpense(walletStatementRepository.sumTotalExpense());
        response.setTotalMembers(memberRepository.count());
        response.setActiveMembers(memberRepository.countByStatus(ActiveStatus.ACTIVE));

        LocalDateTime startOfToday = LocalDate.now().atStartOfDay();
        LocalDateTime endOfToday = LocalDate.now().atTime(LocalTime.MAX);
        response.setNewMembersToday(memberRepository.countByCreatedAtBetween(startOfToday, endOfToday));

        response.setTotalTransactions(transactionRepository.countByDeletedAtIsNull());

        return new BaseResponse<>(
                ErrorConstant.DASHBOARD_SUMMARY_SUCCESS.getCode(),
                ErrorConstant.DASHBOARD_SUMMARY_SUCCESS.getMessage(),
                response
        );
    }

    public BaseResponse<List<FinancialChartItemResponse>> getFinancialChart(int months) {
        adminAuthorizationService.requirePermission(AdminPermission.DASHBOARD_VIEW);

        int lookbackMonths = months <= 0 ? 6 : months;
        LocalDateTime startDate = LocalDate.now()
                .minusMonths(lookbackMonths - 1L)
                .withDayOfMonth(1)
                .atStartOfDay();

        List<Object[]> rows = transactionRepository.getPlatformMonthlyChart(startDate);
        DateTimeFormatter labelFormatter = DateTimeFormatter.ofPattern("yyyy-MM");

        List<FinancialChartItemResponse> items = rows.stream()
                .map(row -> {
                    String label = row[0] instanceof java.sql.Timestamp ts
                            ? ts.toLocalDateTime().format(labelFormatter)
                            : String.valueOf(row[0]);
                    BigDecimal income = toBigDecimal(row[1]);
                    BigDecimal expense = toBigDecimal(row[2]);
                    return new FinancialChartItemResponse(label, income, expense);
                })
                .collect(Collectors.toList());

        return new BaseResponse<>(
                ErrorConstant.DASHBOARD_CHART_SUCCESS.getCode(),
                ErrorConstant.DASHBOARD_CHART_SUCCESS.getMessage(),
                items
        );
    }

    public BaseResponse<List<CategoryDistributionResponse>> getCategoryDistribution() {
        adminAuthorizationService.requirePermission(AdminPermission.DASHBOARD_VIEW);

        List<Object[]> rows = transactionRepository.getCategoryDistribution();

        List<CategoryDistributionResponse> items = rows.stream()
                .map(row -> new CategoryDistributionResponse(String.valueOf(row[0]), toBigDecimal(row[1])))
                .collect(Collectors.toList());

        return new BaseResponse<>(
                ErrorConstant.DASHBOARD_DISTRIBUTION_SUCCESS.getCode(),
                ErrorConstant.DASHBOARD_DISTRIBUTION_SUCCESS.getMessage(),
                items
        );
    }

    public BaseResponse<List<RecentTransactionResponse>> getRecentTransactions() {
        adminAuthorizationService.requirePermission(AdminPermission.DASHBOARD_VIEW);
        // Lists individual member transactions, not aggregates.
        adminAuthorizationService.requireMemberTransactionAccess();

        List<RecentTransactionResponse> items = transactionRepository.findRecentTransactions()
                .stream()
                .map(this::toRecentTransactionResponse)
                .collect(Collectors.toList());

        return new BaseResponse<>(
                ErrorConstant.DASHBOARD_RECENT_TRANSACTIONS_SUCCESS.getCode(),
                ErrorConstant.DASHBOARD_RECENT_TRANSACTIONS_SUCCESS.getMessage(),
                items
        );
    }

    public BaseResponse<List<LatestMemberResponse>> getLatestMembers() {
        adminAuthorizationService.requirePermission(AdminPermission.DASHBOARD_VIEW);

        List<LatestMemberResponse> items = memberRepository.findTop5ByOrderByCreatedAtDesc()
                .stream()
                .map(this::toLatestMemberResponse)
                .collect(Collectors.toList());

        return new BaseResponse<>(
                ErrorConstant.DASHBOARD_LATEST_MEMBERS_SUCCESS.getCode(),
                ErrorConstant.DASHBOARD_LATEST_MEMBERS_SUCCESS.getMessage(),
                items
        );
    }

    private RecentTransactionResponse toRecentTransactionResponse(Transaction transaction) {
        RecentTransactionResponse response = new RecentTransactionResponse();
        response.setTransactionId(transaction.getId());
        response.setReferenceNumber(transaction.getReferenceNumber());
        response.setMemberEmail(transaction.getUserEmail());
        response.setMemberPhoneNumber(transaction.getUserPhoneNumber());
        response.setCategory(transaction.getTransactionCategory());
        response.setType(transaction.getTransactionType());
        response.setStatus(transaction.getStatus());
        response.setAmount(transaction.getAmount());
        response.setTransactionDate(transaction.getTransactionDate());
        return response;
    }

    private LatestMemberResponse toLatestMemberResponse(Member member) {
        LatestMemberResponse response = new LatestMemberResponse();
        response.setMemberId(member.getId());
        response.setFullName(member.getFirstName() + " " + member.getLastName());
        response.setEmail(member.getEmail());
        response.setPhoneNumber(member.getPhoneNumber());
        response.setStatus(member.getStatus());
        response.setJoinDate(member.getCreatedAt());
        return response;
    }

    private BigDecimal toBigDecimal(Object value) {
        if (value == null) return BigDecimal.ZERO;
        if (value instanceof BigDecimal bd) return bd;
        return new BigDecimal(value.toString());
    }
}
