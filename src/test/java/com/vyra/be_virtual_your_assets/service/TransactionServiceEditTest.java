package com.vyra.be_virtual_your_assets.service;

import com.vyra.be_virtual_your_assets.constant.ErrorConstant;
import com.vyra.be_virtual_your_assets.constant.MemberActivityEvent;
import com.vyra.be_virtual_your_assets.constant.WalletEntryType;
import com.vyra.be_virtual_your_assets.constant.transaction.TransactionCategory;
import com.vyra.be_virtual_your_assets.constant.transaction.TransactionStatus;
import com.vyra.be_virtual_your_assets.constant.transaction.TransactionType;
import com.vyra.be_virtual_your_assets.dto.BaseResponse;
import com.vyra.be_virtual_your_assets.dto.transaction.TransactionMutationResponse;
import com.vyra.be_virtual_your_assets.dto.transaction.UpdateTransactionRequest;
import com.vyra.be_virtual_your_assets.dto.wallet.AdjustTransactionWalletRequest;
import com.vyra.be_virtual_your_assets.dto.wallet.GetMemberWalletResponse;
import com.vyra.be_virtual_your_assets.dto.wallet.TransactionWalletResponse;
import com.vyra.be_virtual_your_assets.entity.FinanceCategory;
import com.vyra.be_virtual_your_assets.entity.Member;
import com.vyra.be_virtual_your_assets.entity.Transaction;
import com.vyra.be_virtual_your_assets.exception.BadRequestException;
import com.vyra.be_virtual_your_assets.exception.NotFoundException;
import com.vyra.be_virtual_your_assets.repository.TransactionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** Edit / delete of the member's own transactions (Phase 1.1 / 1.2). */
@ExtendWith(MockitoExtension.class)
class TransactionServiceEditTest {

    private static final String MEMBER_ID = "member-1";
    private static final String TRX_ID = "trx-1";

    @Mock private TransactionRepository transactionRepository;
    @Mock private MemberActivityService memberActivityService;
    @Mock private ValidationService validationService;
    @Mock private WalletService walletService;
    @Mock private CategoryService categoryService;
    @Spy private TimeService timeService =
            new TimeService(Clock.fixed(Instant.parse("2026-10-22T17:30:00Z"), ZoneOffset.UTC), "Asia/Jakarta");

    @InjectMocks
    private TransactionService transactionService;

    private Member member;
    private Transaction expense;

    @BeforeEach
    void setUp() {
        // Any requested category resolves to the built-in "Other" of the transaction type.
        lenient().when(categoryService.resolveForTransaction(any(), any(), any(), any()))
                .thenAnswer(inv -> builtInOther(inv.getArgument(1)));
        member = new Member();
        member.setId(MEMBER_ID);
        member.setPhoneNumber("628123456789");
        member.setTimezone("Asia/Jakarta");
        when(validationService.getMemberById(MEMBER_ID)).thenReturn(member);

        expense = new Transaction();
        expense.setId(TRX_ID);
        expense.setMemberId(MEMBER_ID);
        expense.setTransactionType(TransactionType.EXPENSE);
        expense.setTransactionCategory("FOOD_AND_BEVERAGE");
        expense.setAmount(new BigDecimal("-50000"));
        expense.setStatus(TransactionStatus.SUCCESS);
        expense.setTransactionDesc("Makan siang");
        expense.setTransactionDate(LocalDateTime.of(2026, 10, 22, 5, 30));
        expense.setLocalDate(LocalDate.of(2026, 10, 22));
    }

