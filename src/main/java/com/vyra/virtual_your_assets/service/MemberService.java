package com.vyra.virtual_your_assets.service;

import com.vyra.virtual_your_assets.constant.ErrorConstant;
import com.vyra.virtual_your_assets.constant.MemberActivityEvent;
import com.vyra.virtual_your_assets.constant.OtpType;
import com.vyra.virtual_your_assets.dto.BaseResponse;
import com.vyra.virtual_your_assets.dto.register.ForgotPasswordRequest;
import com.vyra.virtual_your_assets.dto.register.ResetPasswordRequest;
import com.vyra.virtual_your_assets.entity.Member;
import com.vyra.virtual_your_assets.entity.MemberOtp;
import com.vyra.virtual_your_assets.exception.BusinessException;
import com.vyra.virtual_your_assets.repository.MemberOtpRepository;
import com.vyra.virtual_your_assets.repository.MemberRepository;
import com.vyra.virtual_your_assets.util.OtpUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.stream.Stream;

@Service
@RequiredArgsConstructor
@Slf4j
public class MemberService {
    private final MemberRepository memberRepository;
    private final MemberOtpRepository memberOtpRepository;

    private final BCryptPasswordEncoder passwordEncoder;
    private final MemberActivityService memberActivityService;
    private final OtpService otpService;

    public BaseResponse<Void> resetPin(ResetPasswordRequest request) {
        log.info("[START] memberService.resetPassword request : {} ", request);

        memberActivityService.createMemberActivity(request.getPhoneNumber(), MemberActivityEvent.ATTEMPT_RESET_PASSWORD);

        Member member = memberRepository.findByPhoneNumber(request.getPhoneNumber())
                .orElseThrow(() -> new BusinessException(ErrorConstant.MEMBER_NOT_FOUND));

        member.setPin(passwordEncoder.encode(request.getNewPin()));
        member.setUpdatedAt(LocalDateTime.now());
        memberRepository.save(member);

        memberActivityService.createMemberActivity(request.getPhoneNumber(), MemberActivityEvent.SUCCESS_RESET_PASSWORD);

        log.info("[END] memberService.resetPassword successfully request : {} ", request);
        return new BaseResponse<>(
                ErrorConstant.RESET_PIN_SUCCESS.getCode(),
                ErrorConstant.RESET_PIN_SUCCESS.getMessage(),
                null
        );
    }

    public BaseResponse<Void> forgotPin(ForgotPasswordRequest request) {
        // TODO: send OTP via WhatsApp gateway

        log.info("[START] memberService.forgotPin request : {} ", request);
        memberActivityService.createMemberActivity(request.getEmail(), MemberActivityEvent.ATTEMPT_GENERATE_FORGOT_PASSWORD);

        Member member = memberRepository.findByEmailIgnoreCase(request.getEmail())
                .orElseThrow(() -> new BusinessException(ErrorConstant.MEMBER_NOT_FOUND));

        String otp = OtpUtil.generateOtp();

        MemberOtp memberOtp = new MemberOtp();
        memberOtp.setPhoneNumber(member.getPhoneNumber());
        memberOtp.setOtpCode(otp);
        memberOtp.setOtpType(OtpType.FORGOT_PASSWORD);
        memberOtp.setExpiredAt(OtpUtil.getExpiredTime());
        memberOtp.setCreatedAt(LocalDateTime.now());
        memberOtpRepository.save(memberOtp);

        String fullName = member.getFirstName().trim() + member.getLastName().trim();
        otpService.sendOtp(fullName, request.getEmail(), otp);

        memberActivityService.createMemberActivity(request.getEmail(), MemberActivityEvent.SUCCESS_GENERATE_FORGOT_PASSWORD);

        log.info("FORGOT PASSWORD OTP: {}, phoneNumber: {}", otp, request.getEmail());
        log.info("[END] memberService.forgotPin successfully request : {} ", request);

        return new BaseResponse<>(
                ErrorConstant.FORGOT_PASSWORD_OTP_SENT.getCode(),
                ErrorConstant.FORGOT_PASSWORD_OTP_SENT.getMessage(),
                null
        );
    }
}
