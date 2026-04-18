package com.vyra.virtual_your_assets.service;

import com.vyra.virtual_your_assets.constant.ErrorConstant;
import com.vyra.virtual_your_assets.constant.MemberStatus;
import com.vyra.virtual_your_assets.constant.OtpType;
import com.vyra.virtual_your_assets.dto.BaseResponse;
import com.vyra.virtual_your_assets.dto.login.LoginRequest;
import com.vyra.virtual_your_assets.dto.login.LoginResponse;
import com.vyra.virtual_your_assets.dto.register.RegisterRequest;
import com.vyra.virtual_your_assets.dto.register.RegisterResponse;
import com.vyra.virtual_your_assets.dto.register.ResendOtpRequest;
import com.vyra.virtual_your_assets.dto.register.VerifyOtpRequest;
import com.vyra.virtual_your_assets.entity.Member;
import com.vyra.virtual_your_assets.entity.MemberOtp;
import com.vyra.virtual_your_assets.entity.MemberToken;
import com.vyra.virtual_your_assets.exception.BusinessException;
import com.vyra.virtual_your_assets.repository.MemberActivityRepository;
import com.vyra.virtual_your_assets.repository.MemberOtpRepository;
import com.vyra.virtual_your_assets.repository.MemberRepository;
import com.vyra.virtual_your_assets.repository.MemberTokenRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthenticationServiceTest {
    @Mock
    private MemberRepository memberRepository;
    @Mock
    private MemberOtpRepository memberOtpRepository;
    @Mock
    private MemberTokenRepository memberTokenRepository;
    @Mock
    private MemberActivityService memberActivityService;
    @Mock
    private OtpService otpService;
    @Mock
    private ValidationService validationService;
    @Mock
    private BCryptPasswordEncoder passwordEncoder;
    @InjectMocks
    private AuthenticationService authenticationService;

    @Test
    void registerSuccess() {
        RegisterRequest request = new RegisterRequest();
        request.setFirstName("Azhar");
        request.setLastName("Haikal");
        request.setEmail("azhar@mail.com");
        request.setPhoneNumber("62812345678");
        request.setPin("123456");

        when(passwordEncoder.encode(anyString())).thenReturn("hashedPin");

        when(memberRepository.save(any(Member.class))).thenAnswer(invocation -> {
            Member member = invocation.getArgument(0);
            member.setMemberId(UUID.randomUUID().toString());
            return member;
        });

        BaseResponse<RegisterResponse> response = authenticationService.registerMember(request);
        assertNotNull(response);
        assertEquals(ErrorConstant.REGISTER_SUCCESS.getCode(), response.getResponseStatus());
        assertEquals(request.getEmail(), response.getData().getEmail());

        verify(memberRepository, times(1)).save(any(Member.class));
        verify(otpService, times(1)).sendOtp(anyString(), anyString(), anyString());
    }

    @Test
    void resendOtpSuccess() {
        ResendOtpRequest request = new ResendOtpRequest();
        request.setEmail("azhar@mail.com");
        request.setOtpType(OtpType.REGISTER);

        Member member = new Member();
        member.setFirstName("Azhar");
        member.setLastName("Haikal");
        member.setEmail("azhar@mail.com");
        member.setPhoneNumber("62812345678");

        when(validationService.getEmailIgnoreCase(request.getEmail())).thenReturn(member);
        when(passwordEncoder.encode(anyString())).thenReturn("hashedOtpCode");

        BaseResponse<Void> response = authenticationService.resendOtp(request);

        assertNotNull(response);
        assertEquals(ErrorConstant.RESEND_OTP.getCode(), response.getResponseStatus());

        verify(memberOtpRepository, times(1)).deleteByPhoneNumberAndOtpType(member.getPhoneNumber(), request.getOtpType());

        verify(memberOtpRepository, times(1)).save(argThat(otp ->
                otp.getPhoneNumber().equals(member.getPhoneNumber()) &&
                        otp.getAttempts() == 0
        ));

        verify(otpService, times(1)).sendOtp(anyString(), eq(member.getEmail()), anyString());
    }

    @Test
    void verifyOtpSuccess() {
        VerifyOtpRequest request = new VerifyOtpRequest();
        request.setEmail("email");
        request.setOtpCode("123456");
        request.setOtpType(OtpType.REGISTER);

        MemberOtp memberOtp = new MemberOtp();
        memberOtp.setOtpCode("hashedOtp");
        memberOtp.setExpiredAt(LocalDateTime.now().plusMinutes(5));

        Member member = new Member();
        member.setPhoneNumber("1324567989");
        member.setEmail(request.getEmail());
        member.setStatus(MemberStatus.INACTIVE);

        when(validationService.getEmailIgnoreCase(request.getEmail())).thenReturn(member);
        when(validationService.verifyOtp(member, request)).thenReturn(memberOtp);

        BaseResponse<Void> response = authenticationService.verifyOtp(request);

        assertEquals(ErrorConstant.VERIFY_OTP_SUCCESS.getCode(), response.getResponseStatus());
        assertEquals(MemberStatus.ACTIVE, member.getStatus());
        verify(memberOtpRepository).delete(memberOtp);
    }

    @Test
    void loginSuccess() {
        LoginRequest request = new LoginRequest();
        request.setIdentifier("62812345678");
        request.setPin("123456");

        Member member = new Member();
        member.setMemberId("uuid-member");
        member.setEmail("azhar@mail.com");
        member.setPhoneNumber("62812345678");
        member.setPin("hashedPin");
        member.setStatus(MemberStatus.ACTIVE);

        when(validationService.getMemberByEmailOrPhoneNumber(request.getIdentifier())).thenReturn(member);
        when(passwordEncoder.matches(request.getPin(), member.getPin())).thenReturn(true);

        BaseResponse<LoginResponse> response = authenticationService.login(request);

        assertNotNull(response);
        assertEquals(ErrorConstant.LOGIN_SUCCESS.getCode(), response.getResponseStatus());
        assertNotNull(response.getData().getToken());
        assertEquals(member.getEmail(), response.getData().getEmail());

        verify(memberTokenRepository, times(1)).save(any(MemberToken.class));
    }

    @Test
    void loginFailedMemberNotActive() {
        LoginRequest request = new LoginRequest();
        request.setIdentifier("62812345678");

        Member member = new Member();
        member.setStatus(MemberStatus.INACTIVE);
        when(validationService.getMemberByEmailOrPhoneNumber(request.getIdentifier())).thenReturn(member);

        BusinessException ex = assertThrows(BusinessException.class, () ->
                authenticationService.login(request)
        );
        assertEquals(ErrorConstant.MEMBER_NOT_ACTIVE, ex.getErrorConstant());
    }

    @Test
    void loginFailedInvalidPin() {
        LoginRequest request = new LoginRequest();
        request.setIdentifier("62812345678");
        request.setPin("wrong-pin");

        Member member = new Member();
        member.setPin("hashedPin");
        member.setStatus(MemberStatus.ACTIVE);

        when(validationService.getMemberByEmailOrPhoneNumber(request.getIdentifier())).thenReturn(member);
        when(passwordEncoder.matches(request.getPin(), member.getPin())).thenReturn(false);

        BusinessException ex = assertThrows(BusinessException.class, () ->
                authenticationService.login(request)
        );
        assertEquals(ErrorConstant.INVALID_PIN, ex.getErrorConstant());
    }
}