    // =========================================================================
    // update
    // =========================================================================
    @Test
    void update_amountOnly_shouldKeepTypeAndCorrectWalletByDifference() {
        when(transactionRepository.findOwnedForUpdate(TRX_ID, MEMBER_ID)).thenReturn(Optional.of(expense));
        when(walletService.adjustTransactionWallet(any())).thenReturn(wallet("725000"));
        UpdateTransactionRequest request = new UpdateTransactionRequest();
        request.setAmount(new BigDecimal("75000"));

        BaseResponse<TransactionMutationResponse> result =
                transactionService.updateOwnTransaction(MEMBER_ID, TRX_ID, request);

        AdjustTransactionWalletRequest adjust = capturedAdjust();
        assertThat(adjust.getOldType()).isEqualTo(TransactionType.EXPENSE);
        assertThat(adjust.getOldAmount()).isEqualByComparingTo("-50000");
        assertThat(adjust.getNewType()).isEqualTo(TransactionType.EXPENSE);
        assertThat(adjust.getNewAmount()).isEqualByComparingTo("-75000");
        assertThat(adjust.getEntryType()).isEqualTo(WalletEntryType.UPDATE);

        assertThat(result.getResponseStatus()).isEqualTo(ErrorConstant.UPDATE_TRANSACTION_SUCCESS.getCode());
        assertThat(result.getData().getBalance()).isEqualByComparingTo("725000");
        assertThat(result.getData().getTransaction().getAmount()).isEqualByComparingTo("-75000");
        assertThat(expense.getTransactionDesc()).isEqualTo("Makan siang");
        assertThat(expense.getUpdatedAt()).isEqualTo(LocalDateTime.of(2026, 10, 22, 17, 30));
        verify(transactionRepository).save(expense);
        verify(memberActivityService).createMemberActivity("628123456789", MemberActivityEvent.SUCCESS_UPDATE_TRANSACTION);
    }

    @Test
    void update_typeCategoryDateAndDescription_shouldApplyAllAndRecomputeLocalDate() {
        when(transactionRepository.findOwnedForUpdate(TRX_ID, MEMBER_ID)).thenReturn(Optional.of(expense));
        when(walletService.adjustTransactionWallet(any())).thenReturn(wallet("850000"));
        UpdateTransactionRequest request = new UpdateTransactionRequest();
        request.setType(TransactionType.INCOME);
        request.setCategory(TransactionCategory.OTHERS_INCOME);
        request.setTransactionDate("2026-10-23T00:30:00+07:00");
        request.setTransactionDesc("  Refund  ");

        transactionService.updateOwnTransaction(MEMBER_ID, TRX_ID, request);

        assertThat(expense.getTransactionType()).isEqualTo(TransactionType.INCOME);
        assertThat(expense.getAmount()).isEqualByComparingTo("50000");
        assertThat(expense.getTransactionCategory()).isEqualTo("OTHERS_INCOME");
        assertThat(expense.getTransactionDate()).isEqualTo(LocalDateTime.of(2026, 10, 22, 17, 30));
        assertThat(expense.getLocalDate()).isEqualTo(LocalDate.of(2026, 10, 23));
        assertThat(expense.getTransactionDesc()).isEqualTo("Refund");
        assertThat(capturedAdjust().getNewType()).isEqualTo(TransactionType.INCOME);
    }

    @Test
    void update_otherMembersOrMissingTransaction_shouldBe404() {
        when(transactionRepository.findOwnedForUpdate(TRX_ID, MEMBER_ID)).thenReturn(Optional.empty());

        NotFoundException ex = assertThrows(NotFoundException.class,
                () -> transactionService.updateOwnTransaction(MEMBER_ID, TRX_ID, new UpdateTransactionRequest()));

        assertThat(ex.getErrorConstant()).isEqualTo(ErrorConstant.TRANSACTION_NOT_FOUND);
        verify(walletService, never()).adjustTransactionWallet(any());
    }

    @Test
    void update_deletedTransaction_shouldBe404() {
        expense.setStatus(TransactionStatus.DELETED);
        expense.setDeletedAt(LocalDateTime.of(2026, 10, 22, 6, 0));
        when(transactionRepository.findOwnedForUpdate(TRX_ID, MEMBER_ID)).thenReturn(Optional.of(expense));

        assertThrows(NotFoundException.class,
                () -> transactionService.updateOwnTransaction(MEMBER_ID, TRX_ID, new UpdateTransactionRequest()));
    }

    @Test
    void update_notSuccessfulTransaction_shouldBeRejected() {
        expense.setStatus(TransactionStatus.INQUIRY);
        when(transactionRepository.findOwnedForUpdate(TRX_ID, MEMBER_ID)).thenReturn(Optional.of(expense));

        BadRequestException ex = assertThrows(BadRequestException.class,
                () -> transactionService.updateOwnTransaction(MEMBER_ID, TRX_ID, new UpdateTransactionRequest()));

        assertThat(ex.getErrorConstant()).isEqualTo(ErrorConstant.TRANSACTION_NOT_EDITABLE);
        verify(walletService, never()).adjustTransactionWallet(any());
    }

