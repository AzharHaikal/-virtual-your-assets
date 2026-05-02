package com.vyra.virtual_your_assets.service;

import com.vyra.virtual_your_assets.constant.ErrorConstant;
import com.vyra.virtual_your_assets.constant.OtpType;
import com.vyra.virtual_your_assets.dto.register.VerifyOtpRequest;
import com.vyra.virtual_your_assets.entity.Member;
import com.vyra.virtual_your_assets.entity.MemberOtp;
import com.vyra.virtual_your_assets.exception.BusinessException;
import com.vyra.virtual_your_assets.repository.MemberActivityRepository;
import com.vyra.virtual_your_assets.repository.MemberOtpRepository;
import com.vyra.virtual_your_assets.repository.MemberRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ValidationServiceTest {

    @Mock
    private MemberRepository memberRepository;
    @Mock
    private MemberOtpRepository memberOtpRepository;
    @Mock
    private MemberActivityRepository memberActivityRepository;
    @Mock
    private BCryptPasswordEncoder passwordEncoder;

    @InjectMocks
    private ValidationService validationService;

    @Test
    void validateDataExistRegister_phoneExists() {
        when(memberRepository.findByPhoneNumber(anyString())).thenReturn(Optional.of(new Member()));
        BusinessException ex = assertThrows(BusinessException.class, () -> validationService.validateDataExistRegister("123", "email@mail.com"));
        assertEquals(ErrorConstant.PHONE_NUMBER_ALREADY_EXIST, ex.getErrorConstant());
    }

    @Test
    void validateDataExistRegister_emailExists() {
        when(memberRepository.findByPhoneNumber(anyString())).thenReturn(Optional.empty());
        when(memberRepository.findByEmailIgnoreCase(anyString())).thenReturn(Optional.of(new Member()));
        BusinessException ex = assertThrows(BusinessException.class, () -> validationService.validateDataExistRegister("123", "email@mail.com"));
        assertEquals(ErrorConstant.EMAIL_ALREADY_EXIST, ex.getErrorConstant());
    }

    @Test
    void validateDataExistRegister_success() {
        when(memberRepository.findByPhoneNumber(anyString())).thenReturn(Optional.empty());
        when(memberRepository.findByEmailIgnoreCase(anyString())).thenReturn(Optional.empty());
        assertDoesNotThrow(() -> validationService.validateDataExistRegister("123", "email@mail.com"));
    }

    @Test
    void getEmailIgnoreCase_success() {
        Member member = new Member();
        when(memberRepository.findByEmailIgnoreCase(anyString())).thenReturn(Optional.of(member));
        Member result = validationService.getMemberByEmailIgnoreCase("test@mail.com");
        assertNotNull(result);
    }

    @Test
    void getEmailIgnoreCase_notFound() {
        when(memberRepository.findByEmailIgnoreCase(anyString())).thenReturn(Optional.empty());
        BusinessException ex = assertThrows(BusinessException.class, () -> validationService.getMemberByEmailIgnoreCase("test@mail.com"));
        assertEquals(ErrorConstant.MEMBER_NOT_FOUND, ex.getErrorConstant());
    }

    @Test
    void verifyOtp_notFound() {
        Member member = new Member();
        member.setPhoneNumber("123");

        VerifyOtpRequest request = new VerifyOtpRequest();
        request.setOtpType(OtpType.REGISTER);
        when(memberOtpRepository.findTopByPhoneNumberAndOtpTypeOrderByCreatedAtDesc(anyString(), any())).thenReturn(Optional.empty());

        BusinessException ex = assertThrows(BusinessException.class, () -> validationService.verifyOtp(member, request));
        assertEquals(ErrorConstant.OTP_NOT_FOUND, ex.getErrorConstant());
    }

    @Test
    void verifyOtp_expired() {
        Member member = new Member();
        member.setPhoneNumber("123");

        VerifyOtpRequest request = new VerifyOtpRequest();
        request.setOtpType(OtpType.REGISTER);

        MemberOtp otp = new MemberOtp();
        otp.setExpiredAt(LocalDateTime.now().minusMinutes(1));
        when(memberOtpRepository.findTopByPhoneNumberAndOtpTypeOrderByCreatedAtDesc(anyString(), any())).thenReturn(Optional.of(otp));

        BusinessException ex = assertThrows(BusinessException.class, () -> validationService.verifyOtp(member, request));
        assertEquals(ErrorConstant.OTP_EXPIRED, ex.getErrorConstant());
    }

    @ParameterizedTest
    @CsvSource({
            "0,3",
            "1,2",
            "2,1"
    })
    void verifyOtp_invalidOtp_attemptsIncrease(int currentAttempts, int remaining) {
        Member member = new Member();
        member.setPhoneNumber("123");

        VerifyOtpRequest request = new VerifyOtpRequest();
        request.setOtpType(OtpType.REGISTER);
        request.setOtpCode("wrong");

        MemberOtp otp = new MemberOtp();
        otp.setOtpCode("hashed");
        otp.setExpiredAt(LocalDateTime.now().plusMinutes(5));
        otp.setAttempts(currentAttempts);

        when(memberOtpRepository.findTopByPhoneNumberAndOtpTypeOrderByCreatedAtDesc(anyString(), any())).thenReturn(Optional.of(otp));
        when(passwordEncoder.matches(anyString(), anyString())).thenReturn(false);

        BusinessException ex = assertThrows(BusinessException.class, () -> validationService.verifyOtp(member, request));
        assertEquals(ErrorConstant.OTP_INVALID, ex.getErrorConstant());
        verify(memberOtpRepository).save(any(MemberOtp.class));
    }

    @Test
    void verifyOtp_maxAttemptsReached() {
        Member member = new Member();
        member.setPhoneNumber("123");

        VerifyOtpRequest request = new VerifyOtpRequest();
        request.setOtpType(OtpType.REGISTER);
        request.setOtpCode("wrong");

        MemberOtp otp = new MemberOtp();
        otp.setOtpCode("hashed");
        otp.setAttempts(3); // Simulation: wrong 3 times
        otp.setExpiredAt(LocalDateTime.now().plusMinutes(5));
        when(memberOtpRepository.findTopByPhoneNumberAndOtpTypeOrderByCreatedAtDesc(anyString(), any())).thenReturn(Optional.of(otp));
        when(passwordEncoder.matches(anyString(), anyString())).thenReturn(false);

        BusinessException ex = assertThrows(BusinessException.class, () -> validationService.verifyOtp(member, request));
        assertEquals(ErrorConstant.MAX_ATTEMPTS_REACHED, ex.getErrorConstant());

        verify(memberOtpRepository).deleteByPhoneNumber(member.getPhoneNumber());
        verify(memberRepository).deleteByPhoneNumber(member.getPhoneNumber());
        verify(memberActivityRepository).deleteByPhoneNumber(member.getPhoneNumber());
    }

    @Test
    void verifyOtp_success() {
        Member member = new Member();
        member.setPhoneNumber("123");

        VerifyOtpRequest request = new VerifyOtpRequest();
        request.setOtpType(OtpType.REGISTER);
        request.setOtpCode("123456");

        MemberOtp otp = new MemberOtp();
        otp.setOtpCode("hashed");
        otp.setExpiredAt(LocalDateTime.now().plusMinutes(5));

        when(memberOtpRepository.findTopByPhoneNumberAndOtpTypeOrderByCreatedAtDesc(anyString(), any())).thenReturn(Optional.of(otp));
        when(passwordEncoder.matches(anyString(), anyString())).thenReturn(true);

        MemberOtp result = validationService.verifyOtp(member, request);
        assertNotNull(result);
    }

    @ParameterizedTest
    @ValueSource(strings = {"email@mail.com", "62812345678"})
    void getMemberByEmailOrPhoneNumber_success(String identifier) {
        when(memberRepository.getMemberByEmailOrPhoneNumber(identifier)).thenReturn(Optional.of(new Member()));
        Member result = validationService.getMemberByEmailOrPhoneNumber(identifier);
        assertNotNull(result);
    }

    @Test
    void getMemberByEmailOrPhoneNumber_notFound() {
        when(memberRepository.getMemberByEmailOrPhoneNumber(anyString())).thenReturn(Optional.empty());
        BusinessException ex = assertThrows(BusinessException.class, () -> validationService.getMemberByEmailOrPhoneNumber("notfound"));
        assertEquals(ErrorConstant.MEMBER_NOT_FOUND, ex.getErrorConstant());
    }
}