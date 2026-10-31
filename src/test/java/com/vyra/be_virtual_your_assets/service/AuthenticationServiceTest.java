package com.vyra.be_virtual_your_assets.service;

import com.vyra.be_virtual_your_assets.config.SecurityPolicy;
import com.vyra.be_virtual_your_assets.constant.ActiveStatus;
import com.vyra.be_virtual_your_assets.constant.ErrorConstant;
import com.vyra.be_virtual_your_assets.constant.MemberActivityEvent;
import com.vyra.be_virtual_your_assets.constant.OtpType;
import com.vyra.be_virtual_your_assets.dto.BaseResponse;
import com.vyra.be_virtual_your_assets.dto.auth.*;
import com.vyra.be_virtual_your_assets.dto.wallet.CreateWalletRequest;
import com.vyra.be_virtual_your_assets.dto.wallet.CreateWalletResponse;
import com.vyra.be_virtual_your_assets.entity.Member;
import com.vyra.be_virtual_your_assets.entity.MemberOtp;
import com.vyra.be_virtual_your_assets.entity.MemberToken;
import com.vyra.be_virtual_your_assets.exception.BadRequestException;
import com.vyra.be_virtual_your_assets.exception.BusinessException;
import com.vyra.be_virtual_your_assets.exception.TooManyRequestsException;
import com.vyra.be_virtual_your_assets.repository.MemberOtpRepository;
import com.vyra.be_virtual_your_assets.repository.MemberRepository;
import com.vyra.be_virtual_your_assets.repository.MemberTokenRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthenticationServiceTest {

    @Mock private MemberRepository memberRepository;
    @Mock private MemberOtpRepository memberOtpRepository;
    @Mock private MemberTokenRepository memberTokenRepository;
    @Mock private MemberActivityService memberActivityService;
    @Mock private OtpService otpService;
    @Mock private ValidationService validationService;
    @Mock private WalletService walletService;
    @Mock private BCryptPasswordEncoder passwordEncoder;
    @Mock private LockoutService lockoutService;
    @Spy private TimeService timeService = new TimeService(Clock.fixed(Instant.parse("2026-10-22T17:30:00Z"), ZoneOffset.UTC), "Asia/Jakarta");
    @Spy private SecurityPolicy securityPolicy = new SecurityPolicy();

    @InjectMocks
    private AuthenticationService authenticationService;

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
        member.setStatus(ActiveStatus.INACTIVE);
    }

    // =========================================================================
    // registerMember — success
    // =========================================================================
    @Test
    void registerMember_success_shouldSaveMemberAndReturnRegisterResponse() {
        RegisterRequest request = buildRegisterRequest();
        when(passwordEncoder.encode("123456")).thenReturn("hashed-pin");

        CreateWalletResponse walletData = new CreateWalletResponse();
        walletData.setMemberId("member-uuid-001");
        BaseResponse<CreateWalletResponse> walletResp = new BaseResponse<>(
                ErrorConstant.CREATE_WALLET_SUCCESS.getCode(), "ok", walletData);
        when(walletService.createWallet(any(CreateWalletRequest.class))).thenReturn(walletResp);

        doNothing().when(validationService).validateDataExistRegister(anyString(), anyString());
        when(memberRepository.save(any(Member.class))).thenReturn(member);
        when(memberOtpRepository.save(any(MemberOtp.class))).thenReturn(new MemberOtp());
        doNothing().when(otpService).sendOtp(anyString(), anyString(), anyString(), anyString());
        doNothing().when(memberActivityService).createMemberActivity(anyString(), any());

        BaseResponse<RegisterResponse> result = authenticationService.registerMember(request);

        assertThat(result.getResponseStatus()).isEqualTo(ErrorConstant.REGISTER_SUCCESS.getCode());
        assertThat(result.getData().getFullName()).isEqualTo("Budi Santoso");
        verify(memberRepository).save(any(Member.class));
        verify(walletService).createWallet(any(CreateWalletRequest.class));
        verify(otpService).sendOtp(anyString(), anyString(), anyString(), anyString());
    }

    @Test
    void registerMember_withTimezone_shouldStoreItAndDefaultOtherwise() {
        CreateWalletResponse walletData = new CreateWalletResponse();
        walletData.setMemberId("member-uuid-001");
        when(walletService.createWallet(any(CreateWalletRequest.class))).thenReturn(
                new BaseResponse<>(ErrorConstant.CREATE_WALLET_SUCCESS.getCode(), "ok", walletData));
        when(passwordEncoder.encode("123456")).thenReturn("hashed-pin");
        org.mockito.ArgumentCaptor<Member> saved = org.mockito.ArgumentCaptor.forClass(Member.class);
        when(memberRepository.save(saved.capture())).thenAnswer(inv -> inv.getArgument(0));

        RegisterRequest withZone = buildRegisterRequest();
        withZone.setTimezone("Asia/Jayapura");
        authenticationService.registerMember(withZone);
        RegisterRequest withoutZone = buildRegisterRequest();
        authenticationService.registerMember(withoutZone);

        assertThat(saved.getAllValues().get(0).getTimezone()).isEqualTo("Asia/Jayapura");
        assertThat(saved.getAllValues().get(1).getTimezone()).isEqualTo("Asia/Jakarta");
    }

    @Test
    void registerMember_invalidTimezone_shouldThrowBeforeSaving() {
        RegisterRequest request = buildRegisterRequest();
        request.setTimezone("+07:00");

        BadRequestException ex = assertThrows(BadRequestException.class,
                () -> authenticationService.registerMember(request));

        assertThat(ex.getErrorConstant()).isEqualTo(ErrorConstant.INVALID_TIMEZONE);
        verify(memberRepository, never()).save(any());
    }

    // =========================================================================
    // registerMember — wallet returns non-success code → BusinessException
    // =========================================================================
    @Test
    void registerMember_walletResponseCodeNotSuccess_shouldThrowBusinessExceptionAndLogFailed() {
        RegisterRequest request = buildRegisterRequest();
        when(passwordEncoder.encode("123456")).thenReturn("hashed-pin");
        doNothing().when(validationService).validateDataExistRegister(anyString(), anyString());
        when(memberRepository.save(any(Member.class))).thenReturn(member);
        when(memberOtpRepository.save(any(MemberOtp.class))).thenReturn(new MemberOtp());
        doNothing().when(memberActivityService).createMemberActivity(anyString(), any());

        BaseResponse<CreateWalletResponse> walletResp = new BaseResponse<>("VYRA-WLT-001", "failed", null);
        when(walletService.createWallet(any(CreateWalletRequest.class))).thenReturn(walletResp);

        assertThrows(BusinessException.class, () -> authenticationService.registerMember(request));

        verify(memberActivityService).createMemberActivity(request.getPhoneNumber(), MemberActivityEvent.FAILED_REGISTER);
    }

    // =========================================================================
    // registerMember — walletService throws BusinessException → rethrow + FAILED_REGISTER
    // =========================================================================
    @Test
    void registerMember_walletThrowsBusinessException_shouldRethrowAndRecordFailedActivity() {
        RegisterRequest request = buildRegisterRequest();
        when(passwordEncoder.encode("123456")).thenReturn("hashed-pin");
        doNothing().when(validationService).validateDataExistRegister(anyString(), anyString());
        when(memberRepository.save(any(Member.class))).thenReturn(member);
        when(memberOtpRepository.save(any(MemberOtp.class))).thenReturn(new MemberOtp());
        doNothing().when(memberActivityService).createMemberActivity(anyString(), any());
        when(walletService.createWallet(any())).thenThrow(new BusinessException(ErrorConstant.CREATE_WALLET_FAILED));

        assertThrows(BusinessException.class, () -> authenticationService.registerMember(request));
        verify(memberActivityService).createMemberActivity(request.getPhoneNumber(), MemberActivityEvent.FAILED_REGISTER);
    }

    // =========================================================================
    // registerMember — walletService throws unexpected Exception → InternalException
    // =========================================================================
    @Test
    void registerMember_walletThrowsUnexpectedException_shouldThrowInternalExceptionAndRecordFailedActivity() {
        RegisterRequest request = buildRegisterRequest();
        when(passwordEncoder.encode("123456")).thenReturn("hashed-pin");
        doNothing().when(validationService).validateDataExistRegister(anyString(), anyString());
        when(memberRepository.save(any(Member.class))).thenReturn(member);
        when(memberOtpRepository.save(any(MemberOtp.class))).thenReturn(new MemberOtp());
        doNothing().when(memberActivityService).createMemberActivity(anyString(), any());
        when(walletService.createWallet(any())).thenThrow(new RuntimeException("DB connection lost"));

        assertThrows(Exception.class, () -> authenticationService.registerMember(request));
        verify(memberActivityService).createMemberActivity(request.getPhoneNumber(), MemberActivityEvent.FAILED_REGISTER);
    }

    // =========================================================================
    // resendOtp
    // =========================================================================
    @Test
    void resendOtp_success_shouldDeleteOldOtpAndSendNewOne() {
        ResendOtpRequest request = new ResendOtpRequest();
        request.setEmail("budi@example.com");
        request.setOtpType(OtpType.REGISTER);
        member.setStatus(ActiveStatus.INACTIVE);
        when(validationService.getMemberByEmailIgnoreCase("budi@example.com")).thenReturn(member);
        doNothing().when(memberOtpRepository).deleteByPhoneNumberAndOtpType(anyString(), any());
        when(memberOtpRepository.save(any())).thenReturn(new MemberOtp());
        doNothing().when(otpService).sendOtp(anyString(), anyString(), anyString(), anyString());
        doNothing().when(memberActivityService).createMemberActivity(anyString(), any());

        BaseResponse<Void> result = authenticationService.resendOtp(request);

        assertThat(result.getResponseStatus()).isEqualTo(ErrorConstant.RESEND_OTP.getCode());
        verify(memberOtpRepository).deleteByPhoneNumberAndOtpType(member.getPhoneNumber(), OtpType.REGISTER);
        verify(otpService).sendOtp(anyString(), anyString(), anyString(), anyString());
    }

    // =========================================================================
    // verifyOtp
    // =========================================================================
    @Test
    void verifyOtp_success_shouldActivateMemberAndDeleteOtp() {
        VerifyOtpRequest request = new VerifyOtpRequest();
        request.setEmail("budi@example.com");
        request.setOtpCode("123456");
        request.setOtpType(OtpType.REGISTER);

        MemberOtp otp = new MemberOtp();
        when(validationService.getMemberByEmailIgnoreCase("budi@example.com")).thenReturn(member);
        when(validationService.verifyOtp(member, request)).thenReturn(otp);
        when(memberRepository.save(member)).thenReturn(member);
        doNothing().when(memberOtpRepository).delete(otp);
        doNothing().when(memberActivityService).createMemberActivity(anyString(), any());

        BaseResponse<Void> result = authenticationService.verifyOtp(request);

        assertThat(result.getResponseStatus()).isEqualTo(ErrorConstant.VERIFY_OTP_SUCCESS.getCode());
        assertThat(member.getStatus()).isEqualTo(ActiveStatus.ACTIVE);
        verify(memberRepository).save(member);
        verify(memberOtpRepository).delete(otp);
    }

    // =========================================================================
    // loginMember — success
    // =========================================================================
    @Test
    void loginMember_success_shouldSaveTokenAndReturnLoginResponse() {
        member.setStatus(ActiveStatus.ACTIVE);
        LoginRequest request = buildLoginRequest();
        when(validationService.getMemberByEmailOrPhoneNumber("budi@example.com", null)).thenReturn(member);
        when(passwordEncoder.matches("123456", "hashed-pin")).thenReturn(true);
        when(memberTokenRepository.save(any(MemberToken.class))).thenReturn(new MemberToken());
        doNothing().when(memberActivityService).createMemberActivity(anyString(), any());

        BaseResponse<LoginResponse> result = authenticationService.loginMember(request);

        assertThat(result.getResponseStatus()).isEqualTo(ErrorConstant.LOGIN_SUCCESS.getCode());
        assertThat(result.getData().getEmail()).isEqualTo("budi@example.com");
        assertThat(result.getData().getAccessToken()).isNotBlank();
        assertThat(result.getData().getRefreshToken()).isNotBlank();
        verify(memberTokenRepository).save(any(MemberToken.class));
    }

    @Test
    void loginMember_withoutDeviceName_shouldStoreUnknownDeviceInsteadOfFailingOnTheNotNullColumn() {
        member.setStatus(ActiveStatus.ACTIVE);
        LoginRequest request = buildLoginRequest();
        request.setDeviceName("  ");
        when(validationService.getMemberByEmailOrPhoneNumber("budi@example.com", null)).thenReturn(member);
        when(passwordEncoder.matches("123456", "hashed-pin")).thenReturn(true);
        ArgumentCaptor<MemberToken> saved = ArgumentCaptor.forClass(MemberToken.class);
        when(memberTokenRepository.save(saved.capture())).thenReturn(new MemberToken());

        authenticationService.loginMember(request);

        assertThat(saved.getValue().getDeviceName()).isEqualTo(AuthenticationService.UNKNOWN_DEVICE);
    }

    // =========================================================================
    // loginMember — member not ACTIVE
    // =========================================================================
    @Test
    void loginMember_memberNotActive_shouldThrowMemberNotActive() {
        member.setStatus(ActiveStatus.INACTIVE);
        LoginRequest request = buildLoginRequest();
        when(validationService.getMemberByEmailOrPhoneNumber("budi@example.com", null)).thenReturn(member);
        doNothing().when(memberActivityService).createMemberActivity(anyString(), any());

        BusinessException ex = assertThrows(BusinessException.class,
                () -> authenticationService.loginMember(request));

        assertThat(ex.getErrorConstant()).isEqualTo(ErrorConstant.MEMBER_NOT_ACTIVE);
        verify(memberActivityService).createMemberActivity(member.getPhoneNumber(), MemberActivityEvent.FAILED_LOGIN);
    }

    @Test
    void loginMember_memberSuspended_shouldThrowMemberNotActive() {
        member.setStatus(ActiveStatus.SUSPENDED);
        LoginRequest request = buildLoginRequest();
        when(validationService.getMemberByEmailOrPhoneNumber("budi@example.com", null)).thenReturn(member);
        doNothing().when(memberActivityService).createMemberActivity(anyString(), any());

        BusinessException ex = assertThrows(BusinessException.class,
                () -> authenticationService.loginMember(request));

        assertThat(ex.getErrorConstant()).isEqualTo(ErrorConstant.MEMBER_NOT_ACTIVE);
    }

    // =========================================================================
    // loginMember — wrong PIN
    // =========================================================================
    @Test
    void loginMember_wrongPin_shouldThrowInvalidPin() {
        member.setStatus(ActiveStatus.ACTIVE);
        LoginRequest request = buildLoginRequest();
        when(validationService.getMemberByEmailOrPhoneNumber("budi@example.com", null)).thenReturn(member);
        when(passwordEncoder.matches("123456", "hashed-pin")).thenReturn(false);
        doNothing().when(memberActivityService).createMemberActivity(anyString(), any());

        BusinessException ex = assertThrows(BusinessException.class,
                () -> authenticationService.loginMember(request));

        assertThat(ex.getErrorConstant()).isEqualTo(ErrorConstant.INVALID_PIN);
        verify(memberActivityService).createMemberActivity(member.getPhoneNumber(), MemberActivityEvent.FAILED_LOGIN);
    }

    // =========================================================================
    // refreshToken — success
    // =========================================================================
    @Test
    void refreshToken_validToken_shouldReturnNewTokens() {
        RefreshTokenRequest request = new RefreshTokenRequest();
        request.setRefreshToken("valid-refresh");

        MemberToken token = new MemberToken();
        token.setUserId("member-uuid-001");
        token.setRefreshTokenExpiredAt(LocalDateTime.now().plusDays(1));
        member.setStatus(ActiveStatus.ACTIVE);
        when(memberTokenRepository.findByRefreshToken("valid-refresh")).thenReturn(Optional.of(token));
        when(memberRepository.findById("member-uuid-001")).thenReturn(Optional.of(member));
        lenient().when(memberTokenRepository.save(any())).thenReturn(token);

        BaseResponse<RefreshTokenResponse> result = authenticationService.refreshToken(request);

        assertThat(result.getResponseStatus()).isEqualTo(ErrorConstant.REFRESH_TOKEN_SUCCESS.getCode());
        assertThat(result.getData().getAccessToken()).isNotBlank();
        assertThat(result.getData().getRefreshToken()).isNotBlank();
    }

    @Test
    void refreshToken_suspendedMember_shouldRefuseAndRemoveTheToken() {
        RefreshTokenRequest request = new RefreshTokenRequest();
        request.setRefreshToken("valid-refresh");
        MemberToken token = new MemberToken();
        token.setUserId("member-uuid-001");
        token.setRefreshTokenExpiredAt(LocalDateTime.now().plusDays(20));
        member.setStatus(ActiveStatus.SUSPENDED);
        when(memberTokenRepository.findByRefreshToken("valid-refresh")).thenReturn(Optional.of(token));
        when(memberRepository.findById("member-uuid-001")).thenReturn(Optional.of(member));

        BusinessException ex = assertThrows(BusinessException.class, () -> authenticationService.refreshToken(request));

        assertThat(ex.getErrorConstant()).isEqualTo(ErrorConstant.MEMBER_NOT_ACTIVE);
        verify(memberTokenRepository).delete(token);
    }

    @Test
    void refreshToken_deletedMember_shouldRefuse() {
        RefreshTokenRequest request = new RefreshTokenRequest();
        request.setRefreshToken("valid-refresh");
        MemberToken token = new MemberToken();
        token.setUserId("gone");
        token.setRefreshTokenExpiredAt(LocalDateTime.now().plusDays(20));
        when(memberTokenRepository.findByRefreshToken("valid-refresh")).thenReturn(Optional.of(token));
        when(memberRepository.findById("gone")).thenReturn(Optional.empty());

        assertThrows(BusinessException.class, () -> authenticationService.refreshToken(request));

        verify(memberTokenRepository).delete(token);
    }

    // =========================================================================
    // refreshToken — token not found
    // =========================================================================
    @Test
    void refreshToken_tokenNotFound_shouldThrowInvalidRefreshToken() {
        RefreshTokenRequest request = new RefreshTokenRequest();
        request.setRefreshToken("unknown-token");
        when(memberTokenRepository.findByRefreshToken("unknown-token")).thenReturn(Optional.empty());

        BusinessException ex = assertThrows(BusinessException.class,
                () -> authenticationService.refreshToken(request));

        assertThat(ex.getErrorConstant()).isEqualTo(ErrorConstant.INVALID_REFRESH_TOKEN);
    }

    // =========================================================================
    // refreshToken — token expired
    // =========================================================================
    @Test
    void refreshToken_tokenExpired_shouldThrowRefreshTokenExpired() {
        RefreshTokenRequest request = new RefreshTokenRequest();
        request.setRefreshToken("expired-refresh");

        MemberToken token = new MemberToken();
        token.setRefreshTokenExpiredAt(LocalDateTime.now().minusMinutes(1));
        when(memberTokenRepository.findByRefreshToken("expired-refresh")).thenReturn(Optional.of(token));

        BusinessException ex = assertThrows(BusinessException.class,
                () -> authenticationService.refreshToken(request));

        assertThat(ex.getErrorConstant()).isEqualTo(ErrorConstant.REFRESH_TOKEN_EXPIRED);
    }

    // =========================================================================
    // forgotPin
    // =========================================================================
    @Test
    void forgotPin_success_shouldGenerateOtpAndSendToEmail() {
        ForgotPinRequest request = new ForgotPinRequest();
        request.setEmail("budi@example.com");
        member.setStatus(ActiveStatus.ACTIVE);
        when(validationService.getMemberByEmailIgnoreCase("budi@example.com")).thenReturn(member);
        when(memberOtpRepository.save(any())).thenReturn(new MemberOtp());
        doNothing().when(otpService).sendOtp(anyString(), anyString(), anyString(), anyString());
        doNothing().when(memberActivityService).createMemberActivity(anyString(), any());

        BaseResponse<Void> result = authenticationService.forgotPin(request);

        assertThat(result.getResponseStatus()).isEqualTo(ErrorConstant.FORGOT_PIN_OTP_SENT.getCode());
        verify(memberOtpRepository).deleteByPhoneNumberAndOtpType(member.getPhoneNumber(), OtpType.FORGOT_PIN);
        verify(otpService).sendOtp(anyString(), anyString(), anyString(), anyString());
        verify(memberActivityService).createMemberActivity(member.getPhoneNumber(), MemberActivityEvent.ATTEMPT_GENERATE_FORGOT_PIN);
        verify(memberActivityService).createMemberActivity(member.getPhoneNumber(), MemberActivityEvent.SUCCESS_GENERATE_FORGOT_PIN);
    }

    // =========================================================================
    // resetPin
    // =========================================================================
    @Test
    void resetPin_verifiedOtp_shouldEncodeNewPinConsumeOtpAndRevokeSessions() {
        member.setStatus(ActiveStatus.ACTIVE);
        ResetPinRequest request = resetPinRequest();
        MemberOtp otp = verifiedOtp(LocalDateTime.now().plusMinutes(3));
        when(validationService.getMemberByEmailOrPhoneNumber("budi@example.com", null)).thenReturn(member);
        when(memberOtpRepository.findTopByPhoneNumberAndOtpTypeOrderByCreatedAtDesc(member.getPhoneNumber(), OtpType.FORGOT_PIN))
                .thenReturn(Optional.of(otp));
        when(passwordEncoder.encode("654321")).thenReturn("new-hashed-pin");
        when(memberRepository.save(member)).thenReturn(member);

        BaseResponse<Void> result = authenticationService.resetPin(request);

        assertThat(result.getResponseStatus()).isEqualTo(ErrorConstant.RESET_PIN_SUCCESS.getCode());
        assertThat(member.getPin()).isEqualTo("new-hashed-pin");
        verify(memberRepository).save(member);
        verify(memberOtpRepository).delete(otp);
        verify(memberTokenRepository).deleteByUserId("member-uuid-001");
    }

    @Test
    void resetPin_noOtpAtAll_shouldRejectAndKeepPin() {
        member.setStatus(ActiveStatus.ACTIVE);
        when(validationService.getMemberByEmailOrPhoneNumber("budi@example.com", null)).thenReturn(member);
        when(memberOtpRepository.findTopByPhoneNumberAndOtpTypeOrderByCreatedAtDesc(member.getPhoneNumber(), OtpType.FORGOT_PIN))
                .thenReturn(Optional.empty());

        BusinessException ex = assertThrows(BusinessException.class, () -> authenticationService.resetPin(resetPinRequest()));

        assertThat(ex.getErrorConstant()).isEqualTo(ErrorConstant.OTP_NOT_FOUND);
        assertPinNotChanged();
    }

    @Test
    void resetPin_otpIssuedButNeverVerified_shouldRejectAndKeepPin() {
        member.setStatus(ActiveStatus.ACTIVE);
        MemberOtp otp = new MemberOtp();
        otp.setExpiredAt(LocalDateTime.now().plusMinutes(3));
        otp.setVerifiedAt(null);
        when(validationService.getMemberByEmailOrPhoneNumber("budi@example.com", null)).thenReturn(member);
        when(memberOtpRepository.findTopByPhoneNumberAndOtpTypeOrderByCreatedAtDesc(member.getPhoneNumber(), OtpType.FORGOT_PIN))
                .thenReturn(Optional.of(otp));

        BusinessException ex = assertThrows(BusinessException.class, () -> authenticationService.resetPin(resetPinRequest()));

        assertThat(ex.getErrorConstant()).isEqualTo(ErrorConstant.OTP_NOT_FOUND);
        assertPinNotChanged();
        verify(memberOtpRepository, never()).delete(any(MemberOtp.class));
    }

    @Test
    void resetPin_verifiedOtpPastResetWindow_shouldRejectAndKeepPin() {
        member.setStatus(ActiveStatus.ACTIVE);
        MemberOtp otp = verifiedOtp(LocalDateTime.now().minusSeconds(1));
        when(validationService.getMemberByEmailOrPhoneNumber("budi@example.com", null)).thenReturn(member);
        when(memberOtpRepository.findTopByPhoneNumberAndOtpTypeOrderByCreatedAtDesc(member.getPhoneNumber(), OtpType.FORGOT_PIN))
                .thenReturn(Optional.of(otp));

        BusinessException ex = assertThrows(BusinessException.class, () -> authenticationService.resetPin(resetPinRequest()));

        assertThat(ex.getErrorConstant()).isEqualTo(ErrorConstant.OTP_EXPIRED);
        assertPinNotChanged();
    }

    @Test
    void resetPin_memberNotActive_shouldRejectBeforeLookingAtOtp() {
        member.setStatus(ActiveStatus.SUSPENDED);
        when(validationService.getMemberByEmailOrPhoneNumber("budi@example.com", null)).thenReturn(member);

        BusinessException ex = assertThrows(BusinessException.class, () -> authenticationService.resetPin(resetPinRequest()));

        assertThat(ex.getErrorConstant()).isEqualTo(ErrorConstant.MEMBER_NOT_ACTIVE);
        verify(memberOtpRepository, never()).findTopByPhoneNumberAndOtpTypeOrderByCreatedAtDesc(any(), any());
        assertPinNotChanged();
    }

    // =========================================================================
    // Lock-out and cool-down
    // =========================================================================
    @Test
    void loginMember_correctPin_shouldClearFailureCounters() {
        member.setStatus(ActiveStatus.ACTIVE);
        LoginRequest request = buildLoginRequest();
        when(validationService.getMemberByEmailOrPhoneNumber(any(), any())).thenReturn(member);
        when(passwordEncoder.matches(any(), any())).thenReturn(true);
        when(memberTokenRepository.save(any(MemberToken.class))).thenReturn(new MemberToken());

        authenticationService.loginMember(request);

        verify(lockoutService).assertPinNotLocked(member);
        verify(lockoutService).onPinSuccess(member);
        verify(lockoutService, never()).onPinFailure(any());
    }

    @Test
    void loginMember_wrongPin_shouldRecordFailureAndPropagateLock() {
        member.setStatus(ActiveStatus.ACTIVE);
        LoginRequest request = buildLoginRequest();
        when(validationService.getMemberByEmailOrPhoneNumber(any(), any())).thenReturn(member);
        when(passwordEncoder.matches(any(), any())).thenReturn(false);
        doThrow(new TooManyRequestsException(ErrorConstant.ACCOUNT_LOCKED, 900))
                .when(lockoutService).onPinFailure(member);

        TooManyRequestsException ex = assertThrows(TooManyRequestsException.class,
                () -> authenticationService.loginMember(request));

        assertThat(ex.getErrorConstant()).isEqualTo(ErrorConstant.ACCOUNT_LOCKED);
        assertThat(ex.getRetryAfterSeconds()).isEqualTo(900);
        verify(memberTokenRepository, never()).save(any(MemberToken.class));
    }

    @Test
    void loginMember_lockedMember_shouldBeRefusedWithoutCheckingPin() {
        member.setStatus(ActiveStatus.ACTIVE);
        LoginRequest request = buildLoginRequest();
        when(validationService.getMemberByEmailOrPhoneNumber(any(), any())).thenReturn(member);
        doThrow(new TooManyRequestsException(ErrorConstant.ACCOUNT_LOCKED, 600))
                .when(lockoutService).assertPinNotLocked(member);

        assertThrows(TooManyRequestsException.class, () -> authenticationService.loginMember(request));

        verify(passwordEncoder, never()).matches(any(), any());
        verify(memberTokenRepository, never()).save(any(MemberToken.class));
    }

    @Test
    void resendOtp_withinCooldown_shouldBeRefusedAndSendNothing() {
        member.setStatus(ActiveStatus.INACTIVE);
        ResendOtpRequest request = new ResendOtpRequest();
        request.setEmail("budi@example.com");
        request.setOtpType(OtpType.REGISTER);
        MemberOtp recent = new MemberOtp();
        recent.setCreatedAt(LocalDateTime.now().minusSeconds(10));
        when(validationService.getMemberByEmailIgnoreCase("budi@example.com")).thenReturn(member);
        when(memberOtpRepository.findTopByPhoneNumberAndOtpTypeOrderByCreatedAtDesc(member.getPhoneNumber(), OtpType.REGISTER))
                .thenReturn(Optional.of(recent));

        TooManyRequestsException ex = assertThrows(TooManyRequestsException.class,
                () -> authenticationService.resendOtp(request));

        assertThat(ex.getErrorConstant()).isEqualTo(ErrorConstant.OTP_COOLDOWN);
        assertThat(ex.getRetryAfterSeconds()).isBetween(40L, 60L);
        verify(memberOtpRepository, never()).deleteByPhoneNumberAndOtpType(any(), any());
        verify(otpService, never()).sendOtp(anyString(), anyString(), anyString(), anyString());
    }

    @Test
    void resendOtp_afterCooldown_shouldSend() {
        member.setStatus(ActiveStatus.INACTIVE);
        ResendOtpRequest request = new ResendOtpRequest();
        request.setEmail("budi@example.com");
        request.setOtpType(OtpType.REGISTER);
        MemberOtp old = new MemberOtp();
        old.setCreatedAt(LocalDateTime.now().minusMinutes(2));
        when(validationService.getMemberByEmailIgnoreCase("budi@example.com")).thenReturn(member);
        when(memberOtpRepository.findTopByPhoneNumberAndOtpTypeOrderByCreatedAtDesc(member.getPhoneNumber(), OtpType.REGISTER))
                .thenReturn(Optional.of(old));

        authenticationService.resendOtp(request);

        verify(otpService).sendOtp(anyString(), anyString(), anyString(), anyString());
    }

    @Test
    void forgotPin_withinCooldown_shouldBeRefused() {
        member.setStatus(ActiveStatus.ACTIVE);
        ForgotPinRequest request = new ForgotPinRequest();
        request.setEmail("budi@example.com");
        MemberOtp recent = new MemberOtp();
        recent.setCreatedAt(LocalDateTime.now().minusSeconds(5));
        when(validationService.getMemberByEmailIgnoreCase("budi@example.com")).thenReturn(member);
        when(memberOtpRepository.findTopByPhoneNumberAndOtpTypeOrderByCreatedAtDesc(member.getPhoneNumber(), OtpType.FORGOT_PIN))
                .thenReturn(Optional.of(recent));

        TooManyRequestsException ex = assertThrows(TooManyRequestsException.class,
                () -> authenticationService.forgotPin(request));

        assertThat(ex.getErrorConstant()).isEqualTo(ErrorConstant.OTP_COOLDOWN);
        verify(otpService, never()).sendOtp(anyString(), anyString(), anyString(), anyString());
    }

    @Test
    void verifyOtp_whileOtpLocked_shouldBeRefusedWithoutCheckingCode() {
        member.setStatus(ActiveStatus.INACTIVE);
        VerifyOtpRequest request = new VerifyOtpRequest();
        request.setEmail("budi@example.com");
        request.setOtpCode("123456");
        request.setOtpType(OtpType.REGISTER);
        when(validationService.getMemberByEmailIgnoreCase("budi@example.com")).thenReturn(member);
        doThrow(new TooManyRequestsException(ErrorConstant.OTP_LOCKED, 300))
                .when(lockoutService).assertOtpNotLocked(member);

        assertThrows(TooManyRequestsException.class, () -> authenticationService.verifyOtp(request));

        verify(validationService, never()).verifyOtp(any(), any());
    }

    @Test
    void forgotPin_whileOtpLocked_shouldBeRefused() {
        member.setStatus(ActiveStatus.ACTIVE);
        ForgotPinRequest request = new ForgotPinRequest();
        request.setEmail("budi@example.com");
        when(validationService.getMemberByEmailIgnoreCase("budi@example.com")).thenReturn(member);
        doThrow(new TooManyRequestsException(ErrorConstant.OTP_LOCKED, 300))
                .when(lockoutService).assertOtpNotLocked(member);

        assertThrows(TooManyRequestsException.class, () -> authenticationService.forgotPin(request));

        verify(otpService, never()).sendOtp(anyString(), anyString(), anyString(), anyString());
    }

    // =========================================================================
    // OTP type / account state guards
    // =========================================================================
    @Test
    void verifyOtp_forgotPin_shouldMarkVerifiedKeepOtpAndNotTouchStatus() {
        member.setStatus(ActiveStatus.ACTIVE);
        VerifyOtpRequest request = new VerifyOtpRequest();
        request.setEmail("budi@example.com");
        request.setOtpCode("123456");
        request.setOtpType(OtpType.FORGOT_PIN);
        MemberOtp otp = new MemberOtp();
        otp.setExpiredAt(LocalDateTime.now().plusMinutes(1));
        when(validationService.getMemberByEmailIgnoreCase("budi@example.com")).thenReturn(member);
        when(validationService.verifyOtp(member, request)).thenReturn(otp);

        BaseResponse<Void> result = authenticationService.verifyOtp(request);

        assertThat(result.getResponseStatus()).isEqualTo(ErrorConstant.VERIFY_OTP_SUCCESS.getCode());
        assertThat(otp.getVerifiedAt()).isNotNull();
        assertThat(otp.getExpiredAt()).isAfter(LocalDateTime.now().plusMinutes(4));
        assertThat(member.getStatus()).isEqualTo(ActiveStatus.ACTIVE);
        verify(memberOtpRepository).save(otp);
        verify(memberOtpRepository, never()).delete(any(MemberOtp.class));
        verify(memberRepository, never()).save(any(Member.class));
    }

    @Test
    void verifyOtp_registerOtpForSuspendedMember_shouldRejectWithoutReactivating() {
        member.setStatus(ActiveStatus.SUSPENDED);
        VerifyOtpRequest request = new VerifyOtpRequest();
        request.setEmail("budi@example.com");
        request.setOtpCode("123456");
        request.setOtpType(OtpType.REGISTER);
        when(validationService.getMemberByEmailIgnoreCase("budi@example.com")).thenReturn(member);

        BusinessException ex = assertThrows(BusinessException.class, () -> authenticationService.verifyOtp(request));

        assertThat(ex.getErrorConstant()).isEqualTo(ErrorConstant.OTP_NOT_FOUND);
        assertThat(member.getStatus()).isEqualTo(ActiveStatus.SUSPENDED);
        verify(validationService, never()).verifyOtp(any(), any());
        verify(memberRepository, never()).save(any(Member.class));
    }

    @Test
    void verifyOtp_forgotPinOtpForSuspendedMember_shouldReject() {
        member.setStatus(ActiveStatus.SUSPENDED);
        VerifyOtpRequest request = new VerifyOtpRequest();
        request.setEmail("budi@example.com");
        request.setOtpCode("123456");
        request.setOtpType(OtpType.FORGOT_PIN);
        when(validationService.getMemberByEmailIgnoreCase("budi@example.com")).thenReturn(member);

        BusinessException ex = assertThrows(BusinessException.class, () -> authenticationService.verifyOtp(request));

        assertThat(ex.getErrorConstant()).isEqualTo(ErrorConstant.OTP_NOT_FOUND);
        assertThat(member.getStatus()).isEqualTo(ActiveStatus.SUSPENDED);
        verify(validationService, never()).verifyOtp(any(), any());
    }

    @Test
    void forgotPin_memberNotActive_shouldRejectAndSendNothing() {
        member.setStatus(ActiveStatus.SUSPENDED);
        ForgotPinRequest request = new ForgotPinRequest();
        request.setEmail("budi@example.com");
        when(validationService.getMemberByEmailIgnoreCase("budi@example.com")).thenReturn(member);

        BusinessException ex = assertThrows(BusinessException.class, () -> authenticationService.forgotPin(request));

        assertThat(ex.getErrorConstant()).isEqualTo(ErrorConstant.MEMBER_NOT_ACTIVE);
        verify(memberOtpRepository, never()).save(any());
        verify(otpService, never()).sendOtp(anyString(), anyString(), anyString(), anyString());
    }

    @Test
    void resendOtp_suspendedMember_shouldAnswerGenericallyButSendNothing() {
        member.setStatus(ActiveStatus.SUSPENDED);
        ResendOtpRequest request = new ResendOtpRequest();
        request.setEmail("budi@example.com");
        request.setOtpType(OtpType.REGISTER);
        when(validationService.getMemberByEmailIgnoreCase("budi@example.com")).thenReturn(member);

        BaseResponse<Void> result = authenticationService.resendOtp(request);

        assertThat(result.getResponseStatus()).isEqualTo(ErrorConstant.RESEND_OTP.getCode());
        verify(memberOtpRepository, never()).save(any());
        verify(otpService, never()).sendOtp(anyString(), anyString(), anyString(), anyString());
    }

    // =========================================================================
    // logoutMember
    // =========================================================================
    @Test
    void logoutMember_success_shouldDeleteTokenAndReturnLogoutResponse() {
        when(validationService.getMemberById("member-uuid-001")).thenReturn(member);
        doNothing().when(memberTokenRepository).deleteByAccessToken("access-token");
        doNothing().when(memberActivityService).createMemberActivity(anyString(), any());

        BaseResponse<Void> result = authenticationService.logoutMember("member-uuid-001", "access-token");

        assertThat(result.getResponseStatus()).isEqualTo(ErrorConstant.LOGOUT_SUCCESS.getCode());
        verify(memberTokenRepository).deleteByAccessToken("access-token");
        verify(memberActivityService).createMemberActivity(member.getPhoneNumber(), MemberActivityEvent.ATTEMPT_LOGOUT);
        verify(memberActivityService).createMemberActivity(member.getPhoneNumber(), MemberActivityEvent.SUCCESS_LOGOUT);
    }

    // =========================================================================
    // helpers
    // =========================================================================
    private ResetPinRequest resetPinRequest() {
        ResetPinRequest r = new ResetPinRequest();
        r.setEmail("budi@example.com");
        r.setNewPin("654321");
        return r;
    }

    private MemberOtp verifiedOtp(LocalDateTime expiredAt) {
        MemberOtp otp = new MemberOtp();
        otp.setOtpType(OtpType.FORGOT_PIN);
        otp.setVerifiedAt(LocalDateTime.now().minusMinutes(1));
        otp.setExpiredAt(expiredAt);
        return otp;
    }

    private void assertPinNotChanged() {
        assertThat(member.getPin()).isEqualTo("hashed-pin");
        verify(memberRepository, never()).save(any(Member.class));
        verify(memberTokenRepository, never()).deleteByUserId(anyString());
    }

    private RegisterRequest buildRegisterRequest() {
        RegisterRequest r = new RegisterRequest();
        r.setFirstName("Budi");
        r.setLastName("Santoso");
        r.setEmail("budi@example.com");
        r.setPhoneNumber("628123456789");
        r.setPin("123456");
        return r;
    }

    private LoginRequest buildLoginRequest() {
        LoginRequest r = new LoginRequest();
        r.setEmail("budi@example.com");
        r.setPhoneNumber(null);
        r.setPin("123456");
        return r;
    }
}