    // =========================================================================
    // delete
    // =========================================================================
    @Test
    void delete_shouldSoftDeleteAndRemoveFromWallet() {
        when(transactionRepository.findOwnedForUpdate(TRX_ID, MEMBER_ID)).thenReturn(Optional.of(expense));
        when(walletService.getMemberWallet("628123456789")).thenReturn(memberWallet("800000"));

        BaseResponse<TransactionMutationResponse> result = transactionService.deleteOwnTransaction(MEMBER_ID, TRX_ID);

        AdjustTransactionWalletRequest adjust = capturedAdjust();
        assertThat(adjust.getEntryType()).isEqualTo(WalletEntryType.DELETE);
        assertThat(adjust.getOldAmount()).isEqualByComparingTo("-50000");
        assertThat(adjust.getNewType()).isNull();
        assertThat(adjust.getNewAmount()).isEqualByComparingTo("0");

        assertThat(expense.getStatus()).isEqualTo(TransactionStatus.DELETED);
        assertThat(expense.getDeletedAt()).isEqualTo(LocalDateTime.of(2026, 10, 22, 17, 30));
        assertThat(result.getResponseStatus()).isEqualTo(ErrorConstant.DELETE_TRANSACTION_SUCCESS.getCode());
        assertThat(result.getData().getBalance()).isEqualByComparingTo("800000");
        verify(memberActivityService).createMemberActivity("628123456789", MemberActivityEvent.SUCCESS_DELETE_TRANSACTION);
    }

    @Test
    void delete_twice_shouldCorrectTheWalletOnlyOnce() {
        expense.setStatus(TransactionStatus.DELETED);
        expense.setDeletedAt(LocalDateTime.of(2026, 10, 22, 6, 0));
        when(transactionRepository.findOwnedForUpdate(TRX_ID, MEMBER_ID)).thenReturn(Optional.of(expense));
        when(walletService.getMemberWallet("628123456789")).thenReturn(memberWallet("800000"));

        BaseResponse<TransactionMutationResponse> result = transactionService.deleteOwnTransaction(MEMBER_ID, TRX_ID);

        assertThat(result.getResponseStatus()).isEqualTo(ErrorConstant.DELETE_TRANSACTION_SUCCESS.getCode());
        verify(walletService, never()).adjustTransactionWallet(any());
        verify(transactionRepository, never()).save(any());
    }

    @Test
    void delete_inquiryTransaction_shouldNotTouchWallet() {
        expense.setStatus(TransactionStatus.INQUIRY);
        when(transactionRepository.findOwnedForUpdate(TRX_ID, MEMBER_ID)).thenReturn(Optional.of(expense));
        when(walletService.getMemberWallet("628123456789")).thenReturn(memberWallet("750000"));

        transactionService.deleteOwnTransaction(MEMBER_ID, TRX_ID);

        verify(walletService, never()).adjustTransactionWallet(any());
        assertThat(expense.getStatus()).isEqualTo(TransactionStatus.DELETED);
    }

    @Test
    void delete_missingTransaction_shouldBe404() {
        when(transactionRepository.findOwnedForUpdate(TRX_ID, MEMBER_ID)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> transactionService.deleteOwnTransaction(MEMBER_ID, TRX_ID));
    }

    // =========================================================================
    // helpers
    // =========================================================================
    private AdjustTransactionWalletRequest capturedAdjust() {
        ArgumentCaptor<AdjustTransactionWalletRequest> captor = ArgumentCaptor.forClass(AdjustTransactionWalletRequest.class);
        verify(walletService).adjustTransactionWallet(captor.capture());
        return captor.getValue();
    }

    private static TransactionWalletResponse wallet(String balance) {
        TransactionWalletResponse response = new TransactionWalletResponse();
        response.setBalance(new BigDecimal(balance));
        return response;
    }

    private static BaseResponse<GetMemberWalletResponse> memberWallet(String balance) {
        GetMemberWalletResponse response = new GetMemberWalletResponse();
        response.setBalance(new BigDecimal(balance));
        return new BaseResponse<>(ErrorConstant.GET_MEMBER_WALLET_SUCCESS.getCode(), "ok", response);
    }

    private static FinanceCategory builtInOther(TransactionType type) {
        FinanceCategory category = new FinanceCategory();
        category.setId(FinanceCategory.otherId(type));
        category.setCode(type == TransactionType.INCOME ? "OTHERS_INCOME" : "OTHERS_EXPENSE");
        category.setType(type);
        category.setName("Other");
        return category;
    }
}
