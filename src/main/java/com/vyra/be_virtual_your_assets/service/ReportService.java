package com.vyra.be_virtual_your_assets.service;

import com.vyra.be_virtual_your_assets.constant.AdminActivityEvent;
import com.vyra.be_virtual_your_assets.constant.AdminPermission;
import com.vyra.be_virtual_your_assets.constant.ErrorConstant;
import com.vyra.be_virtual_your_assets.constant.transaction.TransactionStatus;
import com.vyra.be_virtual_your_assets.constant.transaction.TransactionType;
import com.vyra.be_virtual_your_assets.dto.BaseResponse;
import com.vyra.be_virtual_your_assets.dto.common.PageResponse;
import com.vyra.be_virtual_your_assets.dto.report.IncomeExpenseReportResponse;
import com.vyra.be_virtual_your_assets.dto.report.MemberReportItemResponse;
import com.vyra.be_virtual_your_assets.dto.report.TransactionReportItemResponse;
import com.vyra.be_virtual_your_assets.entity.Member;
import com.vyra.be_virtual_your_assets.entity.Transaction;
import com.vyra.be_virtual_your_assets.repository.MemberRepository;
import com.vyra.be_virtual_your_assets.repository.TransactionRepository;
import com.vyra.be_virtual_your_assets.repository.WalletStatementRepository;
import com.vyra.be_virtual_your_assets.security.model.CustomAdminDetails;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Generates report data for the portal Reports screen: income/expense
 * breakdown by day, a filterable transaction ledger, and a member report.
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class ReportService {

    private final TransactionRepository transactionRepository;
    private final MemberRepository memberRepository;
    private final WalletStatementRepository walletStatementRepository;
    private final AdminAuthorizationService adminAuthorizationService;
    private final AdminActivityService adminActivityService;

    private static final int MAX_PAGE_SIZE = 500;
    private static final List<TransactionType> ALL_TYPES = List.of(TransactionType.INCOME, TransactionType.EXPENSE);
    /** Inquiries and deleted rows are not real transactions. */
    private static final List<TransactionStatus> LEDGER_STATUSES =
            List.of(TransactionStatus.SUCCESS, TransactionStatus.FAILED, TransactionStatus.SUSPECT);

    /**
     * Daily income/expense totals for the period. Only SUCCESSFUL transactions count,
     * and the totals are computed in the database (exact for any range).
     * A null type returns both income and expense.
     */
    public BaseResponse<List<IncomeExpenseReportResponse>> getIncomeExpenseReport(LocalDate startDate, LocalDate endDate, TransactionType type) {
        adminAuthorizationService.requirePermission(AdminPermission.REPORT_VIEW);

        // period -> [income, expense]
        Map<String, BigDecimal[]> grouped = new LinkedHashMap<>();

        // Days are the members' local dates (transaction.local_date), both ends inclusive.
        if (!startDate.isAfter(endDate)) {
            if (type == null || type == TransactionType.INCOME) {
                accumulate(grouped, transactionRepository.sumPlatformAmountPerDay(TransactionType.INCOME.name(), startDate, endDate), 0);
            }
            if (type == null || type == TransactionType.EXPENSE) {
                accumulate(grouped, transactionRepository.sumPlatformAmountPerDay(TransactionType.EXPENSE.name(), startDate, endDate), 1);
            }
        }

        List<IncomeExpenseReportResponse> items = grouped.entrySet()
                .stream()
                .sorted(Map.Entry.comparingByKey())
                .map(e -> new IncomeExpenseReportResponse(
                        e.getKey(),
                        e.getValue()[0],
                        e.getValue()[1],
                        e.getValue()[0].subtract(e.getValue()[1])
                ))
                .collect(Collectors.toList());

        ErrorConstant successCode = type == TransactionType.EXPENSE
                ? ErrorConstant.REPORT_EXPENSE_SUCCESS
                : ErrorConstant.REPORT_INCOME_SUCCESS;

        return new BaseResponse<>(successCode.getCode(), successCode.getMessage(), items);
    }

    private static void accumulate(Map<String, BigDecimal[]> grouped, List<Object[]> rows, int index) {
        for (Object[] row : rows) {
            BigDecimal total = row[1] == null ? BigDecimal.ZERO : new BigDecimal(row[1].toString());
            BigDecimal[] bucket = grouped.computeIfAbsent(String.valueOf(row[0]),
                    k -> new BigDecimal[]{BigDecimal.ZERO, BigDecimal.ZERO});
            bucket[index] = bucket[index].add(total);
        }
    }

    public BaseResponse<PageResponse<TransactionReportItemResponse>> getTransactionReport(
            LocalDate startDate, LocalDate endDate, TransactionType type, String keyword, int page, int size
    ) {
        return getTransactionReport(startDate, endDate, type, null, keyword, page, size);
    }

    /**
     * Transaction ledger. The Income / Expense reports call this with the type and
     * status=SUCCESS; the general Transaction report passes no status and gets every
     * real transaction (success, failed, suspect).
     */
    public BaseResponse<PageResponse<TransactionReportItemResponse>> getTransactionReport(
            LocalDate startDate, LocalDate endDate, TransactionType type, TransactionStatus status,
            String keyword, int page, int size
    ) {
        adminAuthorizationService.requirePermission(AdminPermission.REPORT_VIEW);
        // One row per member transaction: member-level data. Aggregates stay on getIncomeExpenseReport.
        adminAuthorizationService.requireMemberTransactionAccess();
        if (page <= 0) {
            CustomAdminDetails currentAdmin = adminAuthorizationService.getCurrentAdmin();
            adminActivityService.createAdminActivity(currentAdmin.getAdminId(), null,
                    AdminActivityEvent.VIEW_MEMBER_TRANSACTIONS, "transaction report " + startDate + " to " + endDate, null, null);
        }

        String pattern = StringUtils.isBlank(keyword) ? "%" : "%" + keyword.trim().toLowerCase(Locale.ROOT) + "%";

        Page<Transaction> transactionPage = transactionRepository.findPlatformTransactions(
                startDate, endDate,
                type != null ? List.of(type) : ALL_TYPES,
                status != null ? List.of(status) : LEDGER_STATUSES,
                pattern,
                PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), MAX_PAGE_SIZE))
        );

        Map<String, String> memberNames = resolveMemberNames(transactionPage.getContent());
        List<TransactionReportItemResponse> items = transactionPage.getContent()
                .stream()
                .map(t -> toTransactionReportItem(t, memberNames.get(t.getMemberId())))
                .collect(Collectors.toList());

        return new BaseResponse<>(
                ErrorConstant.REPORT_TRANSACTION_SUCCESS.getCode(),
                ErrorConstant.REPORT_TRANSACTION_SUCCESS.getMessage(),
                PageResponse.from(transactionPage, items)
        );
    }

    public BaseResponse<PageResponse<MemberReportItemResponse>> getMemberReport(int page, int size) {
        adminAuthorizationService.requirePermission(AdminPermission.REPORT_VIEW);

        Page<Member> memberPage = memberRepository.searchMembers("", null, PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), MAX_PAGE_SIZE)));

        List<Member> members = memberPage.getContent();
        // Balances are member-level financial data: left null unless the admin may see them.
        Map<String, BigDecimal> balances = new HashMap<>();
        boolean showBalance = adminAuthorizationService.canViewMemberTransactions();
        if (showBalance) {
            walletStatementRepository.findByPhoneNumberIn(
                    members.stream().map(Member::getPhoneNumber).collect(Collectors.toSet())
            ).forEach(ws -> balances.put(ws.getPhoneNumber(), ws.getBalance()));
        }

        Map<String, Long> transactionCounts = new HashMap<>();
        if (!members.isEmpty()) {
            for (Object[] row : transactionRepository.countByMemberIds(
                    members.stream().map(Member::getId).collect(Collectors.toSet()))) {
                transactionCounts.put(String.valueOf(row[0]), ((Number) row[1]).longValue());
            }
        }

        List<MemberReportItemResponse> items = members.stream()
                .map(m -> toMemberReportItem(m, showBalance ? balances : null, transactionCounts))
                .collect(Collectors.toList());

        return new BaseResponse<>(
                ErrorConstant.REPORT_MEMBER_SUCCESS.getCode(),
                ErrorConstant.REPORT_MEMBER_SUCCESS.getMessage(),
                PageResponse.from(memberPage, items)
        );
    }

    /** One query for all members on the page instead of one per row. */
    private Map<String, String> resolveMemberNames(List<Transaction> transactions) {
        Set<String> ids = transactions.stream()
                .map(Transaction::getMemberId)
                .filter(StringUtils::isNotBlank)
                .collect(Collectors.toSet());
        Map<String, String> names = new HashMap<>();
        if (ids.isEmpty()) {
            return names;
        }
        for (Member member : memberRepository.findByIdIn(ids)) {
            String fullName = (StringUtils.defaultString(member.getFirstName()) + " "
                    + StringUtils.defaultString(member.getLastName())).trim();
            names.put(member.getId(), fullName);
        }
        return names;
    }

    private TransactionReportItemResponse toTransactionReportItem(Transaction transaction, String memberName) {
        TransactionReportItemResponse response = new TransactionReportItemResponse();
        response.setTransactionId(transaction.getId());
        response.setMemberName(memberName);
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

    private MemberReportItemResponse toMemberReportItem(
            Member member, Map<String, BigDecimal> balances, Map<String, Long> transactionCounts
    ) {
        MemberReportItemResponse response = new MemberReportItemResponse();
        response.setMemberId(member.getId());
        response.setFullName(member.getFirstName() + " " + member.getLastName());
        response.setEmail(member.getEmail());
        response.setStatus(member.getStatus());
        response.setJoinDate(member.getCreatedAt());
        response.setBalance(balances == null ? null : balances.getOrDefault(member.getPhoneNumber(), BigDecimal.ZERO));
        response.setTotalTransactions(transactionCounts.getOrDefault(member.getId(), 0L));
        return response;
    }
}
