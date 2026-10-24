package com.vyra.be_virtual_your_assets.service;

import com.vyra.be_virtual_your_assets.config.SecurityPolicy;
import com.vyra.be_virtual_your_assets.constant.ActiveStatus;
import com.vyra.be_virtual_your_assets.constant.ErrorConstant;
import com.vyra.be_virtual_your_assets.constant.MemberActivityEvent;
import com.vyra.be_virtual_your_assets.constant.OtpType;
import com.vyra.be_virtual_your_assets.dto.BaseResponse;
import com.vyra.be_virtual_your_assets.dto.account.DeleteAccountOtpResponse;
import com.vyra.be_virtual_your_assets.dto.auth.VerifyOtpRequest;
import com.vyra.be_virtual_your_assets.entity.Member;
import com.vyra.be_virtual_your_assets.entity.MemberOtp;
import com.vyra.be_virtual_your_assets.exception.BusinessException;
import com.vyra.be_virtual_your_assets.exception.TooManyRequestsException;
import com.vyra.be_virtual_your_assets.repository.MemberOtpRepository;
import com.vyra.be_virtual_your_assets.repository.MemberRepository;
import com.vyra.be_virtual_your_assets.repository.DevicePushTokenRepository;
import com.vyra.be_virtual_your_assets.repository.MemberTokenRepository;
import com.vyra.be_virtual_your_assets.util.OtpUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;

