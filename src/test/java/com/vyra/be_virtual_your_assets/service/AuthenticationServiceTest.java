package com.vyra.be_virtual_your_assets.service;

import com.vyra.be_virtual_your_assets.constant.ErrorConstant;
import com.vyra.be_virtual_your_assets.constant.MemberActivityEvent;
import com.vyra.be_virtual_your_assets.constant.MemberStatus;
import com.vyra.be_virtual_your_assets.constant.OtpType;
import com.vyra.be_virtual_your_assets.dto.BaseResponse;
import com.vyra.be_virtual_your_assets.dto.auth.*;
import com.vyra.be_virtual_your_assets.dto.wallet.CreateWalletRequest;
import com.vyra.be_virtual_your_assets.dto.wallet.CreateWalletResponse;
import com.vyra.be_virtual_your_assets.entity.Member;
import com.vyra.be_virtual_your_assets.entity.MemberOtp;
import com.vyra.be_virtual_your_assets.entity.MemberToken;
import com.vyra.be_virtual_your_assets.exception.BusinessException;
import com.vyra.be_virtual_your_assets.repository.MemberOtpRepository;
import com.vyra.be_virtual_your_assets.repository.MemberRepository;
import com.vyra.be_virtual_your_assets.repository.MemberTokenRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

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
        member.setStatus(MemberStatus.INACTIVE);
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
        member.setStatus(MemberStatus.INACTIVE);
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
        assertThat(member.getStatus()).isEqualTo(MemberStatus.ACTIVE);
        verify(memberRepository).save(member);
        verify(memberOtpRepository).delete(otp);
    }

    // =========================================================================
    // loginMember — success
    // =========================================================================
    @Test
    void loginMember_success_shouldSaveTokenAndReturnLoginResponse() {
        member.setStatus(MemberStatus.ACTIVE);
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

    // =========================================================================
    // loginMember — member not ACTIVE
    // =========================================================================
    @Test
    void loginMember_memberNotActive_shouldThrowMemberNotActive() {
        member.setStatus(MemberStatus.INACTIVE);
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
        member.setStatus(MemberStatus.SUSPENDED);
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
        member.setStatus(MemberStatus.ACTIVE);
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
        token.setRefreshTokenExpiredAt(LocalDateTime.now().plusDays(1));
        when(memberTokenRepository.findByRefreshToken("valid-refresh")).thenReturn(Optional.of(token));
        lenient().when(memberTokenRepository.save(any())).thenReturn(token);

        BaseResponse<RefreshTokenResponse> result = authenticationService.refreshToken(request);

        assertThat(result.getResponseStatus()).isEqualTo(ErrorConstant.REFRESH_TOKEN_SUCCESS.getCode());
        assertThat(result.getData().getAccessToken()).isNotBlank();
        assertThat(result.getData().getRefreshToken()).isNotBlank();
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
        when(validationService.getMemberByEmailIgnoreCase("budi@example.com")).thenReturn(member);
        when(memberOtpRepository.save(any())).thenReturn(new MemberOtp());
        doNothing().when(otpService).sendOtp(anyString(), anyString(), anyString(), anyString());
        doNothing().when(memberActivityService).createMemberActivity(anyString(), any());

        BaseResponse<Void> result = authenticationService.forgotPin(request);

        assertThat(result.getResponseStatus()).isEqualTo(ErrorConstant.FORGOT_PIN_OTP_SENT.getCode());
        verify(otpService).sendOtp(anyString(), anyString(), anyString(), anyString());
        verify(memberActivityService).createMemberActivity(member.getPhoneNumber(), MemberActivityEvent.ATTEMPT_GENERATE_FORGOT_PIN);
        verify(memberActivityService).createMemberActivity(member.getPhoneNumber(), MemberActivityEvent.SUCCESS_GENERATE_FORGOT_PIN);
    }

    // =========================================================================
    // resetPin
    // =========================================================================
    @Test
    void resetPin_success_shouldEncodeNewPinAndSaveMember() {
        ResetPinRequest request = new ResetPinRequest();
        request.setEmail("budi@example.com");
        request.setNewPin("654321");
        when(validationService.getMemberByEmailOrPhoneNumber("budi@example.com", null)).thenReturn(member);
        when(passwordEncoder.encode("654321")).thenReturn("new-hashed-pin");
        when(memberRepository.save(member)).thenReturn(member);
        doNothing().when(memberActivityService).createMemberActivity(anyString(), any());

        BaseResponse<Void> result = authenticationService.resetPin(request);

        assertThat(result.getResponseStatus()).isEqualTo(ErrorConstant.RESET_PIN_SUCCESS.getCode());
        assertThat(member.getPin()).isEqualTo("new-hashed-pin");
        verify(memberRepository).save(member);
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
