package com.vyra.be_virtual_your_assets.service;

import com.vyra.be_virtual_your_assets.config.SecurityPolicy;
import com.vyra.be_virtual_your_assets.constant.ActiveStatus;
import com.vyra.be_virtual_your_assets.constant.ErrorConstant;
import com.vyra.be_virtual_your_assets.constant.MemberActivityEvent;
import com.vyra.be_virtual_your_assets.constant.OtpType;
import com.vyra.be_virtual_your_assets.dto.auth.VerifyOtpRequest;
import com.vyra.be_virtual_your_assets.entity.Member;
import com.vyra.be_virtual_your_assets.entity.MemberOtp;
import com.vyra.be_virtual_your_assets.exception.BusinessException;
import com.vyra.be_virtual_your_assets.exception.TooManyRequestsException;
import com.vyra.be_virtual_your_assets.repository.MemberOtpRepository;
import com.vyra.be_virtual_your_assets.repository.MemberRepository;
import com.vyra.be_virtual_your_assets.repository.MemberTokenRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AccountDeletionServiceTest {

    private static final String MEMBER = "member-1";
    private static final String PHONE = "628123";

    @Mock private MemberRepository memberRepository;
    @Mock private MemberOtpRepository memberOtpRepository;
    @Mock private MemberTokenRepository memberTokenRepository;
    @Mock private ValidationService validationService;
    @Mock private LockoutService lockoutService;
    @Mock private OtpService otpService;
    @Mock private MemberActivityService memberActivityService;
    @Mock private BCryptPasswordEncoder passwordEncoder;

    private AccountDeletionService service;
    private Member member;

    @BeforeEach
    void setUp() {
        TimeService timeService = new TimeService(
                Clock.fixed(Instant.parse("2026-10-24T02:00:00Z"), ZoneOffset.UTC), "Asia/Jakarta");
        service = new AccountDeletionService(memberRepository, memberOtpRepository, memberTokenRepository,
                validationService, lockoutService, otpService, memberActivityService, passwordEncoder,
                new SecurityPolicy(), timeService);
        member = new Member();
        member.setId(MEMBER);
        member.setFirstName("Azhar");
        member.setLastName("Haikal");
        member.setEmail("a@b.c");
        member.setPhoneNumber(PHONE);
        member.setPin("hashed-pin");
        member.setStatus(ActiveStatus.ACTIVE);
        when(validationService.getMemberById(MEMBER)).thenReturn(member);
        lenient().when(passwordEncoder.encode(anyString())).thenReturn("hashed-otp");
    }

    @Test
    void request_rightPin_emailsADeleteAccountOtp_andChangesNothingYet() {
        when(passwordEncoder.matches("123456", "hashed-pin")).thenReturn(true);
        when(memberOtpRepository.findTopByPhoneNumberAndOtpTypeOrderByCreatedAtDesc(PHONE, OtpType.DELETE_ACCOUNT))
                .thenReturn(Optional.empty());

        var result = service.request(MEMBER, "123456");

        assertThat(result.getResponseStatus()).isEqualTo(ErrorConstant.ACCOUNT_DELETE_OTP_SENT.getCode());
        assertThat(result.getData().getEmail()).isEqualTo("a@b.c");
        ArgumentCaptor<MemberOtp> otp = ArgumentCaptor.forClass(MemberOtp.class);
        verify(memberOtpRepository).save(otp.capture());
        assertThat(otp.getValue().getOtpType()).isEqualTo(OtpType.DELETE_ACCOUNT);
        verify(otpService).sendOtp(eq("Azhar Haikal"), eq("a@b.c"), eq(PHONE), anyString());
        verify(lockoutService).onPinSuccess(member);
        assertThat(member.getStatus()).isEqualTo(ActiveStatus.ACTIVE);
        verify(memberTokenRepository, never()).deleteByUserId(any());
    }

    @Test
    void request_wrongPin_countsTowardsTheLockout_andSendsNothing() {
        when(passwordEncoder.matches("000000", "hashed-pin")).thenReturn(false);

        BusinessException ex = assertThrows(BusinessException.class, () -> service.request(MEMBER, "000000"));

        assertThat(ex.getErrorConstant()).isEqualTo(ErrorConstant.INVALID_PIN_MISS_MATCH);
        verify(lockoutService).onPinFailure(member);
        verify(memberActivityService).createMemberActivity(PHONE, MemberActivityEvent.FAILED_DELETE_ACCOUNT);
        verify(otpService, never()).sendOtp(any(), any(), any(), any());
    }

    @Test
    void request_withinTheCooldown_is429() {
        when(passwordEncoder.matches("123456", "hashed-pin")).thenReturn(true);
        MemberOtp recent = new MemberOtp();
        recent.setCreatedAt(LocalDateTime.now());
        when(memberOtpRepository.findTopByPhoneNumberAndOtpTypeOrderByCreatedAtDesc(PHONE, OtpType.DELETE_ACCOUNT))
                .thenReturn(Optional.of(recent));

        assertThrows(TooManyRequestsException.class, () -> service.request(MEMBER, "123456"));
        verify(otpService, never()).sendOtp(any(), any(), any(), any());
    }

    @Test
    void resend_withoutAPendingRequest_isRejected() {
        when(memberOtpRepository.findTopByPhoneNumberAndOtpTypeOrderByCreatedAtDesc(PHONE, OtpType.DELETE_ACCOUNT))
                .thenReturn(Optional.empty());

        BusinessException ex = assertThrows(BusinessException.class, () -> service.resend(MEMBER));
        assertThat(ex.getErrorConstant()).isEqualTo(ErrorConstant.OTP_NOT_FOUND);
    }

    @Test
    void confirm_rightOtp_softDeletesTheAccount_andEndsEverySession() {
        MemberOtp otp = new MemberOtp();
        when(validationService.verifyOtp(eq(member), any(VerifyOtpRequest.class))).thenReturn(otp);

        var result = service.confirm(MEMBER, "654321");

        ArgumentCaptor<VerifyOtpRequest> verification = ArgumentCaptor.forClass(VerifyOtpRequest.class);
        verify(validationService).verifyOtp(eq(member), verification.capture());
        assertThat(verification.getValue().getOtpType()).isEqualTo(OtpType.DELETE_ACCOUNT);
        assertThat(result.getResponseStatus()).isEqualTo(ErrorConstant.ACCOUNT_DELETE_SUCCESS.getCode());
        assertThat(member.getStatus()).isEqualTo(ActiveStatus.DELETED);
        assertThat(member.getDeletedAt()).isEqualTo(LocalDateTime.of(2026, 10, 24, 2, 0));
        verify(memberRepository).save(member);
        verify(memberOtpRepository).delete(otp);
        verify(memberTokenRepository).deleteByUserId(MEMBER);
        verify(memberActivityService).createMemberActivity(PHONE, MemberActivityEvent.SUCCESS_DELETE_ACCOUNT);
    }

    @Test
    void confirm_wrongOtp_keepsTheAccount() {
        doThrow(new BusinessException(ErrorConstant.OTP_INVALID, "3"))
                .when(validationService).verifyOtp(eq(member), any(VerifyOtpRequest.class));

        assertThrows(BusinessException.class, () -> service.confirm(MEMBER, "111111"));

        assertThat(member.getStatus()).isEqualTo(ActiveStatus.ACTIVE);
        verify(memberRepository, never()).save(any());
        verify(memberTokenRepository, never()).deleteByUserId(any());
        verify(memberActivityService).createMemberActivity(PHONE, MemberActivityEvent.FAILED_DELETE_ACCOUNT);
    }

    @Test
    void anAccountThatIsNotActive_cannotStartOrConfirm() {
        member.setStatus(ActiveStatus.SUSPENDED);

        BusinessException request = assertThrows(BusinessException.class, () -> service.request(MEMBER, "123456"));
        BusinessException confirm = assertThrows(BusinessException.class, () -> service.confirm(MEMBER, "123456"));

        assertThat(request.getErrorConstant()).isEqualTo(ErrorConstant.MEMBER_NOT_ACTIVE);
        assertThat(confirm.getErrorConstant()).isEqualTo(ErrorConstant.MEMBER_NOT_ACTIVE);
    }
}
