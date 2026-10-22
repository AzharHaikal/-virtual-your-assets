package com.vyra.be_virtual_your_assets.service;

import com.vyra.be_virtual_your_assets.config.SecurityPolicy;
import com.vyra.be_virtual_your_assets.constant.ErrorConstant;
import com.vyra.be_virtual_your_assets.constant.MemberActivityEvent;
import com.vyra.be_virtual_your_assets.entity.Member;
import com.vyra.be_virtual_your_assets.exception.TooManyRequestsException;
import com.vyra.be_virtual_your_assets.repository.MemberRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Duration;
import java.time.LocalDateTime;

/**
 * Temporary lock-out of a member after repeated wrong PINs (login, change PIN and
 * e-mail/phone change share one counter) and the OTP lock check.
 *
 * Failures are recorded with atomic UPDATE statements in their own transaction, because the
 * business operation that detected the failure always rolls back: without REQUIRES_NEW the
 * counter would never advance.
 */
@Service
@Slf4j
public class LockoutService {

    private final MemberRepository memberRepository;
    private final MemberActivityService memberActivityService;
    private final SecurityPolicy policy;
    private final TransactionTemplate requiresNew;

    public LockoutService(MemberRepository memberRepository,
                          MemberActivityService memberActivityService,
                          SecurityPolicy policy,
                          PlatformTransactionManager transactionManager) {
        this.memberRepository = memberRepository;
        this.memberActivityService = memberActivityService;
        this.policy = policy;
        this.requiresNew = new TransactionTemplate(transactionManager);
        this.requiresNew.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
    }

    /** Throws 429 while the member is locked; clears an expired lock so counting starts fresh. */
    public void assertPinNotLocked(Member member) {
        LocalDateTime until = member.getPinLockedUntil();
        if (until == null) return;

        LocalDateTime now = LocalDateTime.now();
        if (until.isAfter(now)) {
            throw new TooManyRequestsException(ErrorConstant.ACCOUNT_LOCKED, secondsUntil(until, now));
        }

        // Lock expired: reset in its own transaction so it survives the caller's rollback.
        requiresNew.executeWithoutResult(s -> memberRepository.clearPinLock(member.getId()));
        member.setPinLockedUntil(null);
        member.setFailedPinAttempts(0);
    }

    /**
     * Records one wrong PIN. Throws 429 when this attempt reaches the limit; otherwise the
     * caller continues with its normal "wrong PIN" error.
     */
    public void onPinFailure(Member member) {
        final String id = member.getId();
        final LocalDateTime until = LocalDateTime.now().plusMinutes(policy.getPinLockMinutes());

        Integer attempts = requiresNew.execute(s -> {
            memberRepository.incrementFailedPinAttempts(id);
            int current = memberRepository.getFailedPinAttempts(id);
            if (current >= policy.getPinMaxAttempts()) {
                memberRepository.lockPin(id, until);
            }
            return current;
        });

        if (attempts != null && attempts >= policy.getPinMaxAttempts()) {
            log.warn("Member locked after {} wrong PIN attempts. phoneNumber: {}", attempts, member.getPhoneNumber());
            member.setPinLockedUntil(until);
            memberActivityService.createMemberActivity(member.getPhoneNumber(), MemberActivityEvent.ACCOUNT_LOCKED);
            throw new TooManyRequestsException(ErrorConstant.ACCOUNT_LOCKED, policy.getPinLockMinutes() * 60L);
        }
    }

    /** Correct PIN: forget earlier failures. Applied to the managed entity, flushed with the caller's transaction. */
    public void onPinSuccess(Member member) {
        if ((member.getFailedPinAttempts() != null && member.getFailedPinAttempts() > 0)
                || member.getPinLockedUntil() != null) {
            member.setFailedPinAttempts(0);
            member.setPinLockedUntil(null);
        }
    }

    /** OTP verification / sending is blocked while the OTP lock runs. */
    public void assertOtpNotLocked(Member member) {
        LocalDateTime until = member.getOtpLockedUntil();
        LocalDateTime now = LocalDateTime.now();
        if (until != null && until.isAfter(now)) {
            throw new TooManyRequestsException(ErrorConstant.OTP_LOCKED, secondsUntil(until, now));
        }
    }

    private static long secondsUntil(LocalDateTime until, LocalDateTime now) {
        return Math.max(1, Duration.between(now, until).getSeconds());
    }
}
