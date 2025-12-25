package com.vyra.virtual_your_assets.service;

import com.vyra.virtual_your_assets.constant.ErrorConstant;
import com.vyra.virtual_your_assets.dto.BaseResponse;
import com.vyra.virtual_your_assets.dto.register.ForgotPasswordRequest;
import com.vyra.virtual_your_assets.dto.register.ResetPasswordRequest;
import com.vyra.virtual_your_assets.entity.Member;
import com.vyra.virtual_your_assets.entity.MemberOtp;
import com.vyra.virtual_your_assets.exception.BusinessException;
import com.vyra.virtual_your_assets.repository.MemberOtpRepository;
import com.vyra.virtual_your_assets.repository.MemberRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MemberServiceTest {
    @Mock
    private MemberRepository memberRepository;
    @Mock
    private MemberOtpRepository memberOtpRepository;
    @Mock
    private BCryptPasswordEncoder passwordEncoder;
    @Mock
    private MemberActivityService memberActivityService;
    @InjectMocks
    private MemberService memberService;

    @Test
    void resetPasswordSuccess() {
        ResetPasswordRequest request = new ResetPasswordRequest();
        request.setPhoneNumber("62812345678");
        request.setNewPin("654321");

        Member member = new Member();
        member.setPhoneNumber(request.getPhoneNumber());

        when(memberRepository.findByPhoneNumber(request.getPhoneNumber())).thenReturn(Optional.of(member));
        when(passwordEncoder.encode(request.getNewPin())).thenReturn("hashedNewPin");

        BaseResponse<Void> response = memberService.resetPassword(request);

        assertEquals(ErrorConstant.RESET_PIN_SUCCESS.getCode(), response.getResponseStatus());
        assertEquals("hashedNewPin", member.getPin());
        verify(memberRepository, times(1)).save(member);
    }

    @Test
    void resetPasswordFailedMemberNotFound() {
        ResetPasswordRequest request = new ResetPasswordRequest();
        request.setPhoneNumber("62800000000");

        when(memberRepository.findByPhoneNumber(anyString())).thenReturn(Optional.empty());

        BusinessException ex = assertThrows(BusinessException.class, () ->
                memberService.resetPassword(request)
        );

        assertEquals(ErrorConstant.MEMBER_NOT_FOUND, ex.getErrorConstant());
        verify(memberRepository, never()).save(any(Member.class));
    }

    @Test
    void forgotPasswordSuccess() {
        ForgotPasswordRequest request = new ForgotPasswordRequest();
        request.setPhoneNumber("62812345678");

        Member member = new Member();
        member.setPhoneNumber(request.getPhoneNumber());

        when(memberRepository.findByPhoneNumber(request.getPhoneNumber())).thenReturn(Optional.of(member));

        BaseResponse<Void> response = memberService.forgotPassword(request);

        assertEquals(ErrorConstant.FORGOT_PASSWORD_OTP_SENT.getCode(), response.getResponseStatus());
        verify(memberOtpRepository, times(1)).save(any(MemberOtp.class));
    }

    @Test
    void forgotPasswordFailedMemberNotFound() {
        ForgotPasswordRequest request = new ForgotPasswordRequest();
        request.setPhoneNumber("62800000000");

        when(memberRepository.findByPhoneNumber(anyString())).thenReturn(Optional.empty());

        BusinessException ex = assertThrows(BusinessException.class, () ->
                memberService.forgotPassword(request)
        );
        assertEquals(ErrorConstant.MEMBER_NOT_FOUND, ex.getErrorConstant());
        verify(memberOtpRepository, never()).save(any(MemberOtp.class));
    }
}