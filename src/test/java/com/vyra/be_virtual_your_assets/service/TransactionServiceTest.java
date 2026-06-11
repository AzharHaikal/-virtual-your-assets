package com.vyra.be_virtual_your_assets.service;

import com.vyra.be_virtual_your_assets.constant.ErrorConstant;
import com.vyra.be_virtual_your_assets.constant.MemberActivityEvent;
import com.vyra.be_virtual_your_assets.constant.transaction.TransactionCategory;
import com.vyra.be_virtual_your_assets.constant.transaction.TransactionStatus;
import com.vyra.be_virtual_your_assets.constant.transaction.TransactionType;
import com.vyra.be_virtual_your_assets.dto.BaseResponse;
import com.vyra.be_virtual_your_assets.dto.chart.GetChartResponse;
import com.vyra.be_virtual_your_assets.dto.transaction.*;
import com.vyra.be_virtual_your_assets.dto.wallet.TransactionWalletRequest;
import com.vyra.be_virtual_your_assets.dto.wallet.TransactionWalletResponse;
import com.vyra.be_virtual_your_assets.entity.Member;
import com.vyra.be_virtual_your_assets.entity.Transaction;
import com.vyra.be_virtual_your_assets.exception.BusinessException;
import com.vyra.be_virtual_your_assets.repository.TransactionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TransactionServiceTest {

    @Mock private TransactionRepository transactionRepository;
    @Mock private MemberActivityService memberActivityService;
    @Mock private ValidationService validationService;
    @Mock private WalletService walletService;

    @InjectMocks
    private TransactionService transactionService;

    private Member member;

    @BeforeEach
    void setUp() {
        member = new Member();
        member.setId("member-uuid-001");
        member.setFirstName("Budi");
        member.setLastName("Santoso");
        member.setEmail("budi@example.com");
        member.setPhoneNumber("628123456789");
    }

    // =========================================================================
    // getChart — period null → default "1W"
    // =========================================================================
    @Test
    void getChart_periodNull_shouldUseDefaultWeekly() {
        when(transactionRepository.getChartDaily(anyString(), any(LocalDateTime.class))).thenReturn(List.of());

        BaseResponse<GetChartResponse> result = transactionService.getChart("member-uuid-001", null);

        assertThat(result.getResponseStatus()).isEqualTo(ErrorConstant.GET_CHART_SUCCESS.getCode());
        verify(transactionRepository).getChartDaily(eq("member-uuid-001"), any(LocalDateTime.class));
    }

    // =========================================================================
    // getChart — period "1W"
    // =========================================================================
    @Test
    void getChart_period1W_shouldCallGetChartDaily() {
        when(transactionRepository.getChartDaily(anyString(), any(LocalDateTime.class))).thenReturn(List.of());

        transactionService.getChart("member-uuid-001", "1W");

        verify(transactionRepository).getChartDaily(eq("member-uuid-001"), any(LocalDateTime.class));
    }

    // =========================================================================
    // getChart — period "1M"
    // =========================================================================
    @Test
    void getChart_period1M_shouldCallGetChartDailyWithMonthOffset() {
        when(transactionRepository.getChartDaily(anyString(), any(LocalDateTime.class))).thenReturn(List.of());

        transactionService.getChart("member-uuid-001", "1M");

        verify(transactionRepository).getChartDaily(eq("member-uuid-001"), any(LocalDateTime.class));
    }

    // =========================================================================
    // getChart — period "3M"
    // =========================================================================
    @Test
    void getChart_period3M_shouldCallGetChartWeekly() {
        when(transactionRepository.getChartWeekly(anyString(), any(LocalDateTime.class))).thenReturn(List.of());

        transactionService.getChart("member-uuid-001", "3M");

        verify(transactionRepository).getChartWeekly(eq("member-uuid-001"), any(LocalDateTime.class));
    }

    // =========================================================================
    // getChart — period "1Y"
    // =========================================================================
    @Test
    void getChart_period1Y_shouldCallGetChartMonthly() {
        when(transactionRepository.getChartMonthly(anyString(), any(LocalDateTime.class))).thenReturn(List.of());

        transactionService.getChart("member-uuid-001", "1Y");

        verify(transactionRepository).getChartMonthly(eq("member-uuid-001"), any(LocalDateTime.class));
    }

    // =========================================================================
    // getChart — period "ALL"
    // =========================================================================
    @Test
    void getChart_periodAll_shouldCallGetChartMonthlyAll() {
        when(transactionRepository.getChartMonthlyAll("member-uuid-001")).thenReturn(List.of());

        transactionService.getChart("member-uuid-001", "ALL");

        verify(transactionRepository).getChartMonthlyAll("member-uuid-001");
    }

    // =========================================================================
    // getChart — period lowercase "1w" normalized to "1W"
    // =========================================================================
    @Test
    void getChart_periodLowercase_shouldNormalizeAndUseWeekly() {
        when(transactionRepository.getChartDaily(anyString(), any(LocalDateTime.class))).thenReturn(List.of());

        transactionService.getChart("member-uuid-001", "1w");

        verify(transactionRepository).getChartDaily(anyString(), any(LocalDateTime.class));
    }

    // =========================================================================
    // getChart — results have data → chart filled correctly (1W with non-empty bucket)
    // =========================================================================
    @Test
    void getChart_1WWithSomeData_shouldBuildDailyChartAndTrimTrailingEmpty() {
        // Simulate one row for a date within the past 7 days
        String todayLabel = java.time.LocalDate.now().toString();
        Object[] row = new Object[]{todayLabel, BigDecimal.valueOf(100), BigDecimal.valueOf(50)};
        List<Object[]> rowList = new ArrayList<>();
        rowList.add(row);
        when(transactionRepository.getChartDaily(anyString(), any(LocalDateTime.class))).thenReturn(rowList);

        BaseResponse<GetChartResponse> result = transactionService.getChart("member-uuid-001", "1W");

        assertThat(result.getData().getChart()).isNotEmpty();
        // last entry should match today's data
        var last = result.getData().getChart().get(result.getData().getChart().size() - 1);
        assertThat(last.getLabel()).isEqualTo(todayLabel);
        assertThat(last.getIncome()).isEqualByComparingTo(BigDecimal.valueOf(100));
        assertThat(last.getExpense()).isEqualByComparingTo(BigDecimal.valueOf(50));
    }

    // =========================================================================
    // getChart — all buckets empty → returns full range (lastNonEmpty = -1)
    // =========================================================================
    @Test
    void getChart_1WAllEmpty_shouldReturnFullRangeChart() {
        when(transactionRepository.getChartDaily(anyString(), any(LocalDateTime.class))).thenReturn(List.of());

        BaseResponse<GetChartResponse> result = transactionService.getChart("member-uuid-001", "1W");

        assertThat(result.getData().getChart()).hasSize(7); // 7 days
        result.getData().getChart().forEach(item -> {
            assertThat(item.getIncome()).isEqualByComparingTo(BigDecimal.ZERO);
            assertThat(item.getExpense()).isEqualByComparingTo(BigDecimal.ZERO);
        });
    }

    @Test
    void getChart_1W_incomeOnly_shouldCoverExpenseFalseCondition() {
        String today = java.time.LocalDate.now().toString();
        Object[] row = new Object[]{today, BigDecimal.valueOf(100), BigDecimal.ZERO};
        when(transactionRepository.getChartDaily(anyString(), any())).thenReturn(List.<Object[]>of(row));

        BaseResponse<GetChartResponse> result = transactionService.getChart("member-uuid-001", "1W");

        var last = result.getData().getChart().get(result.getData().getChart().size() - 1);
        assertThat(last.getIncome()).isEqualByComparingTo("100");
        assertThat(last.getExpense()).isEqualByComparingTo("0");
    }

    @Test
    void getChart_1W_expenseOnly_shouldCoverIncomeFalseCondition() {
        String today = java.time.LocalDate.now().toString();
        Object[] row = new Object[]{today, BigDecimal.ZERO, BigDecimal.valueOf(50)};

        when(transactionRepository.getChartDaily(anyString(), any())).thenReturn(List.<Object[]>of(row));

        BaseResponse<GetChartResponse> result = transactionService.getChart("member-uuid-001", "1W");

        var last = result.getData().getChart().get(result.getData().getChart().size() - 1);
        assertThat(last.getIncome()).isEqualByComparingTo("0");
        assertThat(last.getExpense()).isEqualByComparingTo("50");
    }

    @Test
    void getChart_3M_shouldBuildWeeklyChart() {

        Object[] row = new Object[]{"Week-1", BigDecimal.valueOf(200), BigDecimal.valueOf(80)};

        when(transactionRepository.getChartWeekly(anyString(), any())).thenReturn(List.<Object[]>of(row));

        BaseResponse<GetChartResponse> result = transactionService.getChart("member-uuid-001", "3M");

        assertThat(result.getData().getChart()).hasSize(1);
        assertThat(result.getData().getChart().get(0).getLabel()).isEqualTo("Week-1");
        assertThat(result.getData().getChart().get(0).getIncome()).isEqualByComparingTo("200");
        assertThat(result.getData().getChart().get(0).getExpense()).isEqualByComparingTo("80");
    }

    @Test
    void getChart_1Y_shouldBuildMonthlyChart() {

        Object[] row = new Object[]{"2026-06", BigDecimal.valueOf(1000), BigDecimal.valueOf(300)};
        when(transactionRepository.getChartMonthly(anyString(), any())).thenReturn(List.<Object[]>of(row));

        BaseResponse<GetChartResponse> result = transactionService.getChart("member-uuid-001", "1Y");

        assertThat(result.getData().getChart()).hasSize(1);
        assertThat(result.getData().getChart().get(0).getLabel()).isEqualTo("2026-06");
        assertThat(result.getData().getChart().get(0).getIncome()).isEqualByComparingTo("1000");
        assertThat(result.getData().getChart().get(0).getExpense()).isEqualByComparingTo("300");
    }

    @Test
    void getChart_all_shouldBuildMonthlyChart() {
        Object[] row = new Object[]{"2025-01", BigDecimal.valueOf(500), BigDecimal.valueOf(100)};
        when(transactionRepository.getChartMonthlyAll(anyString())).thenReturn(List.<Object[]>of(row));

        BaseResponse<GetChartResponse> result = transactionService.getChart("member-uuid-001", "ALL");

        assertThat(result.getData().getChart()).hasSize(1);
        assertThat(result.getData().getChart().get(0).getLabel()).isEqualTo("2025-01");
    }

    // =========================================================================
    // getTopTransactionHistory
    // =========================================================================
    @Test
    void getTopTransactionHistory_success_shouldReturnMappedList() {
        Transaction t = buildTransaction("trx-001", TransactionType.INCOME);
        when(validationService.getMemberById("member-uuid-001")).thenReturn(member);
        when(transactionRepository.findTransactionHistory("member-uuid-001")).thenReturn(List.of(t));

        BaseResponse<List<TransactionHistoryResponse>> result =
                transactionService.getTopTransactionHistory("member-uuid-001");

        assertThat(result.getResponseStatus()).isEqualTo(ErrorConstant.GET_TOP_TRANSACTION_HISTORY_SUCCESS.getCode());
        assertThat(result.getData()).hasSize(1);
        assertThat(result.getData().get(0).getTransactionId()).isEqualTo("trx-001");
        assertThat(result.getData().get(0).getType()).isEqualTo(TransactionType.INCOME);
    }

    @Test
    void getTopTransactionHistory_emptyList_shouldReturnEmptyResponse() {
        when(validationService.getMemberById("member-uuid-001")).thenReturn(member);
        when(transactionRepository.findTransactionHistory("member-uuid-001")).thenReturn(List.of());

        BaseResponse<List<TransactionHistoryResponse>> result =
                transactionService.getTopTransactionHistory("member-uuid-001");

        assertThat(result.getData()).isEmpty();
    }

    // =========================================================================
    // createTransaction — INCOME success
    // =========================================================================
    @Test
    void createTransaction_income_shouldNotNegateAmountAndReturnResponse() {
        CreateTransactionRequest request = buildCreateRequest(TransactionType.INCOME, BigDecimal.valueOf(500000));
        TransactionWalletResponse walletData = buildWalletResponse(BigDecimal.valueOf(1500000));
        BaseResponse<TransactionWalletResponse> walletResp = new BaseResponse<>(
                ErrorConstant.CREATE_TRANSACTION_WALLET_SUCCESS.getCode(), "ok", walletData);

        when(validationService.getMemberById("member-uuid-001")).thenReturn(member);
        when(transactionRepository.save(any(Transaction.class))).thenAnswer(inv -> {
            Transaction t = inv.getArgument(0);
            t.setId("trx-new-001");
            return t;
        });
        when(walletService.createTransactionWallet(any(TransactionWalletRequest.class))).thenReturn(walletResp);
        doNothing().when(memberActivityService).createMemberActivity(anyString(), any());

        BaseResponse<CreateTransactionResponse> result =
                transactionService.createTransaction("member-uuid-001", request);

        assertThat(result.getResponseStatus()).isEqualTo(ErrorConstant.CREATE_TRANSACTION_SUCCESS.getCode());
        assertThat(result.getData().getBalance()).isEqualByComparingTo(BigDecimal.valueOf(1500000));
        // Amount should remain positive for INCOME
        assertThat(request.getAmount()).isEqualByComparingTo(BigDecimal.valueOf(500000));
        verify(transactionRepository, times(2)).save(any(Transaction.class)); // INQUIRY then SUCCESS
    }

    // =========================================================================
    // createTransaction — EXPENSE success (amount negated)
    // =========================================================================
    @Test
    void createTransaction_expense_shouldNegateAmountBeforeSaving() {
        CreateTransactionRequest request = buildCreateRequest(TransactionType.EXPENSE, BigDecimal.valueOf(200000));
        TransactionWalletResponse walletData = buildWalletResponse(BigDecimal.valueOf(800000));
        BaseResponse<TransactionWalletResponse> walletResp = new BaseResponse<>(
                ErrorConstant.CREATE_TRANSACTION_WALLET_SUCCESS.getCode(), "ok", walletData);

        when(validationService.getMemberById("member-uuid-001")).thenReturn(member);
        when(transactionRepository.save(any(Transaction.class))).thenAnswer(inv -> {
            Transaction t = inv.getArgument(0);
            t.setId("trx-exp-001");
            return t;
        });
        when(walletService.createTransactionWallet(any(TransactionWalletRequest.class))).thenReturn(walletResp);
        doNothing().when(memberActivityService).createMemberActivity(anyString(), any());

        transactionService.createTransaction("member-uuid-001", request);

        // Amount should be negated
        assertThat(request.getAmount()).isEqualByComparingTo(BigDecimal.valueOf(-200000));
    }

    // =========================================================================
    // createTransaction — wallet response code not success → BusinessException BAD_REQUEST
    // =========================================================================
    @Test
    void createTransaction_walletResponseNotSuccess_shouldThrowBadRequestAndRecordFailedActivity() {
        CreateTransactionRequest request = buildCreateRequest(TransactionType.INCOME, BigDecimal.valueOf(100000));
        BaseResponse<TransactionWalletResponse> walletResp = new BaseResponse<>("VYRA-TWS-001", "fail", null);

        when(validationService.getMemberById("member-uuid-001")).thenReturn(member);
        when(transactionRepository.save(any())).thenAnswer(inv -> {
            Transaction t = inv.getArgument(0);
            t.setId("trx-fail-001");
            return t;
        });
        when(walletService.createTransactionWallet(any())).thenReturn(walletResp);
        doNothing().when(memberActivityService).createMemberActivity(anyString(), any());

        BusinessException ex = assertThrows(BusinessException.class,
                () -> transactionService.createTransaction("member-uuid-001", request));

        assertThat(ex.getErrorConstant()).isEqualTo(ErrorConstant.BAD_REQUEST);
        verify(memberActivityService).createMemberActivity(member.getPhoneNumber(), MemberActivityEvent.FAILED_CREATE_TRANSACTION);
    }

    // =========================================================================
    // createTransaction — walletService throws BusinessException → BAD_REQUEST
    // =========================================================================
    @Test
    void createTransaction_walletThrowsBusinessException_shouldThrowBadRequest() {
        CreateTransactionRequest request = buildCreateRequest(TransactionType.INCOME, BigDecimal.valueOf(100000));

        when(validationService.getMemberById("member-uuid-001")).thenReturn(member);
        when(transactionRepository.save(any())).thenAnswer(inv -> {
            Transaction t = inv.getArgument(0);
            t.setId("trx-err-001");
            return t;
        });
        when(walletService.createTransactionWallet(any()))
                .thenThrow(new BusinessException(ErrorConstant.MEMBER_NOT_FOUND));
        doNothing().when(memberActivityService).createMemberActivity(anyString(), any());

        BusinessException ex = assertThrows(BusinessException.class,
                () -> transactionService.createTransaction("member-uuid-001", request));

        assertThat(ex.getErrorConstant()).isEqualTo(ErrorConstant.BAD_REQUEST);
        verify(memberActivityService).createMemberActivity(member.getPhoneNumber(), MemberActivityEvent.FAILED_CREATE_TRANSACTION);
    }

    // =========================================================================
    // createTransaction — walletService throws unexpected Exception → InternalException
    // =========================================================================
    @Test
    void createTransaction_walletThrowsUnexpectedException_shouldThrowInternalExceptionAndRecordFailed() {
        CreateTransactionRequest request = buildCreateRequest(TransactionType.INCOME, BigDecimal.valueOf(100000));

        when(validationService.getMemberById("member-uuid-001")).thenReturn(member);
        when(transactionRepository.save(any())).thenAnswer(inv -> {
            Transaction t = inv.getArgument(0);
            t.setId("trx-err-002");
            return t;
        });
        when(walletService.createTransactionWallet(any()))
                .thenThrow(new RuntimeException("unexpected"));
        doNothing().when(memberActivityService).createMemberActivity(anyString(), any());

        assertThrows(Exception.class,
                () -> transactionService.createTransaction("member-uuid-001", request));

        verify(memberActivityService).createMemberActivity(member.getPhoneNumber(), MemberActivityEvent.FAILED_CREATE_TRANSACTION);
    }

    // =========================================================================
    // getTransactionHistory — with type filter
    // =========================================================================
    @Test
    void getTransactionHistory_withType_shouldReturnPagedResponse() {
        Transaction t = buildTransaction("trx-hist-001", TransactionType.INCOME);
        Page<Transaction> page = new PageImpl<>(List.of(t));

        when(validationService.getMemberById("member-uuid-001")).thenReturn(member);
        when(transactionRepository.findTransactionHistory(
                eq("member-uuid-001"), any(LocalDateTime.class), any(LocalDateTime.class),
                eq(TransactionType.INCOME), any(Pageable.class)))
                .thenReturn(page);

        BaseResponse<Page<TransactionHistoryResponse>> result =
                transactionService.getTransactionHistory("2026-01-01", "2026-06-01",
                        TransactionType.INCOME, 0, 10, "member-uuid-001");

        assertThat(result.getResponseStatus()).isEqualTo(ErrorConstant.GET_TRANSACTION_HISTORY_SUCCESS.getCode());
        assertThat(result.getData().getContent()).hasSize(1);
        assertThat(result.getData().getContent().get(0).getTransactionId()).isEqualTo("trx-hist-001");
    }

    // =========================================================================
    // getTransactionHistory — without type filter (null)
    // =========================================================================
    @Test
    void getTransactionHistory_withoutType_shouldPassNullType() {
        Page<Transaction> emptyPage = new PageImpl<>(List.of());
        when(validationService.getMemberById("member-uuid-001")).thenReturn(member);
        when(transactionRepository.findTransactionHistory(
                eq("member-uuid-001"), any(LocalDateTime.class), any(LocalDateTime.class),
                isNull(), any(Pageable.class)))
                .thenReturn(emptyPage);

        BaseResponse<Page<TransactionHistoryResponse>> result =
                transactionService.getTransactionHistory("2026-01-01", "2026-06-01",
                        null, 0, 10, "member-uuid-001");

        assertThat(result.getData().getContent()).isEmpty();
        verify(transactionRepository).findTransactionHistory(
                eq("member-uuid-001"), any(), any(), isNull(), any(Pageable.class));
    }

    // =========================================================================
    // updateTransaction — with transactions found
    // =========================================================================
    @Test
    void updateTransaction_withTransactions_shouldUpdateAllRows() {
        Transaction t1 = buildTransaction("trx-up-001", TransactionType.INCOME);
        Transaction t2 = buildTransaction("trx-up-002", TransactionType.EXPENSE);
        when(transactionRepository.findAllByUserPhoneNumber("628123456789")).thenReturn(List.of(t1, t2));

        TransactionUpdateRequest request = new TransactionUpdateRequest();
        request.setOldPhoneNumber("628123456789");
        request.setNewPhoneNumber("628999999999");
        request.setEmail("new@example.com");

        transactionService.updateTransaction(request);

        assertThat(t1.getUserPhoneNumber()).isEqualTo("628999999999");
        assertThat(t1.getUserEmail()).isEqualTo("new@example.com");
        assertThat(t1.getModifiedBy()).isEqualTo("628999999999");
        assertThat(t1.getUpdatedAt()).isNotNull();

        assertThat(t2.getUserPhoneNumber()).isEqualTo("628999999999");
    }

    // =========================================================================
    // updateTransaction — no transactions found → no updates
    // =========================================================================
    @Test
    void updateTransaction_noTransactions_shouldDoNothing() {
        when(transactionRepository.findAllByUserPhoneNumber("628000000000")).thenReturn(List.of());

        TransactionUpdateRequest request = new TransactionUpdateRequest();
        request.setOldPhoneNumber("628000000000");
        request.setNewPhoneNumber("628111111111");
        request.setEmail("new@example.com");

        transactionService.updateTransaction(request);
        // No exception, no save calls needed
        verify(transactionRepository).findAllByUserPhoneNumber("628000000000");
    }

    // =========================================================================
    // helpers
    // =========================================================================
    private Transaction buildTransaction(String id, TransactionType type) {
        Transaction t = new Transaction();
        t.setId(id);
        t.setMemberId("member-uuid-001");
        t.setUserPhoneNumber("628123456789");
        t.setUserEmail("budi@example.com");
        t.setTransactionCategory(TransactionCategory.SALARY.name());
        t.setTransactionType(type);
        t.setAmount(BigDecimal.valueOf(500000));
        t.setReferenceNumber("VYRA-REF-001");
        t.setTransactionDesc("Test transaction");
        t.setStatus(TransactionStatus.SUCCESS);
        t.setTransactionDate(LocalDateTime.now());
        return t;
    }

    private CreateTransactionRequest buildCreateRequest(TransactionType type, BigDecimal amount) {
        CreateTransactionRequest r = new CreateTransactionRequest();
        r.setCategory(TransactionCategory.SALARY);
        r.setType(type);
        r.setAmount(amount);
        r.setTransactionDate(LocalDateTime.now());
        r.setTransactionDesc("Test");
        return r;
    }

    private TransactionWalletResponse buildWalletResponse(BigDecimal balance) {
        TransactionWalletResponse r = new TransactionWalletResponse();
        r.setPhoneNumber("628123456789");
        r.setBalance(balance);
        r.setTotalIncome(BigDecimal.valueOf(500000));
        r.setTotalExpense(BigDecimal.ZERO);
        return r;
    }
}
