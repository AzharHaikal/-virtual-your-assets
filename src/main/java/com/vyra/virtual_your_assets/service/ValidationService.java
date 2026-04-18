package com.vyra.virtual_your_assets.service;

import com.vyra.virtual_your_assets.constant.ErrorConstant;
import com.vyra.virtual_your_assets.dto.register.VerifyOtpRequest;
import com.vyra.virtual_your_assets.entity.Member;
import com.vyra.virtual_your_assets.entity.MemberOtp;
import com.vyra.virtual_your_assets.exception.BusinessException;
import com.vyra.virtual_your_assets.repository.MemberActivityRepository;
import com.vyra.virtual_your_assets.repository.MemberOtpRepository;
import com.vyra.virtual_your_assets.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
@RequiredArgsConstructor
@Slf4j
public class ValidationService {
    private final MemberRepository memberRepository;
    private final MemberOtpRepository memberOtpRepository;
    private final MemberActivityRepository memberActivityRepository;
    private final BCryptPasswordEncoder passwordEncoder;

    public void validateDataExistRegister(String phoneNumber, String email) {
        if (memberRepository.findByPhoneNumber(phoneNumber).isPresent()) throw new BusinessException(ErrorConstant.PHONE_NUMBER_ALREADY_EXIST);
        if (memberRepository.findByEmailIgnoreCase(email).isPresent()) throw new BusinessException(ErrorConstant.EMAIL_ALREADY_EXIST);
    }


    public Member getEmailIgnoreCase(String email) {
        return memberRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new BusinessException(ErrorConstant.MEMBER_NOT_FOUND));
    }

    public MemberOtp verifyOtp(Member member, VerifyOtpRequest request) {
        log.info("[START] authenticationService.verifyOtp request: {} ", request);

        MemberOtp memberOtp = memberOtpRepository.findTopByPhoneNumberAndOtpTypeOrderByCreatedAtDesc(member.getPhoneNumber(), request.getOtpType())
                .orElseThrow(() -> new BusinessException(ErrorConstant.OTP_NOT_FOUND));

        if (memberOtp.getExpiredAt().isBefore(LocalDateTime.now())) throw new BusinessException(ErrorConstant.OTP_EXPIRED);

        if (!passwordEncoder.matches(request.getOtpCode(), memberOtp.getOtpCode())) {
            int currentAttempts = (memberOtp.getAttempts() == null ? 0 : memberOtp.getAttempts()) + 1;
            memberOtp.setAttempts(currentAttempts);

            int maxAttempts = 4;
            int remainingAttempts = maxAttempts - currentAttempts;

            if (currentAttempts >= maxAttempts) {
                memberOtpRepository.deleteByPhoneNumber(member.getPhoneNumber());
                memberRepository.deleteByPhoneNumber(member.getPhoneNumber());
                memberActivityRepository.deleteByPhoneNumber(member.getPhoneNumber());

                log.warn("Max attempts {} reached for {}. Data deleted.", currentAttempts, request.getEmail());
                throw new BusinessException(ErrorConstant.MAX_ATTEMPTS_REACHED);
            } else {
                memberOtpRepository.save(memberOtp);
                throw new BusinessException(ErrorConstant.OTP_INVALID, String.valueOf(remainingAttempts));
            }
        }
        return memberOtp;
    }

    public Member getMemberByEmailOrPhoneNumber(String identifier) {
        return memberRepository.getMemberByEmailOrPhoneNumber(identifier)
                .orElseThrow(() -> new BusinessException(ErrorConstant.MEMBER_NOT_FOUND));
    }
}
