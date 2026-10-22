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
import com.vyra.be_virtual_your_assets.exception.TooManyRequestsException;
import com.vyra.be_virtual_your_assets.repository.MemberActivityRepository;
import com.vyra.be_virtual_your_assets.repository.MemberTokenRepository;
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
import java.util.List;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import com.vyra.be_virtual_your_assets.dto.activity.MemberActivityResponse;
import com.vyra.be_virtual_your_assets.dto.common.PageResponse;
import com.vyra.be_virtual_your_assets.entity.MemberActivity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MemberServiceTest {

    @Mock private TransactionRepository transactionRepository;
    @Mock private MemberActivityRepository memberActivityRepository;
    @Mock private MemberActivityService memberActivityService;
    @Mock private TransactionService transactionService;
    @Mock private ValidationService validationService;
    @Mock private WalletService walletService;
    @Mock private BCryptPasswordEncoder passwordEncoder;
    @Mock private LockoutService lockoutService;
    @Mock private MemberTokenRepository memberTokenRepository;
    @Mock private ContactChangeService contactChangeService;

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
    // updateProfile — phone changed → only staged, OTP verification applies it
    // =========================================================================
    @Test
    void updateProfile_phoneChanged_shouldOnlyStageTheChangeAndLeaveTheIdentityUntouched() {
        UpdateProfileRequest request = new UpdateProfileRequest();
        request.setPhoneNumber("628999999999");
        request.setPin("123456");

        when(validationService.getMemberById("member-uuid-001")).thenReturn(member);
        when(passwordEncoder.matches("123456", "hashed-pin")).thenReturn(true);
        when(validationService.validateIsUpdateProfile(eq(member), any(UpdateProfileRequest.class)))
                .thenReturn(new ValidateIsUpdateProfile());
        when(contactChangeService.stage(member, null, "628999999999")).thenReturn("budi@example.com");

        BaseResponse<UpdateProfileResponse> result = memberService.updateProfile("member-uuid-001", request);

        assertThat(result.getResponseStatus()).isEqualTo(ErrorConstant.UPDATE_PROFILE_PENDING.getCode());
        assertThat(result.getData().isContactChangePending()).isTrue();
        assertThat(result.getData().getVerificationSentTo()).isEqualTo("b***@example.com");
        // still the old values: nothing is applied before the OTP is verified
        assertThat(result.getData().getPhoneNumber()).isEqualTo("628123456789");
        assertThat(member.getPhoneNumber()).isEqualTo("628123456789");
        verify(walletService, never()).updateMemberWallet(any());
        verify(transactionService, never()).updateTransaction(any());
    }

    // =========================================================================
    // updateProfile — email changed → staged, OTP goes to the NEW address
    // =========================================================================
    @Test
    void updateProfile_emailChanged_shouldStageNormalisedNewEmail() {
        UpdateProfileRequest request = new UpdateProfileRequest();
        request.setEmail("  New@Example.com ");
        request.setPin("123456");

        when(validationService.getMemberById("member-uuid-001")).thenReturn(member);
        when(passwordEncoder.matches("123456", "hashed-pin")).thenReturn(true);
        when(validationService.validateIsUpdateProfile(eq(member), any(UpdateProfileRequest.class)))
                .thenReturn(new ValidateIsUpdateProfile());
        when(contactChangeService.stage(member, "new@example.com", null)).thenReturn("new@example.com");

        BaseResponse<UpdateProfileResponse> result = memberService.updateProfile("member-uuid-001", request);

        assertThat(result.getResponseStatus()).isEqualTo(ErrorConstant.UPDATE_PROFILE_PENDING.getCode());
        assertThat(result.getData().getEmail()).isEqualTo("budi@example.com");
        assertThat(result.getData().getVerificationSentTo()).isEqualTo("n***@example.com");
        verify(contactChangeService).stage(member, "new@example.com", null);
    }

    @Test
    void updateProfile_nameAndEmailChanged_shouldSaveNamesNowAndStageOnlyTheContact() {
        UpdateProfileRequest request = new UpdateProfileRequest();
        request.setFirstName("Andi");
        request.setEmail("new@example.com");
        request.setPin("123456");

        when(validationService.getMemberById("member-uuid-001")).thenReturn(member);
        when(passwordEncoder.matches("123456", "hashed-pin")).thenReturn(true);
        when(validationService.validateIsUpdateProfile(eq(member), any(UpdateProfileRequest.class)))
                .thenReturn(new ValidateIsUpdateProfile());
        when(contactChangeService.stage(any(), any(), any())).thenReturn("new@example.com");

        memberService.updateProfile("member-uuid-001", request);

        org.mockito.ArgumentCaptor<UpdateProfileRequest> captor = org.mockito.ArgumentCaptor.forClass(UpdateProfileRequest.class);
        verify(validationService).validateIsUpdateProfile(eq(member), captor.capture());
        assertThat(captor.getValue().getFirstName()).isEqualTo("Andi");
        assertThat(captor.getValue().getEmail()).isNull();
        assertThat(captor.getValue().getPhoneNumber()).isNull();
    }

    @Test
    void maskEmail_shouldHideTheLocalPart() {
        assertThat(MemberService.maskEmail("budi@example.com")).isEqualTo("b***@example.com");
        assertThat(MemberService.maskEmail("a@example.com")).isEqualTo("***@example.com");
        assertThat(MemberService.maskEmail(null)).isNull();
    }

    // =========================================================================
    // updateProfile — contact change needs the current PIN
    // =========================================================================
    @Test
    void updateProfile_emailChangedWithoutPin_shouldRejectBeforeAnyChange() {
        UpdateProfileRequest request = new UpdateProfileRequest();
        request.setEmail("attacker@example.com");
        when(validationService.getMemberById("member-uuid-001")).thenReturn(member);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> memberService.updateProfile("member-uuid-001", request));

        assertThat(ex.getErrorConstant()).isEqualTo(ErrorConstant.INVALID_PIN_MISS_MATCH);
        assertThat(member.getEmail()).isEqualTo("budi@example.com");
        verify(validationService, never()).validateIsUpdateProfile(any(), any());
        verify(transactionService, never()).updateTransaction(any());
        verify(memberActivityService).createMemberActivity(member.getPhoneNumber(), MemberActivityEvent.FAILED_UPDATE_PROFILE);
    }

    @Test
    void updateProfile_phoneChangedWithWrongPin_shouldRejectBeforeAnyChange() {
        UpdateProfileRequest request = new UpdateProfileRequest();
        request.setPhoneNumber("628999999999");
        request.setPin("000000");
        when(validationService.getMemberById("member-uuid-001")).thenReturn(member);
        when(passwordEncoder.matches("000000", "hashed-pin")).thenReturn(false);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> memberService.updateProfile("member-uuid-001", request));

        assertThat(ex.getErrorConstant()).isEqualTo(ErrorConstant.INVALID_PIN_MISS_MATCH);
        assertThat(member.getPhoneNumber()).isEqualTo("628123456789");
        verify(walletService, never()).updateMemberWallet(any());
        verify(validationService, never()).validateIsUpdateProfile(any(), any());
    }

    @Test
    void updateProfile_nameOnlyChange_shouldNotRequirePin() {
        UpdateProfileRequest request = new UpdateProfileRequest();
        request.setFirstName("Andi");
        ValidateIsUpdateProfile isUpdate = new ValidateIsUpdateProfile();
        when(validationService.getMemberById("member-uuid-001")).thenReturn(member);
        when(validationService.validateIsUpdateProfile(member, request)).thenReturn(isUpdate);

        BaseResponse<UpdateProfileResponse> result = memberService.updateProfile("member-uuid-001", request);

        assertThat(result.getResponseStatus()).isEqualTo(ErrorConstant.UPDATE_PROFILE_SUCCESS.getCode());
        verify(passwordEncoder, never()).matches(any(), any());
    }

    @Test
    void updateProfile_sameEmailResent_shouldNotRequirePin() {
        UpdateProfileRequest request = new UpdateProfileRequest();
        request.setEmail("BUDI@example.com");
        ValidateIsUpdateProfile isUpdate = new ValidateIsUpdateProfile();
        isUpdate.setEmailChanged(true);
        when(validationService.getMemberById("member-uuid-001")).thenReturn(member);
        when(validationService.validateIsUpdateProfile(member, request)).thenReturn(isUpdate);

        memberService.updateProfile("member-uuid-001", request);

        verify(passwordEncoder, never()).matches(any(), any());
    }

    // =========================================================================
    // lock-out on the PIN checks
    // =========================================================================
    @Test
    void changePin_wrongOldPin_shouldRecordFailureAndPropagateLock() {
        ChangePinRequest request = new ChangePinRequest();
        request.setOldPin("000000");
        request.setNewPin("654321");
        when(validationService.getMemberById("member-uuid-001")).thenReturn(member);
        when(passwordEncoder.matches("000000", "hashed-pin")).thenReturn(false);
        doThrow(new TooManyRequestsException(ErrorConstant.ACCOUNT_LOCKED, 900))
                .when(lockoutService).onPinFailure(member);

        assertThrows(TooManyRequestsException.class, () -> memberService.changePin("member-uuid-001", request, "current-token"));

        assertThat(member.getPin()).isEqualTo("hashed-pin");
        verify(passwordEncoder, never()).encode(any());
    }

    @Test
    void changePin_lockedMember_shouldBeRefusedWithoutCheckingPin() {
        ChangePinRequest request = new ChangePinRequest();
        request.setOldPin("123456");
        request.setNewPin("654321");
        when(validationService.getMemberById("member-uuid-001")).thenReturn(member);
        doThrow(new TooManyRequestsException(ErrorConstant.ACCOUNT_LOCKED, 600))
                .when(lockoutService).assertPinNotLocked(member);

        assertThrows(TooManyRequestsException.class, () -> memberService.changePin("member-uuid-001", request, "current-token"));

        verify(passwordEncoder, never()).matches(any(), any());
    }

    @Test
    void changePin_correctOldPin_shouldClearFailureCounters() {
        ChangePinRequest request = new ChangePinRequest();
        request.setOldPin("123456");
        request.setNewPin("654321");
        when(validationService.getMemberById("member-uuid-001")).thenReturn(member);
        when(passwordEncoder.matches("123456", "hashed-pin")).thenReturn(true);
        when(passwordEncoder.encode("654321")).thenReturn("new-hashed-pin");

        memberService.changePin("member-uuid-001", request, "current-token");

        verify(lockoutService).onPinSuccess(member);
        verify(lockoutService, never()).onPinFailure(any());
    }

    @Test
    void changePin_success_shouldEndEveryOtherSessionButKeepTheCurrentOne() {
        ChangePinRequest request = new ChangePinRequest();
        request.setOldPin("123456");
        request.setNewPin("654321");
        when(validationService.getMemberById("member-uuid-001")).thenReturn(member);
        when(passwordEncoder.matches("123456", "hashed-pin")).thenReturn(true);
        when(passwordEncoder.encode("654321")).thenReturn("new-hashed-pin");

        memberService.changePin("member-uuid-001", request, "current-token");

        verify(memberTokenRepository).deleteByUserIdAndAccessTokenNot("member-uuid-001", "current-token");
        verify(memberTokenRepository, never()).deleteByUserId(any());
    }

    @Test
    void changePin_wrongOldPin_shouldNotRevokeAnySession() {
        ChangePinRequest request = new ChangePinRequest();
        request.setOldPin("000000");
        request.setNewPin("654321");
        when(validationService.getMemberById("member-uuid-001")).thenReturn(member);
        when(passwordEncoder.matches("000000", "hashed-pin")).thenReturn(false);

        assertThrows(BusinessException.class, () -> memberService.changePin("member-uuid-001", request, "current-token"));

        verifyNoInteractions(memberTokenRepository);
    }

    @Test
    void updateProfile_contactChangeWithWrongPin_shouldCountTowardsLockout() {
        UpdateProfileRequest request = new UpdateProfileRequest();
        request.setEmail("attacker@example.com");
        request.setPin("000000");
        when(validationService.getMemberById("member-uuid-001")).thenReturn(member);
        when(passwordEncoder.matches("000000", "hashed-pin")).thenReturn(false);

        assertThrows(BusinessException.class, () -> memberService.updateProfile("member-uuid-001", request));

        verify(lockoutService).onPinFailure(member);
    }

    @Test
    void updateProfile_contactChangeWhileLocked_shouldBeRefusedWithoutCheckingPin() {
        UpdateProfileRequest request = new UpdateProfileRequest();
        request.setEmail("attacker@example.com");
        request.setPin("123456");
        when(validationService.getMemberById("member-uuid-001")).thenReturn(member);
        doThrow(new TooManyRequestsException(ErrorConstant.ACCOUNT_LOCKED, 600))
                .when(lockoutService).assertPinNotLocked(member);

        assertThrows(TooManyRequestsException.class, () -> memberService.updateProfile("member-uuid-001", request));

        verify(passwordEncoder, never()).matches(any(), any());
        verify(validationService, never()).validateIsUpdateProfile(any(), any());
    }

    @Test
    void updateProfile_nameOnlyChange_shouldNotTouchLockoutAtAll() {
        UpdateProfileRequest request = new UpdateProfileRequest();
        request.setFirstName("Andi");
        when(validationService.getMemberById("member-uuid-001")).thenReturn(member);
        when(validationService.validateIsUpdateProfile(member, request)).thenReturn(new ValidateIsUpdateProfile());

        memberService.updateProfile("member-uuid-001", request);

        verifyNoInteractions(lockoutService);
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

        BaseResponse<Void> result = memberService.changePin("member-uuid-001", request, "current-token");

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
                () -> memberService.changePin("member-uuid-001", request, "current-token"));

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


    // =========================================================================
    // getMemberActivity
    // =========================================================================
    @Test
    void getMemberActivity_shouldMapItemsAndClampPaging() {
        MemberActivity activity = new MemberActivity();
        activity.setId("act-1");
        activity.setPhoneNumber("628123456789");
        activity.setEvent("SUCCESS_LOGIN");
        activity.setDescription("Member successfully logged in");
        activity.setCreatedAt(LocalDateTime.of(2026, 6, 1, 9, 0));

        when(validationService.getMemberById("member-uuid-001")).thenReturn(member);
        when(memberActivityRepository.findByPhoneNumber(eq("628123456789"), any(PageRequest.class)))
                .thenReturn(new PageImpl<>(List.of(activity), PageRequest.of(0, 50), 1));

        BaseResponse<PageResponse<MemberActivityResponse>> result =
                memberService.getMemberActivity("member-uuid-001", -5, 1000);

        assertThat(result.getResponseStatus()).isEqualTo(ErrorConstant.GET_MEMBER_ACTIVITY_SUCCESS.getCode());
        assertThat(result.getData().getContent()).hasSize(1);
        assertThat(result.getData().getContent().get(0).getEvent()).isEqualTo("SUCCESS_LOGIN");
        verify(memberActivityRepository).findByPhoneNumber("628123456789", PageRequest.of(0, 50));
    }
}
