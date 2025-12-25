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
    private MemberActivityRepository memberActivityRepository;
    @Mock
    private MemberActivityService memberActivityService;
    @Mock
    private OtpService otpService;
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

        when(memberRepository.findByPhoneNumber(request.getPhoneNumber())).thenReturn(Optional.empty());
        when(memberRepository.findByEmail(request.getEmail())).thenReturn(Optional.empty());
        when(passwordEncoder.encode(anyString())).thenReturn("hashedPin");

        when(memberRepository.save(any(Member.class))).thenAnswer(invocation -> {
            Member member = invocation.getArgument(0);
            member.setMemberId(UUID.randomUUID().toString());
            return member;
        });

        BaseResponse<RegisterResponse> response = authenticationService.register(request);

        assertNotNull(response);
        assertEquals(ErrorConstant.REGISTER_SUCCESS.getCode(), response.getResponseStatus());
        assertEquals(request.getEmail(), response.getData().getEmail());

        verify(memberRepository, times(1)).save(any(Member.class));
        verify(otpService, times(1)).sendOtp(anyString(), anyString(), anyString());
    }

    @Test
    void registerFailedPhoneNumberExist() {
        RegisterRequest request = new RegisterRequest();
        request.setPhoneNumber("62812345678");

        when(memberRepository.findByPhoneNumber(request.getPhoneNumber())).thenReturn(Optional.of(new Member()));

        BusinessException exception = assertThrows(BusinessException.class, () -> {
            authenticationService.register(request);
        });

        assertEquals(ErrorConstant.PHONE_NUMBER_ALREADY_EXIST, exception.getErrorConstant());
        verify(memberRepository, never()).save(any(Member.class));
    }

    @Test
    void registerFailedEmailExist() {
        RegisterRequest request = new RegisterRequest();
        request.setPhoneNumber("62812345678");
        request.setEmail("existing@mail.com");

        when(memberRepository.findByPhoneNumber(request.getPhoneNumber())).thenReturn(Optional.empty());
        when(memberRepository.findByEmail(request.getEmail())).thenReturn(Optional.of(new Member()));

        BusinessException exception = assertThrows(BusinessException.class, () -> {
            authenticationService.register(request);
        });

        assertEquals(ErrorConstant.EMAIL_ALREADY_EXIST, exception.getErrorConstant());
        verify(memberRepository, never()).save(any(Member.class));
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

        when(memberRepository.findByEmail(request.getEmail())).thenReturn(Optional.of(member));
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
    void resendOtpFailedMemberNotFound() {
        ResendOtpRequest request = new ResendOtpRequest();
        request.setEmail("unknown@mail.com");

        when(memberRepository.findByEmail(request.getEmail())).thenReturn(Optional.empty());

        BusinessException exception = assertThrows(BusinessException.class, () -> {
            authenticationService.resendOtp(request);
        });

        assertEquals(ErrorConstant.MEMBER_NOT_FOUND, exception.getErrorConstant());

        verify(memberOtpRepository, never()).deleteByPhoneNumberAndOtpType(anyString(), any());
        verify(memberOtpRepository, never()).save(any(MemberOtp.class));
        verify(otpService, never()).sendOtp(anyString(), anyString(), anyString());
    }

    @Test
    void verifyOtpSuccess() {
        VerifyOtpRequest request = new VerifyOtpRequest();
        request.setPhoneNumber("62812345678");
        request.setOtpCode("123456");
        request.setOtpType(OtpType.REGISTER);

        MemberOtp memberOtp = new MemberOtp();
        memberOtp.setOtpCode("hashedOtp");
        memberOtp.setExpiredAt(LocalDateTime.now().plusMinutes(5));

        Member member = new Member();
        member.setPhoneNumber(request.getPhoneNumber());
        member.setStatus(MemberStatus.INACTIVE);

        when(memberOtpRepository.findTopByPhoneNumberAndOtpTypeOrderByCreatedAtDesc(anyString(), any())).thenReturn(Optional.of(memberOtp));
        when(passwordEncoder.matches(request.getOtpCode(), memberOtp.getOtpCode())).thenReturn(true);
        when(memberRepository.findByPhoneNumber(request.getPhoneNumber())).thenReturn(Optional.of(member));

        BaseResponse<Void> response = authenticationService.verifyOtp(request);

        assertEquals(ErrorConstant.VERIFY_OTP_SUCCESS.getCode(), response.getResponseStatus());
        assertEquals(MemberStatus.ACTIVE, member.getStatus());
        verify(memberOtpRepository).delete(memberOtp);
    }

    @Test
    void verifyOtpFailedOtpNotFound() {
        VerifyOtpRequest request = new VerifyOtpRequest();
        when(memberOtpRepository.findTopByPhoneNumberAndOtpTypeOrderByCreatedAtDesc(any(), any())).thenReturn(Optional.empty());

        BusinessException ex = assertThrows(BusinessException.class, () -> authenticationService.verifyOtp(request));
        assertEquals(ErrorConstant.OTP_NOT_FOUND, ex.getErrorConstant());
    }

    @Test
    void verifyOtpFailedOtpExpired() {
        VerifyOtpRequest request = new VerifyOtpRequest();
        request.setPhoneNumber("62812345678");
        request.setOtpCode("123456");

        MemberOtp memberOtp = new MemberOtp();
        memberOtp.setExpiredAt(LocalDateTime.parse("2000-12-25T20:09:45.072")); // set old date

        when(memberOtpRepository.findTopByPhoneNumberAndOtpTypeOrderByCreatedAtDesc(any(), any())).thenReturn(Optional.of(memberOtp));

        BusinessException ex = assertThrows(BusinessException.class, () -> authenticationService.verifyOtp(request));
        assertEquals(ErrorConstant.OTP_EXPIRED, ex.getErrorConstant());
    }

    @Test
    void verifyOtpFailedMaxAttempts() {
        VerifyOtpRequest request = new VerifyOtpRequest();
        request.setPhoneNumber("62812345678");
        request.setOtpCode("wrong-code");
        request.setOtpType(OtpType.REGISTER);

        MemberOtp memberOtp = new MemberOtp();
        memberOtp.setOtpCode("hashedOtp");
        memberOtp.setAttempts(2); // Simulation: wrong 2 times
        memberOtp.setExpiredAt(LocalDateTime.now().plusMinutes(5));

        when(memberOtpRepository.findTopByPhoneNumberAndOtpTypeOrderByCreatedAtDesc(anyString(), any())).thenReturn(Optional.of(memberOtp));
        when(passwordEncoder.matches(anyString(), anyString())).thenReturn(false);

        BusinessException ex = assertThrows(BusinessException.class, () ->
                authenticationService.verifyOtp(request)
        );

        assertEquals(ErrorConstant.MAX_ATTEMPTS_REACHED, ex.getErrorConstant());

        verify(memberOtpRepository).deleteByPhoneNumber(request.getPhoneNumber());
        verify(memberRepository).deleteByPhoneNumber(request.getPhoneNumber());
        verify(memberActivityRepository).deleteByPhoneNumber(request.getPhoneNumber());
    }

    @Test
    void verifyOtpFailedMemberNotFound() {
        VerifyOtpRequest request = new VerifyOtpRequest();
        request.setOtpCode("123456");

        MemberOtp memberOtp = new MemberOtp();
        memberOtp.setOtpCode("hashedOtp");
        memberOtp.setExpiredAt(LocalDateTime.now().plusMinutes(5));

        when(memberOtpRepository.findTopByPhoneNumberAndOtpTypeOrderByCreatedAtDesc(any(), any())).thenReturn(Optional.of(memberOtp));
        when(passwordEncoder.matches(anyString(), anyString())).thenReturn(true);
        when(memberRepository.findByPhoneNumber(any())).thenReturn(Optional.empty());

        BusinessException ex = assertThrows(BusinessException.class, () -> authenticationService.verifyOtp(request));
        assertEquals(ErrorConstant.MEMBER_NOT_FOUND, ex.getErrorConstant());
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

        when(memberRepository.findByIdentifier(request.getIdentifier())).thenReturn(Optional.of(member));
        when(passwordEncoder.matches(request.getPin(), member.getPin())).thenReturn(true);

        BaseResponse<LoginResponse> response = authenticationService.login(request);

        assertNotNull(response);
        assertEquals(ErrorConstant.LOGIN_SUCCESS.getCode(), response.getResponseStatus());
        assertNotNull(response.getData().getToken());
        assertEquals(member.getEmail(), response.getData().getEmail());

        verify(memberTokenRepository, times(1)).save(any(MemberToken.class));
    }

    @Test
    void loginFailedMemberNotFound() {
        LoginRequest request = new LoginRequest();
        request.setIdentifier("nonexistent");

        when(memberRepository.findByIdentifier(anyString())).thenReturn(Optional.empty());

        BusinessException ex = assertThrows(BusinessException.class, () ->
                authenticationService.login(request)
        );
        assertEquals(ErrorConstant.MEMBER_NOT_FOUND, ex.getErrorConstant());
    }

    @Test
    void loginFailedMemberNotActive() {
        LoginRequest request = new LoginRequest();
        request.setIdentifier("62812345678");

        Member member = new Member();

        when(memberRepository.findByIdentifier(request.getIdentifier())).thenReturn(Optional.of(member));

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

        when(memberRepository.findByIdentifier(request.getIdentifier())).thenReturn(Optional.of(member));
        when(passwordEncoder.matches(request.getPin(), member.getPin())).thenReturn(false);

        BusinessException ex = assertThrows(BusinessException.class, () ->
                authenticationService.login(request)
        );
        assertEquals(ErrorConstant.INVALID_PIN, ex.getErrorConstant());
    }
}