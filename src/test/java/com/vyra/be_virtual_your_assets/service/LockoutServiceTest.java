package com.vyra.be_virtual_your_assets.service;

import com.vyra.be_virtual_your_assets.config.SecurityPolicy;
import com.vyra.be_virtual_your_assets.constant.ErrorConstant;
import com.vyra.be_virtual_your_assets.constant.MemberActivityEvent;
import com.vyra.be_virtual_your_assets.entity.Member;
import com.vyra.be_virtual_your_assets.exception.TooManyRequestsException;
import com.vyra.be_virtual_your_assets.repository.MemberRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.transaction.PlatformTransactionManager;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LockoutServiceTest {

    @Mock private MemberRepository memberRepository;
    @Mock private MemberActivityService memberActivityService;
    @Mock private PlatformTransactionManager transactionManager;

    private LockoutService lockoutService;
    private Member member;

    @BeforeEach
    void setUp() {
        lockoutService = new LockoutService(memberRepository, memberActivityService, new SecurityPolicy(), transactionManager);
        member = new Member();
        member.setId("member-id-001");
        member.setPhoneNumber("628123456789");
    }

    // ---- PIN lock ---------------------------------------------------------
    @Test
    void assertPinNotLocked_noLock_shouldPass() {
        assertDoesNotThrow(() -> lockoutService.assertPinNotLocked(member));
        verify(memberRepository, never()).clearPinLock(any());
    }

    @Test
    void assertPinNotLocked_activeLock_shouldThrow429WithRemainingTime() {
        member.setPinLockedUntil(LocalDateTime.now().plusMinutes(7));

        TooManyRequestsException ex = assertThrows(TooManyRequestsException.class,
                () -> lockoutService.assertPinNotLocked(member));

        assertThat(ex.getErrorConstant()).isEqualTo(ErrorConstant.ACCOUNT_LOCKED);
        assertThat(ex.getRetryAfterSeconds()).isBetween(6 * 60L, 7 * 60L);
        assertThat(ex.getMessage()).contains("minutes");
    }

    @Test
    void assertPinNotLocked_expiredLock_shouldResetCountersInOwnTransaction() {
        member.setPinLockedUntil(LocalDateTime.now().minusSeconds(5));
        member.setFailedPinAttempts(5);

        lockoutService.assertPinNotLocked(member);

        verify(memberRepository).clearPinLock("member-id-001");
        assertThat(member.getPinLockedUntil()).isNull();
        assertThat(member.getFailedPinAttempts()).isZero();
    }

    // ---- failures ---------------------------------------------------------
    @Test
    void onPinFailure_belowLimit_shouldCountButNotLock() {
        when(memberRepository.getFailedPinAttempts("member-id-001")).thenReturn(2);

        assertDoesNotThrow(() -> lockoutService.onPinFailure(member));

        verify(memberRepository).incrementFailedPinAttempts("member-id-001");
        verify(memberRepository, never()).lockPin(any(), any());
        assertThat(member.getPinLockedUntil()).isNull();
    }

    @Test
    void onPinFailure_reachingLimit_shouldLockAuditAndThrow429() {
        when(memberRepository.getFailedPinAttempts("member-id-001")).thenReturn(5);

        TooManyRequestsException ex = assertThrows(TooManyRequestsException.class,
                () -> lockoutService.onPinFailure(member));

        assertThat(ex.getErrorConstant()).isEqualTo(ErrorConstant.ACCOUNT_LOCKED);
        assertThat(ex.getRetryAfterSeconds()).isEqualTo(15 * 60);
        verify(memberRepository).lockPin(eq("member-id-001"), any(LocalDateTime.class));
        verify(memberActivityService).createMemberActivity("628123456789", MemberActivityEvent.ACCOUNT_LOCKED);
        assertThat(member.getPinLockedUntil()).isAfter(LocalDateTime.now().plusMinutes(14));
    }

    // ---- success ----------------------------------------------------------
    @Test
    void onPinSuccess_shouldForgetEarlierFailures() {
        member.setFailedPinAttempts(3);

        lockoutService.onPinSuccess(member);

        assertThat(member.getFailedPinAttempts()).isZero();
        assertThat(member.getPinLockedUntil()).isNull();
    }

    // ---- OTP lock ---------------------------------------------------------
    @Test
    void assertOtpNotLocked_activeLock_shouldThrow429() {
        member.setOtpLockedUntil(LocalDateTime.now().plusMinutes(5));

        TooManyRequestsException ex = assertThrows(TooManyRequestsException.class,
                () -> lockoutService.assertOtpNotLocked(member));

        assertThat(ex.getErrorConstant()).isEqualTo(ErrorConstant.OTP_LOCKED);
    }

    @Test
    void assertOtpNotLocked_expiredOrNone_shouldPass() {
        assertDoesNotThrow(() -> lockoutService.assertOtpNotLocked(member));
        member.setOtpLockedUntil(LocalDateTime.now().minusMinutes(1));
        assertDoesNotThrow(() -> lockoutService.assertOtpNotLocked(member));
    }
}
