package com.vyra.be_virtual_your_assets.service;

import com.vyra.be_virtual_your_assets.config.SecurityPolicy;
import com.vyra.be_virtual_your_assets.constant.ActiveStatus;
import com.vyra.be_virtual_your_assets.constant.ErrorConstant;
import com.vyra.be_virtual_your_assets.dto.auth.VerifyOtpRequest;
import com.vyra.be_virtual_your_assets.dto.member.UpdateProfileRequest;
import com.vyra.be_virtual_your_assets.dto.member.ValidateIsUpdateProfile;
import com.vyra.be_virtual_your_assets.entity.Member;
import com.vyra.be_virtual_your_assets.entity.MemberOtp;
import com.vyra.be_virtual_your_assets.exception.BusinessException;
import com.vyra.be_virtual_your_assets.repository.MemberOtpRepository;
import com.vyra.be_virtual_your_assets.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
@RequiredArgsConstructor
@Slf4j
public class ValidationService {
    private final MemberRepository memberRepository;
    private final MemberOtpRepository memberOtpRepository;
    private final BCryptPasswordEncoder passwordEncoder;
    private final SecurityPolicy securityPolicy;

    public void validateDataExistRegister(String phoneNumber, String email) {
        if (memberRepository.findByPhoneNumber(phoneNumber).isPresent()) throw new BusinessException(ErrorConstant.PHONE_NUMBER_ALREADY_EXIST);
        if (memberRepository.findByEmailIgnoreCase(email).isPresent()) throw new BusinessException(ErrorConstant.EMAIL_ALREADY_EXIST);
    }

    public Member getMemberById(String memberId) {
        return memberRepository.findById(memberId).orElseThrow(() -> new BusinessException(ErrorConstant.MEMBER_NOT_FOUND));
    }

    public Member getMemberByEmailOrPhoneNumber(String email, String phoneNumber) {
        if (StringUtils.isNotBlank(email)) return getMemberByEmailIgnoreCase(email);
        return getMemberByPhoneNumber(phoneNumber);
    }

    public Member getMemberByPhoneNumber(String phoneNumber) {
        return memberRepository.findByPhoneNumber(phoneNumber).orElseThrow(() -> new BusinessException(ErrorConstant.MEMBER_NOT_FOUND));
    }

    public Member getMemberByEmailIgnoreCase(String email) {
        return memberRepository.findByEmailIgnoreCase(email).orElseThrow(() -> new BusinessException(ErrorConstant.MEMBER_NOT_FOUND));
    }

    public MemberOtp verifyOtp(Member member, VerifyOtpRequest request) {
        log.info("[START] validationService.verifyOtp phoneNumber: {}", member.getPhoneNumber());

        MemberOtp memberOtp = memberOtpRepository.findTopByPhoneNumberAndOtpTypeOrderByCreatedAtDesc(member.getPhoneNumber(), request.getOtpType())
                .orElseThrow(() -> new BusinessException(ErrorConstant.OTP_NOT_FOUND));

        if (memberOtp.getExpiredAt().isBefore(LocalDateTime.now())) throw new BusinessException(ErrorConstant.OTP_EXPIRED);

        if (!passwordEncoder.matches(request.getOtpCode(), memberOtp.getOtpCode())) {
            int currentAttempts = (memberOtp.getAttempts() == null ? 0 : memberOtp.getAttempts()) + 1;
            memberOtp.setAttempts(currentAttempts);

            int maxAttempts = 4;
            int remainingAttempts = maxAttempts - currentAttempts;

            if (currentAttempts >= maxAttempts) {
                // A temporary lock instead of suspending: suspending let anyone who knows an e-mail
                // address take the account offline for good by sending wrong codes.
                member.setOtpLockedUntil(LocalDateTime.now().plusMinutes(securityPolicy.getOtpLockMinutes()));
                memberRepository.save(member);
                memberOtpRepository.deleteByPhoneNumber(member.getPhoneNumber());
                log.warn("Max attempts {} reached for {}. OTP verification locked for {} minutes.",
                        currentAttempts, member.getPhoneNumber(), securityPolicy.getOtpLockMinutes());
                throw new BusinessException(ErrorConstant.MAX_ATTEMPTS_REACHED,
                        securityPolicy.getOtpLockMinutes() + " minutes.");
            } else {
                memberOtpRepository.save(memberOtp);
                throw new BusinessException(ErrorConstant.OTP_INVALID, String.valueOf(remainingAttempts));
            }
        }
        return memberOtp;
    }

    /**
     * The new e-mail address / phone number (either may be null) must not belong to another member.
     * Same rules and error codes as the profile update, used by the verified contact change flow.
     */
    public void assertContactAvailable(Member member, String newEmail, String newPhoneNumber) {
        if (StringUtils.isNotBlank(newEmail)) {
            memberRepository.findByEmailIgnoreCase(newEmail)
                    .filter(existing -> !existing.getId().equals(member.getId()))
                    .ifPresent(existing -> {
                        throw new BusinessException(ErrorConstant.EMAIL_ALREADY_EXIST_V2);
                    });
        }
        if (StringUtils.isNotBlank(newPhoneNumber)) {
            memberRepository.findByPhoneNumber(newPhoneNumber)
                    .filter(existing -> !existing.getId().equals(member.getId()))
                    .ifPresent(existing -> {
                        throw new BusinessException(ErrorConstant.PHONE_NUMBER_ALREADY_EXIST_V2);
                    });
        }
    }

    public ValidateIsUpdateProfile validateIsUpdateProfile(Member member, UpdateProfileRequest request) {
        log.info("Validate request update profile. phoneNumber: {}", member.getPhoneNumber());
        ValidateIsUpdateProfile isUpdateProfile = new ValidateIsUpdateProfile();

        if (StringUtils.isNotBlank(request.getFirstName())) member.setFirstName(request.getFirstName().trim());
        if (StringUtils.isNotBlank(request.getLastName())) member.setLastName(request.getLastName().trim());

        if (StringUtils.isNotBlank(request.getEmail())) {
            log.info("Check email already exist. phoneNumber: {}", member.getPhoneNumber());
            memberRepository.findByEmailIgnoreCase(request.getEmail())
                    .ifPresent(existingMember -> {
                        if (!existingMember.getId().equals(member.getId())) {
                            log.warn("Email already exist for another account. phoneNumber: {}", member.getPhoneNumber());
                            throw new BusinessException(ErrorConstant.EMAIL_ALREADY_EXIST_V2);
                        }
                    });
            member.setEmail(request.getEmail());
            isUpdateProfile.setEmailChanged(true);
        }

        if (StringUtils.isNotBlank(request.getPhoneNumber())) {
            log.info("Check phone number already exist. phoneNumber: {}", member.getPhoneNumber());
            memberRepository.findByPhoneNumber(request.getPhoneNumber())
                    .ifPresent(existingMember -> {
                        if (!existingMember.getId().equals(member.getId())) {
                            log.warn("Phone number already exist for another account. phoneNumber: {}", member.getPhoneNumber());
                            throw new BusinessException(ErrorConstant.PHONE_NUMBER_ALREADY_EXIST_V2);
                        }
                    });
            member.setPhoneNumber(request.getPhoneNumber());
            isUpdateProfile.setPhoneChanged(true);
        }
        return isUpdateProfile;
    }
}
