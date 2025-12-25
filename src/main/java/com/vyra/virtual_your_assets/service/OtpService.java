package com.vyra.virtual_your_assets.service;

import com.vyra.virtual_your_assets.constant.ErrorConstant;
import com.vyra.virtual_your_assets.constant.MemberActivityEvent;
import com.vyra.virtual_your_assets.constant.MemberStatus;
import com.vyra.virtual_your_assets.constant.OtpType;
import com.vyra.virtual_your_assets.dto.BaseResponse;
import com.vyra.virtual_your_assets.dto.register.ForgotPasswordRequest;
import com.vyra.virtual_your_assets.dto.register.RegisterRequest;
import com.vyra.virtual_your_assets.dto.register.ResendOtpRequest;
import com.vyra.virtual_your_assets.dto.register.VerifyOtpRequest;
import com.vyra.virtual_your_assets.entity.Member;
import com.vyra.virtual_your_assets.entity.MemberActivity;
import com.vyra.virtual_your_assets.entity.MemberOtp;
import com.vyra.virtual_your_assets.exception.BusinessException;
import com.vyra.virtual_your_assets.repository.MemberActivityRepository;
import com.vyra.virtual_your_assets.repository.MemberOtpRepository;
import com.vyra.virtual_your_assets.repository.MemberRepository;
import com.vyra.virtual_your_assets.util.OtpUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Slf4j
public class OtpService {
    private final MemberOtpRepository memberOtpRepository;
    private final MemberRepository memberRepository;
    private final MemberActivityRepository memberActivityRepository;

    private final MemberActivityService memberActivityService;

    private final EmailClient emailClient;
    // private final WhatsAppClient whatsAppClient;
    private final BCryptPasswordEncoder passwordEncoder;

    public void sendOtp(String fullName, String email, String otp) {
        try {
            emailClient.sendOtpEmail(fullName, email, otp);
            /*
            Not used for now
            String message = String.format(
                    "VYRA Verification Code: %s\nThis code is confidential and valid for a limited time.",
                    otp
            );
            whatsAppClient.sendMessage(phoneNumber, message);
            */
        } catch (Exception ex) {
            throw new BusinessException(ErrorConstant.INTERNAL_SERVER_ERROR);
        }
    }

    public BaseResponse<Void> forgotPassword(ForgotPasswordRequest request) {
        log.info("[START] otpService.verifyForgotPasswordOtp request : {} ", request);

        memberActivityService.createMemberActivity(request.getPhoneNumber(), MemberActivityEvent.ATTEMPT_GENERATE_FORGOT_PASSWORD);

        Member member = memberRepository.findByPhoneNumber(request.getPhoneNumber())
                .orElseThrow(() -> new BusinessException(ErrorConstant.MEMBER_NOT_FOUND));

        // TODO: send OTP via SMS gateway
        String otp = OtpUtil.generateOtp();

        MemberOtp memberOtp = new MemberOtp();
        memberOtp.setPhoneNumber(member.getPhoneNumber());
        memberOtp.setOtpCode(otp);
        memberOtp.setOtpType(OtpType.FORGOT_PASSWORD);
        memberOtp.setExpiredAt(OtpUtil.getExpiredTime());
        memberOtp.setCreatedAt(LocalDateTime.now());

        memberOtpRepository.save(memberOtp);
        log.info("FORGOT PASSWORD OTP: {}, phoneNumber: {}", otp, request.getPhoneNumber());

        memberActivityService.createMemberActivity(request.getPhoneNumber(), MemberActivityEvent.SUCCESS_GENERATE_FORGOT_PASSWORD);
        log.info("[END] otpService.verifyForgotPasswordOtp successfully request : {} ", request);

        return new BaseResponse<>(
                ErrorConstant.FORGOT_PASSWORD_OTP_SENT.getCode(),
                ErrorConstant.FORGOT_PASSWORD_OTP_SENT.getMessage(),
                null
        );
    }

    @Transactional
    public BaseResponse<Void> verifyOtp(VerifyOtpRequest request) {
        log.info("[START] otpService.verifyOtp request: {} ", request);
        memberActivityService.createMemberActivity(request.getPhoneNumber(), MemberActivityEvent.ATTEMPT_VERIFY_OTP);

        MemberOtp memberOtp = memberOtpRepository.findTopByPhoneNumberAndOtpTypeOrderByCreatedAtDesc(request.getPhoneNumber(), request.getOtpType())
                .orElseThrow(() -> new BusinessException(ErrorConstant.OTP_NOT_FOUND));

        if (memberOtp.getExpiredAt().isBefore(LocalDateTime.now())) {
            throw new BusinessException(ErrorConstant.OTP_EXPIRED);
        }

        if (!passwordEncoder.matches(request.getOtpCode(), memberOtp.getOtpCode())) {
            int currentAttempts = (memberOtp.getAttempts() == null ? 0 : memberOtp.getAttempts()) + 1;
            memberOtp.setAttempts(currentAttempts);

            if (currentAttempts >= 3) {
                memberOtpRepository.deleteByPhoneNumber(request.getPhoneNumber());
                memberRepository.deleteByPhoneNumber(request.getPhoneNumber());
                memberActivityRepository.deleteByPhoneNumber(request.getPhoneNumber());

                log.warn("Max attempts reached for {}. Data deleted.", request.getPhoneNumber());
                throw new BusinessException(ErrorConstant.MAX_ATTEMPTS_REACHED);
            } else {
                memberOtpRepository.save(memberOtp);
                throw new BusinessException(ErrorConstant.OTP_INVALID);
            }
        }

        Member member = memberRepository.findByPhoneNumber(request.getPhoneNumber())
                .orElseThrow(() -> new BusinessException(ErrorConstant.MEMBER_NOT_FOUND));

        member.setStatus(MemberStatus.ACTIVE);
        member.setUpdatedAt(LocalDateTime.now());
        memberRepository.save(member);
        memberOtpRepository.delete(memberOtp);

        return new BaseResponse<>(
                ErrorConstant.VERIFY_OTP_SUCCESS.getCode(),
                ErrorConstant.VERIFY_OTP_SUCCESS.getMessage(),
                null
        );
    }
}
