package com.vyra.be_virtual_your_assets.service;

import com.vyra.be_virtual_your_assets.constant.ErrorConstant;
import com.vyra.be_virtual_your_assets.constant.MemberActivityEvent;
import com.vyra.be_virtual_your_assets.constant.MemberStatus;
import com.vyra.be_virtual_your_assets.constant.transaction.TransactionType;
import com.vyra.be_virtual_your_assets.dto.BaseResponse;
import com.vyra.be_virtual_your_assets.dto.wallet.*;
import com.vyra.be_virtual_your_assets.entity.MemberWallet;
import com.vyra.be_virtual_your_assets.entity.WalletStatement;
import com.vyra.be_virtual_your_assets.entity.WalletStatementHistory;
import com.vyra.be_virtual_your_assets.exception.BusinessException;
import com.vyra.be_virtual_your_assets.repository.MemberWalletRepository;
import com.vyra.be_virtual_your_assets.repository.WalletStatementHistoryRepository;
import com.vyra.be_virtual_your_assets.repository.WalletStatementRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class WalletServiceTest {

    @Mock private MemberWalletRepository memberWalletRepository;
    @Mock private WalletStatementRepository walletStatementRepository;
    @Mock private WalletStatementHistoryRepository walletStatementHistoryRepository;
    @Mock private MemberActivityService memberActivityService;

    @InjectMocks
    private WalletService walletService;

    private WalletStatement walletStatement;
    private MemberWallet memberWallet;

    @BeforeEach
    void setUp() {
        walletStatement = new WalletStatement();
        walletStatement.setId("ws-id-001");
        walletStatement.setMemberWalletId("mw-id-001");
        walletStatement.setPhoneNumber("628123456789");
        walletStatement.setTotalIncome(BigDecimal.ZERO);
        walletStatement.setTotalExpense(BigDecimal.ZERO);
        walletStatement.setBalance(BigDecimal.valueOf(1000000));

        memberWallet = new MemberWallet();
        memberWallet.setId("mw-id-001");
        memberWallet.setMemberId("member-uuid-001");
        memberWallet.setPhoneNumber("628123456789");
        memberWallet.setStatus(MemberStatus.ACTIVE);
    }

    // =========================================================================
    // createWallet
    // =========================================================================
    @Test
    void createWallet_success_shouldSaveWalletAndStatementAndReturnResponse() {
        CreateWalletRequest request = new CreateWalletRequest();
        request.setPhoneNumber("628123456789");
        request.setMemberId("member-uuid-001");

        when(memberWalletRepository.save(any(MemberWallet.class))).thenReturn(memberWallet);
        when(walletStatementRepository.save(any(WalletStatement.class))).thenReturn(walletStatement);
        doNothing().when(memberActivityService).createMemberActivity(anyString(), any());

        BaseResponse<CreateWalletResponse> result = walletService.createWallet(request);

        assertThat(result.getResponseStatus()).isEqualTo(ErrorConstant.CREATE_WALLET_SUCCESS.getCode());
        assertThat(result.getData().getMemberId()).isEqualTo("member-uuid-001");
        assertThat(result.getData().getBalance()).isEqualByComparingTo(BigDecimal.ZERO);
        verify(memberWalletRepository).save(any(MemberWallet.class));
        verify(walletStatementRepository).save(any(WalletStatement.class));
        verify(memberActivityService).createMemberActivity(request.getPhoneNumber(), MemberActivityEvent.ATTEMPT_CREATE_WALLET);
        verify(memberActivityService).createMemberActivity(request.getPhoneNumber(), MemberActivityEvent.SUCCESS_CREATE_WALLET);
    }

    // =========================================================================
    // getMemberWallet — success
    // =========================================================================
    @Test
    void getMemberWallet_found_shouldReturnWalletResponse() {
        walletStatement.setTotalIncome(BigDecimal.valueOf(5000000));
        walletStatement.setTotalExpense(BigDecimal.valueOf(1000000));
        walletStatement.setBalance(BigDecimal.valueOf(4000000));
        when(walletStatementRepository.findByPhoneNumber("628123456789")).thenReturn(Optional.of(walletStatement));

        BaseResponse<GetMemberWalletResponse> result = walletService.getMemberWallet("628123456789");

        assertThat(result.getResponseStatus()).isEqualTo(ErrorConstant.GET_MEMBER_WALLET_SUCCESS.getCode());
        assertThat(result.getData().getBalance()).isEqualByComparingTo(BigDecimal.valueOf(4000000));
        assertThat(result.getData().getTotalIncome()).isEqualByComparingTo(BigDecimal.valueOf(5000000));
        assertThat(result.getData().getTotalExpense()).isEqualByComparingTo(BigDecimal.valueOf(1000000));
    }

    // =========================================================================
    // getMemberWallet — not found
    // =========================================================================
    @Test
    void getMemberWallet_notFound_shouldThrowMemberNotFound() {
        when(walletStatementRepository.findByPhoneNumber("628000000000")).thenReturn(Optional.empty());

        BusinessException ex = assertThrows(BusinessException.class,
                () -> walletService.getMemberWallet("628000000000"));

        assertThat(ex.getErrorConstant()).isEqualTo(ErrorConstant.MEMBER_NOT_FOUND);
    }

    // =========================================================================
    // updateMemberWallet — success
    // =========================================================================
    @Test
    void updateMemberWallet_success_shouldUpdateBothWalletAndStatement() {
        WalletUpdateRequest request = new WalletUpdateRequest();
        request.setOldPhoneNumber("628123456789");
        request.setNewPhoneNumber("628999999999");

        when(walletStatementRepository.findByPhoneNumber("628123456789")).thenReturn(Optional.of(walletStatement));
        when(memberWalletRepository.findByPhoneNumber("628123456789")).thenReturn(Optional.of(memberWallet));

        walletService.updateMemberWallet(request);

        assertThat(walletStatement.getPhoneNumber()).isEqualTo("628999999999");
        assertThat(memberWallet.getPhoneNumber()).isEqualTo("628999999999");
        assertThat(walletStatement.getModifiedBy()).isEqualTo("628999999999");
        assertThat(memberWallet.getModifiedBy()).isEqualTo("628999999999");
        assertThat(walletStatement.getUpdatedAt()).isNotNull();
        assertThat(memberWallet.getUpdatedAt()).isNotNull();
    }

    // =========================================================================
    // updateMemberWallet — wallet statement not found
    // =========================================================================
    @Test
    void updateMemberWallet_walletStatementNotFound_shouldThrowMemberNotFound() {
        WalletUpdateRequest request = new WalletUpdateRequest();
        request.setOldPhoneNumber("628000000000");
        request.setNewPhoneNumber("628111111111");
        when(walletStatementRepository.findByPhoneNumber("628000000000")).thenReturn(Optional.empty());

        BusinessException ex = assertThrows(BusinessException.class,
                () -> walletService.updateMemberWallet(request));

        assertThat(ex.getErrorConstant()).isEqualTo(ErrorConstant.MEMBER_NOT_FOUND);
        verify(memberWalletRepository, never()).findByPhoneNumber(any());
    }

    // =========================================================================
    // updateMemberWallet — member wallet not found
    // =========================================================================
    @Test
    void updateMemberWallet_memberWalletNotFound_shouldThrowMemberNotFound() {
        WalletUpdateRequest request = new WalletUpdateRequest();
        request.setOldPhoneNumber("628123456789");
        request.setNewPhoneNumber("628999999999");
        when(walletStatementRepository.findByPhoneNumber("628123456789")).thenReturn(Optional.of(walletStatement));
        when(memberWalletRepository.findByPhoneNumber("628123456789")).thenReturn(Optional.empty());

        BusinessException ex = assertThrows(BusinessException.class,
                () -> walletService.updateMemberWallet(request));

        assertThat(ex.getErrorConstant()).isEqualTo(ErrorConstant.MEMBER_NOT_FOUND);
    }

    // =========================================================================
    // createTransactionWallet — INCOME
    // =========================================================================
    @Test
    void createTransactionWallet_income_shouldAddToIncomeAndBalance() {
        TransactionWalletRequest request = buildTxWalletRequest("trx-001", "628123456789",
                BigDecimal.valueOf(500000), TransactionType.INCOME);
        when(walletStatementRepository.findByPhoneNumber("628123456789")).thenReturn(Optional.of(walletStatement));
        when(walletStatementHistoryRepository.save(any())).thenReturn(new WalletStatementHistory());
        when(walletStatementRepository.save(any())).thenReturn(walletStatement);
        doNothing().when(memberActivityService).createMemberActivity(anyString(), any());

        BaseResponse<TransactionWalletResponse> result = walletService.createTransactionWallet(request);

        assertThat(result.getResponseStatus()).isEqualTo(ErrorConstant.CREATE_TRANSACTION_WALLET_SUCCESS.getCode());
        assertThat(walletStatement.getBalance()).isEqualByComparingTo(BigDecimal.valueOf(1500000));
        assertThat(walletStatement.getTotalIncome()).isEqualByComparingTo(BigDecimal.valueOf(500000));
        assertThat(walletStatement.getTotalExpense()).isEqualByComparingTo(BigDecimal.ZERO);

        // Verify history captured
        ArgumentCaptor<WalletStatementHistory> histCaptor = ArgumentCaptor.forClass(WalletStatementHistory.class);
        verify(walletStatementHistoryRepository).save(histCaptor.capture());
        WalletStatementHistory history = histCaptor.getValue();
        assertThat(history.getIncome()).isEqualByComparingTo(BigDecimal.valueOf(500000));
        assertThat(history.getExpense()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(history.getPreviousBalance()).isEqualByComparingTo(BigDecimal.valueOf(1000000));
        assertThat(history.getCurrentBalance()).isEqualByComparingTo(BigDecimal.valueOf(1500000));
    }

    // =========================================================================
    // createTransactionWallet — EXPENSE (negative amount)
    // =========================================================================
    @Test
    void createTransactionWallet_expense_shouldAddNegativeAmountToExpenseAndBalance() {
        // Expense amounts are negated before reaching WalletService
        BigDecimal negativeAmount = BigDecimal.valueOf(-200000);
        TransactionWalletRequest request = buildTxWalletRequest("trx-002", "628123456789",
                negativeAmount, TransactionType.EXPENSE);
        when(walletStatementRepository.findByPhoneNumber("628123456789")).thenReturn(Optional.of(walletStatement));
        when(walletStatementHistoryRepository.save(any())).thenReturn(new WalletStatementHistory());
        when(walletStatementRepository.save(any())).thenReturn(walletStatement);
        doNothing().when(memberActivityService).createMemberActivity(anyString(), any());

        BaseResponse<TransactionWalletResponse> result = walletService.createTransactionWallet(request);

        assertThat(result.getResponseStatus()).isEqualTo(ErrorConstant.CREATE_TRANSACTION_WALLET_SUCCESS.getCode());
        assertThat(walletStatement.getBalance()).isEqualByComparingTo(BigDecimal.valueOf(800000));
        assertThat(walletStatement.getTotalExpense()).isEqualByComparingTo(BigDecimal.valueOf(-200000));
        assertThat(walletStatement.getTotalIncome()).isEqualByComparingTo(BigDecimal.ZERO);

        ArgumentCaptor<WalletStatementHistory> histCaptor = ArgumentCaptor.forClass(WalletStatementHistory.class);
        verify(walletStatementHistoryRepository).save(histCaptor.capture());
        WalletStatementHistory history = histCaptor.getValue();
        assertThat(history.getExpense()).isEqualByComparingTo(BigDecimal.valueOf(-200000));
        assertThat(history.getIncome()).isEqualByComparingTo(BigDecimal.ZERO);
    }

    // =========================================================================
    // createTransactionWallet — wallet not found
    // =========================================================================
    @Test
    void createTransactionWallet_walletNotFound_shouldThrowMemberNotFound() {
        TransactionWalletRequest request = buildTxWalletRequest("trx-003", "628000000000",
                BigDecimal.valueOf(100000), TransactionType.INCOME);
        when(walletStatementRepository.findByPhoneNumber("628000000000")).thenReturn(Optional.empty());
        doNothing().when(memberActivityService).createMemberActivity(anyString(), any());

        BusinessException ex = assertThrows(BusinessException.class,
                () -> walletService.createTransactionWallet(request));

        assertThat(ex.getErrorConstant()).isEqualTo(ErrorConstant.MEMBER_NOT_FOUND);
    }

    // =========================================================================
    // helpers
    // =========================================================================
    private TransactionWalletRequest buildTxWalletRequest(String trxId, String phone,
                                                           BigDecimal amount, TransactionType type) {
        TransactionWalletRequest req = new TransactionWalletRequest();
        req.setTransactionId(trxId);
        req.setPhoneNumber(phone);
        req.setAmount(amount);
        req.setTransactionType(type);
        return req;
    }
}
