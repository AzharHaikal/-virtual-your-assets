//package com.vyra.virtual_your_assets.service;
//
//import com.vyra.virtual_your_assets.constant.ErrorConstant;
//import com.vyra.virtual_your_assets.constant.MemberStatus;
//import com.vyra.virtual_your_assets.constant.OtpType;
//import com.vyra.virtual_your_assets.dto.BaseResponse;
//import com.vyra.virtual_your_assets.dto.auth.LoginRequest;
//import com.vyra.virtual_your_assets.dto.auth.LoginResponse;
//import com.vyra.virtual_your_assets.dto.auth.RegisterRequest;
//import com.vyra.virtual_your_assets.dto.auth.RegisterResponse;
//import com.vyra.virtual_your_assets.dto.auth.ResendOtpRequest;
//import com.vyra.virtual_your_assets.dto.auth.VerifyOtpRequest;
//import com.vyra.virtual_your_assets.dto.wallet.CreateWalletResponse;
//import com.vyra.virtual_your_assets.entity.Member;
//import com.vyra.virtual_your_assets.entity.MemberOtp;
//import com.vyra.virtual_your_assets.exception.BusinessException;
//import com.vyra.virtual_your_assets.repository.MemberOtpRepository;
//import com.vyra.virtual_your_assets.repository.MemberRepository;
//import com.vyra.virtual_your_assets.repository.MemberTokenRepository;
//import org.apache.logging.log4j.util.InternalException;
//import org.junit.jupiter.api.Test;
//import org.junit.jupiter.api.extension.ExtendWith;
//import org.mockito.InjectMocks;
//import org.mockito.Mock;
//import org.mockito.junit.jupiter.MockitoExtension;
//import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
//
//import java.time.LocalDateTime;
//
//import static org.junit.jupiter.api.Assertions.*;
//import static org.mockito.ArgumentMatchers.any;
//import static org.mockito.Mockito.*;
//
//@ExtendWith(MockitoExtension.class)
//class AuthenticationServiceTest {
//    @Mock private MemberRepository memberRepository;
//    @Mock private MemberOtpRepository memberOtpRepository;
//    @Mock private MemberTokenRepository memberTokenRepository;
//    @Mock private MemberActivityService memberActivityService;
//    @Mock private OtpService otpService;
//    @Mock private ValidationService validationService;
//    @Mock private WalletService walletService;
//    @Mock private BCryptPasswordEncoder passwordEncoder;
//    @InjectMocks private AuthenticationService authenticationService;
//
//    @Test
//    void registerMemberSuccess() {
//        RegisterRequest request = getRegisterRequest();
//
//        BaseResponse<CreateWalletResponse> createWalletResponse = new BaseResponse<>();
//        createWalletResponse.setResponseStatus(ErrorConstant.CREATE_WALLET_SUCCESS.getCode());
//
//        when(passwordEncoder.encode(any())).thenReturn("hashedPin");
//        when(walletService.createMemberWallet(any())).thenReturn(createWalletResponse);
//
//        BaseResponse<RegisterResponse> response = authenticationService.registerMember(request);
//        assertEquals(ErrorConstant.REGISTER_SUCCESS.getCode(), response.getResponseStatus());
//        assertEquals(ErrorConstant.REGISTER_SUCCESS.getMessage(), response.getResponseMessage());
//    }
//
//    @Test
//    void registerMember_failed_whenCreateWallet() {
//        RegisterRequest request = getRegisterRequest();
//
//        BaseResponse<CreateWalletResponse> createWalletResponse = new BaseResponse<>();
//        createWalletResponse.setResponseStatus(ErrorConstant.CREATE_WALLET_FAILED.getCode());
//
//        when(passwordEncoder.encode(any())).thenReturn("hashedPin");
//        when(walletService.createMemberWallet(any())).thenReturn(createWalletResponse);
//
//        assertThrows(BusinessException.class, () -> authenticationService.registerMember(request));
//    }
//
//    @Test
//    void registerMember_failed_whenCreateWalletReturnException() {
//        RegisterRequest request = getRegisterRequest();
//        when(passwordEncoder.encode(any())).thenReturn("hashedPin");
//        when(walletService.createMemberWallet(any())).thenThrow(new InternalException(ErrorConstant.INTERNAL_SERVER_ERROR.getMessage()));
//        assertThrows(InternalException.class, () -> authenticationService.registerMember(request));
//    }
//
//    @Test
//    void resendOtpSuccess() {
//        ResendOtpRequest request = new ResendOtpRequest();
//        request.setEmail("azhar@mail.com");
//        request.setOtpType(OtpType.REGISTER);
//
//        Member member = getMember();
//
//        when(validationService.getMemberByEmailIgnoreCase(any())).thenReturn(member);
//        when(passwordEncoder.encode(any())).thenReturn("hashedOtpCode");
//
//        BaseResponse<Void> response = authenticationService.resendOtp(request);
//        assertEquals(ErrorConstant.RESEND_OTP.getCode(), response.getResponseStatus());
//        assertEquals(ErrorConstant.RESEND_OTP.getMessage(), response.getResponseMessage());
//    }
//
//    @Test
//    void resendOtp_failed_whenSendOtpReturnException() {
//        ResendOtpRequest request = new ResendOtpRequest();
//        request.setEmail("azhar@mail.com");
//        request.setOtpType(OtpType.REGISTER);
//
//        Member member = getMember();
//
//        when(validationService.getMemberByEmailIgnoreCase(any())).thenReturn(member);
//        when(passwordEncoder.encode(any())).thenReturn("hashedOtpCode");
//
//        doThrow(new BusinessException(ErrorConstant.EMAIL_SEND_FAILED)).when(otpService).sendOtp(any(), any(), any());
//        assertThrows(BusinessException.class, () -> authenticationService.resendOtp(request));
//    }
//
//    @Test
//    void verifyOtpSuccess() {
//        VerifyOtpRequest request = new VerifyOtpRequest();
//        request.setEmail("email");
//        request.setOtpCode("123456");
//        request.setOtpType(OtpType.REGISTER);
//
//        MemberOtp memberOtp = new MemberOtp();
//        memberOtp.setOtpCode("hashedOtp");
//        memberOtp.setExpiredAt(LocalDateTime.now().plusMinutes(5));
//
//        Member member = getMember();
//        member.setStatus(MemberStatus.INACTIVE);
//
//        when(validationService.getMemberByEmailIgnoreCase(any())).thenReturn(member);
//        when(validationService.verifyOtp(any(), any())).thenReturn(memberOtp);
//
//        BaseResponse<Void> response = authenticationService.verifyOtp(request);
//        assertEquals(ErrorConstant.VERIFY_OTP_SUCCESS.getCode(), response.getResponseStatus());
//        assertEquals(ErrorConstant.VERIFY_OTP_SUCCESS.getMessage(), response.getResponseMessage());
//    }
//
//    @Test
//    void loginSuccess() {
//        LoginRequest request = new LoginRequest();
//        request.setIdentifier("62812345678");
//        request.setPin("123456");
//
//        Member member = getMember();
//        member.setMemberId("uuid-member");
//        member.setEmail("azhar@mail.com");
//        member.setPhoneNumber("62812345678");
//        member.setPin("hashedPin");
//        member.setStatus(MemberStatus.ACTIVE);
//
//        when(validationService.getMemberByEmailOrPhoneNumber(any())).thenReturn(member);
//        when(passwordEncoder.matches(any(), any())).thenReturn(true);
//
//        BaseResponse<LoginResponse> response = authenticationService.loginMember(request);
//        assertEquals(ErrorConstant.LOGIN_SUCCESS.getCode(), response.getResponseStatus());
//        assertEquals(ErrorConstant.LOGIN_SUCCESS.getMessage(), response.getResponseMessage());
//    }
//
//    @Test
//    void login_failed_memberInactive() {
//        LoginRequest request = new LoginRequest();
//        request.setIdentifier("62812345678");
//
//        Member member = getMember();
//        member.setStatus(MemberStatus.INACTIVE);
//        when(validationService.getMemberByEmailOrPhoneNumber(any())).thenReturn(member);
//
//        assertThrows(BusinessException.class, () -> authenticationService.loginMember(request));
//    }
//
//    @Test
//    void login_failed_invalidPin() {
//        LoginRequest request = new LoginRequest();
//        request.setIdentifier("62812345678");
//        request.setPin("wrong-pin");
//
//        Member member = new Member();
//        member.setPin("hashedPin");
//        member.setStatus(MemberStatus.ACTIVE);
//        member.setPhoneNumber("123456789");
//
//        when(validationService.getMemberByEmailOrPhoneNumber(any())).thenReturn(member);
//        when(passwordEncoder.matches(any(), any())).thenReturn(false);
//
//        assertThrows(BusinessException.class, () -> authenticationService.loginMember(request));
//    }
//
//    // ═════════════════════════════════ //
//    //              HELPER               //
//    // ═════════════════════════════════ //
//    private RegisterRequest getRegisterRequest() {
//        RegisterRequest request = new RegisterRequest();
//        request.setFirstName("Azhar");
//        request.setLastName("Haikal");
//        request.setEmail("azhar@mail.com");
//        request.setPhoneNumber("62812345678");
//        request.setPin("123456");
//        return request;
//    }
//
//    private Member getMember() {
//        Member member = new Member();
//        member.setFirstName("Azhar");
//        member.setLastName("Haikal");
//        member.setEmail("azhar@mail.com");
//        member.setPhoneNumber("62812345678");
//        return member;
//    }
//}