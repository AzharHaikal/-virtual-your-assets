package com.vyra.be_virtual_your_assets.service;

import com.vyra.be_virtual_your_assets.constant.ErrorConstant;
import com.vyra.be_virtual_your_assets.constant.MemberActivityEvent;
import com.vyra.be_virtual_your_assets.dto.BaseResponse;
import com.vyra.be_virtual_your_assets.dto.auth.ChangePinRequest;
import com.vyra.be_virtual_your_assets.dto.member.GetMemberResponse;
import com.vyra.be_virtual_your_assets.dto.member.UpdateProfileRequest;
import com.vyra.be_virtual_your_assets.dto.member.UpdateProfileResponse;
import com.vyra.be_virtual_your_assets.dto.member.ValidateIsUpdateProfile;
import com.vyra.be_virtual_your_assets.dto.transaction.TransactionUpdateRequest;
import com.vyra.be_virtual_your_assets.dto.wallet.GetMemberWalletResponse;
import com.vyra.be_virtual_your_assets.dto.wallet.WalletUpdateRequest;
import com.vyra.be_virtual_your_assets.entity.Member;
import com.vyra.be_virtual_your_assets.exception.BusinessException;
import com.vyra.be_virtual_your_assets.repository.TransactionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MemberServiceTest {

    @Mock private TransactionRepository transactionRepository;
    @Mock private MemberActivityService memberActivityService;
    @Mock private TransactionService transactionService;
    @Mock private ValidationService validationService;
    @Mock private WalletService walletService;
    @Mock private BCryptPasswordEncoder passwordEncoder;

    @InjectMocks
    private MemberService memberService;

    private Member member;

    @BeforeEach
    void setUp() {
        member = new Member();
        member.setId("member-uuid-001");
        member.setFirstName("Budi");
        member.setLastName("Santoso");
        member.setEmail("budi@example.com");
        member.setPhoneNumber("628123456789");
        member.setPin("hashed-pin");
    }

    // =========================================================================
    // getMember — success, previous month income > 0 (growth calculated)
    // =========================================================================
    @Test
    void getMember_success_withPositivePreviousIncome_shouldCalculateGrowth() {
        GetMemberWalletResponse walletData = buildWalletResponse(BigDecimal.valueOf(5000000),
                BigDecimal.valueOf(2000000), BigDecimal.valueOf(3000000));
        BaseResponse<GetMemberWalletResponse> walletResp = new BaseResponse<>(
                ErrorConstant.GET_MEMBER_WALLET_SUCCESS.getCode(), "ok", walletData);

        when(validationService.getMemberById("member-uuid-001")).thenReturn(member);
        when(walletService.getMemberWallet("628123456789")).thenReturn(walletResp);
        when(transactionRepository.getPreviousMonthIncome(anyString(), any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(BigDecimal.valueOf(2000000));

        BaseResponse<GetMemberResponse> result = memberService.getMember("member-uuid-001");

        assertThat(result.getResponseStatus()).isEqualTo(ErrorConstant.GET_MEMBER_SUCCESS.getCode());
        assertThat(result.getData().getFirstName()).isEqualTo("Budi");
        assertThat(result.getData().getFullName()).isEqualTo("Budi Santoso");
        assertThat(result.getData().getBalance()).isEqualByComparingTo(BigDecimal.valueOf(3000000));
        // growth = (3000000 - 2000000) * 100 / 2000000 = 50%
        assertThat(result.getData().getGrowthPercentage()).isEqualByComparingTo(BigDecimal.valueOf(50.00).setScale(2));
    }

    // =========================================================================
    // getMember — previous month = 0, current balance = 0 → growth = 0
    // =========================================================================
    @Test
    void getMember_previousAndCurrentZero_shouldReturnGrowthZero() {
        GetMemberWalletResponse walletData = buildWalletResponse(BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO);
        BaseResponse<GetMemberWalletResponse> walletResp = new BaseResponse<>(
                ErrorConstant.GET_MEMBER_WALLET_SUCCESS.getCode(), "ok", walletData);

        when(validationService.getMemberById("member-uuid-001")).thenReturn(member);
        when(walletService.getMemberWallet("628123456789")).thenReturn(walletResp);
        when(transactionRepository.getPreviousMonthIncome(anyString(), any(), any()))
                .thenReturn(BigDecimal.ZERO);

        BaseResponse<GetMemberResponse> result = memberService.getMember("member-uuid-001");

        assertThat(result.getData().getGrowthPercentage()).isEqualByComparingTo(BigDecimal.ZERO);
    }

    // =========================================================================
    // getMember — previous month = 0, current balance > 0 → growth = 100
    // =========================================================================
    @Test
    void getMember_previousZeroCurrentPositive_shouldReturnGrowth100() {
        GetMemberWalletResponse walletData = buildWalletResponse(BigDecimal.valueOf(1000000),
                BigDecimal.ZERO, BigDecimal.valueOf(1000000));
        BaseResponse<GetMemberWalletResponse> walletResp = new BaseResponse<>(
                ErrorConstant.GET_MEMBER_WALLET_SUCCESS.getCode(), "ok", walletData);

        when(validationService.getMemberById("member-uuid-001")).thenReturn(member);
        when(walletService.getMemberWallet("628123456789")).thenReturn(walletResp);
        when(transactionRepository.getPreviousMonthIncome(anyString(), any(), any()))
                .thenReturn(BigDecimal.ZERO);

        BaseResponse<GetMemberResponse> result = memberService.getMember("member-uuid-001");

        assertThat(result.getData().getGrowthPercentage()).isEqualByComparingTo(BigDecimal.valueOf(100));
    }

    // =========================================================================
    // getMember — wallet response code not success → BusinessException
    // =========================================================================
    @Test
    void getMember_walletResponseCodeNotSuccess_shouldThrowBusinessException() {
        when(validationService.getMemberById("member-uuid-001")).thenReturn(member);
        BaseResponse<GetMemberWalletResponse> walletResp = new BaseResponse<>("VYRA-GWS-001", "failed", null);
        when(walletService.getMemberWallet("628123456789")).thenReturn(walletResp);

        assertThrows(BusinessException.class, () -> memberService.getMember("member-uuid-001"));
    }

    // =========================================================================
    // getMember — walletService throws BusinessException → rethrow
    // =========================================================================
    @Test
    void getMember_walletThrowsBusinessException_shouldRethrow() {
        when(validationService.getMemberById("member-uuid-001")).thenReturn(member);
        when(walletService.getMemberWallet("628123456789"))
                .thenThrow(new BusinessException(ErrorConstant.MEMBER_NOT_FOUND));

        assertThrows(BusinessException.class, () -> memberService.getMember("member-uuid-001"));
    }

    // =========================================================================
    // getMember — walletService throws unexpected exception → InternalException
    // =========================================================================
    @Test
    void getMember_walletThrowsUnexpectedException_shouldThrowInternalException() {
        when(validationService.getMemberById("member-uuid-001")).thenReturn(member);
        when(walletService.getMemberWallet("628123456789"))
                .thenThrow(new RuntimeException("timeout"));

        assertThrows(Exception.class, () -> memberService.getMember("member-uuid-001"));
    }

    // =========================================================================
    // updateProfile — no email/phone change
    // =========================================================================
    @Test
    void updateProfile_noEmailPhoneChange_shouldNotSyncWalletOrTransaction() {
        UpdateProfileRequest request = new UpdateProfileRequest();
        request.setFirstName("Andi");

        ValidateIsUpdateProfile isUpdate = new ValidateIsUpdateProfile();
        // isEmailChanged = false, isPhoneChanged = false
        when(validationService.getMemberById("member-uuid-001")).thenReturn(member);
        when(validationService.validateIsUpdateProfile(member, request)).thenReturn(isUpdate);
        doNothing().when(memberActivityService).createMemberActivity(anyString(), any());

        BaseResponse<UpdateProfileResponse> result = memberService.updateProfile("member-uuid-001", request);

        assertThat(result.getResponseStatus()).isEqualTo(ErrorConstant.UPDATE_PROFILE_SUCCESS.getCode());
        verify(walletService, never()).updateMemberWallet(any());
    }

    // =========================================================================
    // updateProfile — phone changed → sync wallet + transaction
    // =========================================================================
    @Test
    void updateProfile_phoneChanged_shouldSyncWalletAndTransaction() {
        UpdateProfileRequest request = new UpdateProfileRequest();
        request.setPhoneNumber("628999999999");

        ValidateIsUpdateProfile isUpdate = new ValidateIsUpdateProfile();
        isUpdate.setPhoneChanged(true);

        when(validationService.getMemberById("member-uuid-001")).thenReturn(member);
        when(validationService.validateIsUpdateProfile(member, request)).thenReturn(isUpdate);
        doNothing().when(walletService).updateMemberWallet(any(WalletUpdateRequest.class));
        doNothing().when(transactionService).updateTransaction(any(TransactionUpdateRequest.class));
        doNothing().when(memberActivityService).createMemberActivity(anyString(), any());

        BaseResponse<UpdateProfileResponse> result = memberService.updateProfile("member-uuid-001", request);

        assertThat(result.getResponseStatus()).isEqualTo(ErrorConstant.UPDATE_PROFILE_SUCCESS.getCode());
        verify(walletService).updateMemberWallet(any(WalletUpdateRequest.class));
        verify(transactionService).updateTransaction(any(TransactionUpdateRequest.class));
        // modifiedBy should use new phone number
        assertThat(member.getModifiedBy()).isEqualTo("628999999999");
    }

    // =========================================================================
    // updateProfile — email changed (no phone change) → only sync transaction
    // =========================================================================
    @Test
    void updateProfile_emailChangedOnly_shouldNotSyncWalletButSyncTransaction() {
        UpdateProfileRequest request = new UpdateProfileRequest();
        request.setEmail("new@example.com");

        ValidateIsUpdateProfile isUpdate = new ValidateIsUpdateProfile();
        isUpdate.setEmailChanged(true);

        when(validationService.getMemberById("member-uuid-001")).thenReturn(member);
        when(validationService.validateIsUpdateProfile(member, request)).thenReturn(isUpdate);
        doNothing().when(transactionService).updateTransaction(any(TransactionUpdateRequest.class));
        doNothing().when(memberActivityService).createMemberActivity(anyString(), any());

        memberService.updateProfile("member-uuid-001", request);

        verify(walletService, never()).updateMemberWallet(any());
        verify(transactionService).updateTransaction(any(TransactionUpdateRequest.class));
        // modifiedBy should use existing phone (no phone change)
        assertThat(member.getModifiedBy()).isEqualTo("628123456789");
    }

    // =========================================================================
    // changePin — success
    // =========================================================================
    @Test
    void changePin_success_shouldEncodeNewPinAndSaveViaActivity() {
        ChangePinRequest request = new ChangePinRequest();
        request.setOldPin("123456");
        request.setNewPin("654321");

        when(validationService.getMemberById("member-uuid-001")).thenReturn(member);
        when(passwordEncoder.matches("123456", "hashed-pin")).thenReturn(true);
        when(passwordEncoder.encode("654321")).thenReturn("new-hashed-pin");
        doNothing().when(memberActivityService).createMemberActivity(anyString(), any());

        BaseResponse<Void> result = memberService.changePin("member-uuid-001", request);

        assertThat(result.getResponseStatus()).isEqualTo(ErrorConstant.CHANGE_PIN_SUCCESS.getCode());
        assertThat(member.getPin()).isEqualTo("new-hashed-pin");
        assertThat(member.getUpdatedAt()).isNotNull();
        verify(memberActivityService).createMemberActivity(member.getPhoneNumber(), MemberActivityEvent.ATTEMPT_CHANGE_PIN);
        verify(memberActivityService).createMemberActivity(member.getPhoneNumber(), MemberActivityEvent.SUCCESS_CHANGE_PIN);
    }

    // =========================================================================
    // changePin — wrong old pin
    // =========================================================================
    @Test
    void changePin_wrongOldPin_shouldThrowInvalidPinMissMatch() {
        ChangePinRequest request = new ChangePinRequest();
        request.setOldPin("wrong");
        request.setNewPin("654321");

        when(validationService.getMemberById("member-uuid-001")).thenReturn(member);
        when(passwordEncoder.matches("wrong", "hashed-pin")).thenReturn(false);
        doNothing().when(memberActivityService).createMemberActivity(anyString(), any());

        BusinessException ex = assertThrows(BusinessException.class,
                () -> memberService.changePin("member-uuid-001", request));

        assertThat(ex.getErrorConstant()).isEqualTo(ErrorConstant.INVALID_PIN_MISS_MATCH);
        verify(memberActivityService).createMemberActivity(member.getPhoneNumber(), MemberActivityEvent.FAILED_CHANGE_PIN);
        verify(memberActivityService, never()).createMemberActivity(member.getPhoneNumber(), MemberActivityEvent.SUCCESS_CHANGE_PIN);
    }

    // =========================================================================
    // helpers
    // =========================================================================
    private GetMemberWalletResponse buildWalletResponse(BigDecimal income, BigDecimal expense, BigDecimal balance) {
        GetMemberWalletResponse r = new GetMemberWalletResponse();
        r.setPhoneNumber("628123456789");
        r.setTotalIncome(income);
        r.setTotalExpense(expense);
        r.setBalance(balance);
        return r;
    }
}