/**
 * The member deletes their own account in two steps, like a contact change:
 * <ol>
 *   <li>{@link #request}: the PIN is checked (with the usual lock-out) and an OTP is e-mailed.</li>
 *   <li>{@link #confirm}: the correct OTP marks the account DELETED and ends every session.</li>
 * </ol>
 * Soft delete only: the member row and all their data stay in the database (status DELETED,
 * deleted_at). Login and every token are refused from then on; recovery goes through support.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AccountDeletionService {

    private final MemberRepository memberRepository;
    private final MemberOtpRepository memberOtpRepository;
    private final MemberTokenRepository memberTokenRepository;
    private final DevicePushTokenRepository pushTokenRepository;
    private final ValidationService validationService;
    private final LockoutService lockoutService;
    private final OtpService otpService;
    private final MemberActivityService memberActivityService;
    private final BCryptPasswordEncoder passwordEncoder;
    private final SecurityPolicy securityPolicy;
    private final TimeService timeService;

    /** Checks the PIN and e-mails the confirmation code. */
    @Transactional(noRollbackFor = {BusinessException.class, TooManyRequestsException.class})
    public BaseResponse<DeleteAccountOtpResponse> request(String memberId, String pin) {
        Member member = activeMember(memberId);
        lockoutService.assertPinNotLocked(member);
        if (pin == null || !passwordEncoder.matches(pin, member.getPin())) {
            log.warn("Account deletion rejected, wrong PIN. memberId: {}", memberId);
            memberActivityService.createMemberActivity(member.getPhoneNumber(), MemberActivityEvent.FAILED_DELETE_ACCOUNT);
            lockoutService.onPinFailure(member); // throws 429 when this attempt reaches the limit
            throw new BusinessException(ErrorConstant.INVALID_PIN_MISS_MATCH);
        }
        lockoutService.onPinSuccess(member);
        lockoutService.assertOtpNotLocked(member);
        assertCooldown(member);

        memberActivityService.createMemberActivity(member.getPhoneNumber(), MemberActivityEvent.REQUEST_DELETE_ACCOUNT);
        return otpSent(sendOtp(member));
    }

    /** Sends a new code for a request whose PIN was already checked. */
    @Transactional(noRollbackFor = {BusinessException.class, TooManyRequestsException.class})
    public BaseResponse<DeleteAccountOtpResponse> resend(String memberId) {
        Member member = activeMember(memberId);
        lockoutService.assertOtpNotLocked(member);
        if (memberOtpRepository
                .findTopByPhoneNumberAndOtpTypeOrderByCreatedAtDesc(member.getPhoneNumber(), OtpType.DELETE_ACCOUNT)
                .isEmpty()) {
            throw new BusinessException(ErrorConstant.OTP_NOT_FOUND);
        }
        assertCooldown(member);
        return otpSent(sendOtp(member));
    }

    /** The correct OTP deletes the account (soft) and revokes every session. */
    @Transactional(noRollbackFor = {BusinessException.class, TooManyRequestsException.class})
    public BaseResponse<Void> confirm(String memberId, String otpCode) {
        Member member = activeMember(memberId);
        lockoutService.assertOtpNotLocked(member);

        VerifyOtpRequest verification = new VerifyOtpRequest();
        verification.setEmail(member.getEmail());
        verification.setOtpCode(otpCode);
        verification.setOtpType(OtpType.DELETE_ACCOUNT);
        MemberOtp otp;
        try {
            otp = validationService.verifyOtp(member, verification);
        } catch (BusinessException e) {
            memberActivityService.createMemberActivity(member.getPhoneNumber(), MemberActivityEvent.FAILED_DELETE_ACCOUNT);
            throw e;
        }

        member.setStatus(ActiveStatus.DELETED);
        member.setDeletedAt(timeService.nowUtc());
        member.setUpdatedAt(timeService.nowUtc());
        member.setModifiedBy(memberId);
        memberRepository.save(member);

        memberOtpRepository.delete(otp);
        memberTokenRepository.deleteByUserId(member.getId());
        pushTokenRepository.deleteAllOfMember(member.getId());
        memberActivityService.createMemberActivity(member.getPhoneNumber(), MemberActivityEvent.SUCCESS_DELETE_ACCOUNT);
        log.info("Member deleted their account. memberId: {}", memberId);

        return new BaseResponse<>(ErrorConstant.ACCOUNT_DELETE_SUCCESS.getCode(),
                ErrorConstant.ACCOUNT_DELETE_SUCCESS.getMessage(), null);
    }

    // ------------------------------------------------------------------------

    private Member activeMember(String memberId) {
        Member member = validationService.getMemberById(memberId);
        if (ActiveStatus.ACTIVE != member.getStatus()) {
            throw new BusinessException(ErrorConstant.MEMBER_NOT_ACTIVE);
        }
        return member;
    }

    private static BaseResponse<DeleteAccountOtpResponse> otpSent(String email) {
        return new BaseResponse<>(ErrorConstant.ACCOUNT_DELETE_OTP_SENT.getCode(),
                ErrorConstant.ACCOUNT_DELETE_OTP_SENT.getMessage(), new DeleteAccountOtpResponse(email));
    }

    /** Always to the member's current e-mail address; returns it. */
    private String sendOtp(Member member) {
        memberOtpRepository.deleteByPhoneNumberAndOtpType(member.getPhoneNumber(), OtpType.DELETE_ACCOUNT);

        String otp = OtpUtil.generateOtp();
        MemberOtp memberOtp = new MemberOtp();
        memberOtp.setPhoneNumber(member.getPhoneNumber());
        memberOtp.setOtpCode(passwordEncoder.encode(otp));
        memberOtp.setOtpType(OtpType.DELETE_ACCOUNT);
        memberOtp.setAttempts(0);
        memberOtp.setExpiredAt(OtpUtil.getExpiredTime());
        memberOtp.setCreatedAt(LocalDateTime.now());
        memberOtpRepository.save(memberOtp);

        String fullName = member.getFirstName().trim() + " " + member.getLastName().trim();
        otpService.sendOtp(fullName, member.getEmail(), member.getPhoneNumber(), otp);
        return member.getEmail();
    }

    private void assertCooldown(Member member) {
        memberOtpRepository
                .findTopByPhoneNumberAndOtpTypeOrderByCreatedAtDesc(member.getPhoneNumber(), OtpType.DELETE_ACCOUNT)
                .map(MemberOtp::getCreatedAt)
                .ifPresent(createdAt -> {
                    LocalDateTime next = createdAt.plusSeconds(securityPolicy.getOtpResendCooldownSeconds());
                    LocalDateTime now = LocalDateTime.now();
                    if (next.isAfter(now)) {
                        throw new TooManyRequestsException(ErrorConstant.OTP_COOLDOWN,
                                Duration.between(now, next).getSeconds() + 1);
                    }
                });
    }
}
