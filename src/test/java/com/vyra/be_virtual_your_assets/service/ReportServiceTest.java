package com.vyra.be_virtual_your_assets.service;

import com.vyra.be_virtual_your_assets.constant.ActiveStatus;
import com.vyra.be_virtual_your_assets.constant.AdminActivityEvent;
import com.vyra.be_virtual_your_assets.constant.AdminPermission;
import com.vyra.be_virtual_your_assets.constant.AdminRole;
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
import com.vyra.be_virtual_your_assets.entity.WalletStatement;
import com.vyra.be_virtual_your_assets.exception.ForbiddenException;
import com.vyra.be_virtual_your_assets.repository.MemberRepository;
import com.vyra.be_virtual_your_assets.repository.TransactionRepository;
import com.vyra.be_virtual_your_assets.repository.WalletStatementRepository;
import com.vyra.be_virtual_your_assets.security.model.CustomAdminDetails;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReportServiceTest {

    @Mock private TransactionRepository transactionRepository;
    @Mock private MemberRepository memberRepository;
    @Mock private WalletStatementRepository walletStatementRepository;
    @Mock private AdminAuthorizationService adminAuthorizationService;
    @Mock private AdminActivityService adminActivityService;

    @InjectMocks
    private ReportService reportService;

    @BeforeEach
    void setUp() {
        doNothing().when(adminAuthorizationService).requirePermission(AdminPermission.REPORT_VIEW);
        // Default: a Super Admin with member transaction access switched on.
        lenient().when(adminAuthorizationService.getCurrentAdmin())
                .thenReturn(new CustomAdminDetails("super-admin-id", "token", AdminRole.SUPER_ADMIN));
        lenient().when(adminAuthorizationService.canViewMemberTransactions()).thenReturn(true);
    }

    // =========================================================================
    // getIncomeExpenseReport
    // =========================================================================
    @Test
    void getIncomeExpenseReport_income_shouldReturnDailyTotalsFromDatabase() {
        when(transactionRepository.sumPlatformAmountPerDay(eq("INCOME"), any(), any()))
                .thenReturn(List.of(
                        new Object[]{"2026-06-01", new BigDecimal("150000")},
                        new Object[]{"2026-06-02", new BigDecimal("200000")}
                ));

        BaseResponse<List<IncomeExpenseReportResponse>> result = reportService.getIncomeExpenseReport(
                LocalDate.of(2026, 6, 1), LocalDate.of(2026, 6, 2), TransactionType.INCOME
        );

        assertThat(result.getResponseStatus()).isEqualTo(ErrorConstant.REPORT_INCOME_SUCCESS.getCode());
        assertThat(result.getData()).hasSize(2);
        assertThat(result.getData().get(0).getPeriod()).isEqualTo("2026-06-01");
        assertThat(result.getData().get(0).getTotalIncome()).isEqualTo(new BigDecimal("150000"));
        assertThat(result.getData().get(0).getTotalExpense()).isEqualTo(BigDecimal.ZERO);
        assertThat(result.getData().get(1).getTotalIncome()).isEqualTo(new BigDecimal("200000"));
        verify(transactionRepository, never()).sumPlatformAmountPerDay(eq("EXPENSE"), any(), any());
    }

    @Test
    void getIncomeExpenseReport_shouldPassInclusiveLocalDateRange() {
        when(transactionRepository.sumPlatformAmountPerDay(any(), any(), any())).thenReturn(List.of());

        reportService.getIncomeExpenseReport(
                LocalDate.of(2026, 6, 1), LocalDate.of(2026, 6, 1), TransactionType.INCOME);

        verify(transactionRepository).sumPlatformAmountPerDay(
                "INCOME",
                LocalDate.of(2026, 6, 1),
                LocalDate.of(2026, 6, 1));
    }

    @Test
    void getIncomeExpenseReport_expense_shouldUseExpenseSuccessCode() {
        when(transactionRepository.sumPlatformAmountPerDay(eq("EXPENSE"), any(), any())).thenReturn(List.of());

        BaseResponse<List<IncomeExpenseReportResponse>> result = reportService.getIncomeExpenseReport(
                LocalDate.of(2026, 6, 1), LocalDate.of(2026, 6, 2), TransactionType.EXPENSE
        );

        assertThat(result.getResponseStatus()).isEqualTo(ErrorConstant.REPORT_EXPENSE_SUCCESS.getCode());
        assertThat(result.getData()).isEmpty();
    }

    @Test
    void getIncomeExpenseReport_noType_shouldMergeIncomeAndExpenseWithNet() {
        when(transactionRepository.sumPlatformAmountPerDay(eq("INCOME"), any(), any()))
                .thenReturn(List.<Object[]>of(new Object[]{"2026-06-01", new BigDecimal("300000")}));
        when(transactionRepository.sumPlatformAmountPerDay(eq("EXPENSE"), any(), any()))
                .thenReturn(List.<Object[]>of(new Object[]{"2026-06-01", new BigDecimal("120000")}));

        BaseResponse<List<IncomeExpenseReportResponse>> result = reportService.getIncomeExpenseReport(
                LocalDate.of(2026, 6, 1), LocalDate.of(2026, 6, 1), null
        );

        assertThat(result.getData()).hasSize(1);
        assertThat(result.getData().get(0).getTotalIncome()).isEqualTo(new BigDecimal("300000"));
        assertThat(result.getData().get(0).getTotalExpense()).isEqualTo(new BigDecimal("120000"));
        assertThat(result.getData().get(0).getNetAmount()).isEqualTo(new BigDecimal("180000"));
    }

    @Test
    void getIncomeExpenseReport_startAfterEnd_shouldReturnEmptyWithoutQuerying() {
        BaseResponse<List<IncomeExpenseReportResponse>> result = reportService.getIncomeExpenseReport(
                LocalDate.of(2026, 6, 5), LocalDate.of(2026, 6, 1), TransactionType.INCOME
        );

        assertThat(result.getData()).isEmpty();
        verify(transactionRepository, never()).sumPlatformAmountPerDay(any(), any(), any());
    }

    // =========================================================================
    // getTransactionReport
    // =========================================================================
    @Test
    void getTransactionReport_shouldReturnPagedTransactionItems() {
        Transaction transaction = buildTransaction(TransactionType.INCOME, new BigDecimal("100000"), LocalDateTime.now());
        Page<Transaction> page = new PageImpl<>(List.of(transaction), PageRequest.of(0, 10), 1);
        when(transactionRepository.findPlatformTransactions(
                any(), any(), eq(List.of(TransactionType.INCOME)), any(), eq("%ref%"), any(Pageable.class)))
                .thenReturn(page);

        BaseResponse<PageResponse<TransactionReportItemResponse>> result = reportService.getTransactionReport(
                LocalDate.of(2026, 6, 1), LocalDate.of(2026, 6, 18), TransactionType.INCOME, "REF", 0, 10
        );

        assertThat(result.getResponseStatus()).isEqualTo(ErrorConstant.REPORT_TRANSACTION_SUCCESS.getCode());
        assertThat(result.getData().getContent()).hasSize(1);
        assertThat(result.getData().getContent().get(0).getMemberEmail()).isEqualTo("budi@example.com");
    }

    @Test
    void getTransactionReport_noFilters_shouldUseBothTypesAndMatchAllKeywords() {
        when(transactionRepository.findPlatformTransactions(any(), any(), any(), any(), any(), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of()));

        reportService.getTransactionReport(LocalDate.of(2026, 6, 1), LocalDate.of(2026, 6, 1), null, "   ", 0, 10);

        verify(transactionRepository).findPlatformTransactions(
                eq(LocalDate.of(2026, 6, 1)),
                eq(LocalDate.of(2026, 6, 1)),
                eq(List.of(TransactionType.INCOME, TransactionType.EXPENSE)),
                eq(List.of(TransactionStatus.SUCCESS, TransactionStatus.FAILED, TransactionStatus.SUSPECT)),
                eq("%"),
                eq(PageRequest.of(0, 10)));
    }

    @Test
    void getTransactionReport_withStatus_shouldFilterOnlyThatStatusAndResolveMemberName() {
        Transaction transaction = buildTransaction(TransactionType.EXPENSE, new BigDecimal("25000"), LocalDateTime.now());
        transaction.setMemberId("member-id-001");
        Member member = new Member();
        member.setId("member-id-001");
        member.setFirstName("Budi");
        member.setLastName("Santoso");

        when(memberRepository.findByIdIn(any())).thenReturn(List.of(member));
        when(transactionRepository.findPlatformTransactions(
                any(), any(), eq(List.of(TransactionType.EXPENSE)), eq(List.of(TransactionStatus.SUCCESS)), any(), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(transaction), PageRequest.of(0, 10), 1));

        BaseResponse<PageResponse<TransactionReportItemResponse>> result = reportService.getTransactionReport(
                LocalDate.of(2026, 6, 1), LocalDate.of(2026, 6, 1),
                TransactionType.EXPENSE, TransactionStatus.SUCCESS, null, 0, 10);

        assertThat(result.getData().getContent()).hasSize(1);
        assertThat(result.getData().getContent().get(0).getMemberName()).isEqualTo("Budi Santoso");
    }

    @Test
    void getTransactionReport_withoutTransactionAccess_shouldThrowForbiddenAndNotQuery() {
        doThrow(new ForbiddenException(ErrorConstant.ADMIN_UNAUTHORIZED))
                .when(adminAuthorizationService).requireMemberTransactionAccess();

        assertThrows(ForbiddenException.class, () -> reportService.getTransactionReport(
                LocalDate.of(2026, 6, 1), LocalDate.of(2026, 6, 1), null, null, 0, 10));

        verifyNoInteractions(transactionRepository, adminActivityService);
    }

    @Test
    void getTransactionReport_auditsOnlyTheFirstPage() {
        when(transactionRepository.findPlatformTransactions(any(), any(), any(), any(), any(), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of()));

        reportService.getTransactionReport(LocalDate.of(2026, 6, 1), LocalDate.of(2026, 6, 2), null, null, 0, 10);
        reportService.getTransactionReport(LocalDate.of(2026, 6, 1), LocalDate.of(2026, 6, 2), null, null, 1, 10);

        verify(adminActivityService, times(1)).createAdminActivity("super-admin-id", null,
                AdminActivityEvent.VIEW_MEMBER_TRANSACTIONS, "transaction report 2026-06-01 to 2026-06-02", null, null);
    }

    @Test
    void getTransactionReport_oversizedPage_shouldBeClamped() {
        when(transactionRepository.findPlatformTransactions(any(), any(), any(), any(), any(), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of()));

        reportService.getTransactionReport(LocalDate.of(2026, 6, 1), LocalDate.of(2026, 6, 1), null, null, -1, 99999);

        verify(transactionRepository).findPlatformTransactions(
                any(), any(), any(), any(), any(), eq(PageRequest.of(0, 500)));
    }

    // =========================================================================
    // getMemberReport
    // =========================================================================
    @Test
    void getMemberReport_shouldReturnPagedMembersWithBalanceAndTransactionCount() {
        Member member = new Member();
        member.setId("member-id-001");
        member.setFirstName("Budi");
        member.setLastName("Santoso");
        member.setEmail("budi@example.com");
        member.setPhoneNumber("628123456789");
        member.setStatus(ActiveStatus.ACTIVE);
        member.setCreatedAt(LocalDateTime.now());

        Page<Member> memberPage = new PageImpl<>(List.of(member), PageRequest.of(0, 10), 1);
        when(memberRepository.searchMembers(eq(""), isNull(), any(Pageable.class))).thenReturn(memberPage);

        WalletStatement ws = new WalletStatement();
        ws.setPhoneNumber("628123456789");
        ws.setBalance(new BigDecimal("900000"));
        when(walletStatementRepository.findByPhoneNumberIn(any())).thenReturn(List.of(ws));
        when(transactionRepository.countByMemberIds(any()))
                .thenReturn(List.<Object[]>of(new Object[]{"member-id-001", 12L}));

        BaseResponse<PageResponse<MemberReportItemResponse>> result = reportService.getMemberReport(0, 10);

        assertThat(result.getResponseStatus()).isEqualTo(ErrorConstant.REPORT_MEMBER_SUCCESS.getCode());
        MemberReportItemResponse item = result.getData().getContent().get(0);
        assertThat(item.getBalance()).isEqualTo(new BigDecimal("900000"));
        assertThat(item.getTotalTransactions()).isEqualTo(12L);
    }

    @Test
    void getMemberReport_memberWithoutWalletStatement_shouldDefaultBalanceToZero() {
        Member member = new Member();
        member.setId("member-id-002");
        member.setFirstName("Sari");
        member.setLastName("Wijaya");
        member.setEmail("sari@example.com");
        member.setPhoneNumber("628999999999");
        member.setStatus(ActiveStatus.ACTIVE);

        Page<Member> memberPage = new PageImpl<>(List.of(member), PageRequest.of(0, 10), 1);
        when(memberRepository.searchMembers(eq(""), isNull(), any(Pageable.class))).thenReturn(memberPage);
        when(walletStatementRepository.findByPhoneNumberIn(any())).thenReturn(List.of());
        when(transactionRepository.countByMemberIds(any())).thenReturn(List.of());

        BaseResponse<PageResponse<MemberReportItemResponse>> result = reportService.getMemberReport(0, 10);

        assertThat(result.getData().getContent().get(0).getBalance()).isEqualTo(BigDecimal.ZERO);
        assertThat(result.getData().getContent().get(0).getTotalTransactions()).isZero();
    }

    @Test
    void getMemberReport_withoutTransactionAccess_shouldHideBalance() {
        when(adminAuthorizationService.canViewMemberTransactions()).thenReturn(false);
        Member member = new Member();
        member.setId("member-id-003");
        member.setFirstName("Andi");
        member.setLastName("Pratama");
        member.setPhoneNumber("628777777777");

        when(memberRepository.searchMembers(eq(""), isNull(), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(member), PageRequest.of(0, 10), 1));
        when(transactionRepository.countByMemberIds(any()))
                .thenReturn(List.<Object[]>of(new Object[]{"member-id-003", 4L}));

        MemberReportItemResponse item = reportService.getMemberReport(0, 10).getData().getContent().get(0);

        assertThat(item.getBalance()).isNull();
        assertThat(item.getTotalTransactions()).isEqualTo(4L);
        verifyNoInteractions(walletStatementRepository);
    }

    @Test
    void getMemberReport_emptyPage_shouldNotQueryBalancesOrCountsPerMember() {
        when(memberRepository.searchMembers(eq(""), isNull(), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(), PageRequest.of(0, 10), 0));
        when(walletStatementRepository.findByPhoneNumberIn(any())).thenReturn(List.of());

        BaseResponse<PageResponse<MemberReportItemResponse>> result = reportService.getMemberReport(0, 10);

        assertThat(result.getData().getContent()).isEmpty();
        verify(transactionRepository, never()).countByMemberIds(any());
    }

    // =========================================================================
    // helpers
    // =========================================================================
    private Transaction buildTransaction(TransactionType type, BigDecimal amount, LocalDateTime date) {
        Transaction transaction = new Transaction();
        transaction.setId("trx-" + date.toLocalTime());
        transaction.setReferenceNumber("REF-001");
        transaction.setUserEmail("budi@example.com");
        transaction.setUserPhoneNumber("628123456789");
        transaction.setTransactionCategory(type == TransactionType.INCOME ? "SALARY" : "FOOD_AND_BEVERAGE");
        transaction.setTransactionType(type);
        transaction.setStatus(TransactionStatus.SUCCESS);
        transaction.setAmount(amount);
        transaction.setTransactionDate(date);
        return transaction;
    }


    @Test
    void getIncomeExpenseReport_nullTotal_shouldBeTreatedAsZero() {
        when(transactionRepository.sumPlatformAmountPerDay(eq("INCOME"), any(), any()))
                .thenReturn(List.<Object[]>of(new Object[]{"2026-06-01", null}));

        BaseResponse<List<IncomeExpenseReportResponse>> result = reportService.getIncomeExpenseReport(
                LocalDate.of(2026, 6, 1), LocalDate.of(2026, 6, 1), TransactionType.INCOME);

        assertThat(result.getData().get(0).getTotalIncome()).isEqualTo(BigDecimal.ZERO);
    }
}
