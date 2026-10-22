package com.vyra.be_virtual_your_assets.integration;

import com.vyra.be_virtual_your_assets.config.SecurityPolicy;
import com.vyra.be_virtual_your_assets.constant.ActiveStatus;
import com.vyra.be_virtual_your_assets.constant.ErrorConstant;
import com.vyra.be_virtual_your_assets.constant.transaction.TransactionType;
import com.vyra.be_virtual_your_assets.dto.BaseResponse;
import com.vyra.be_virtual_your_assets.dto.member.UpdateProfileRequest;
import com.vyra.be_virtual_your_assets.dto.member.UpdateProfileResponse;
import com.vyra.be_virtual_your_assets.dto.member.VerifyContactChangeRequest;
import com.vyra.be_virtual_your_assets.dto.wallet.CreateWalletRequest;
import com.vyra.be_virtual_your_assets.dto.wallet.TransactionWalletRequest;
import com.vyra.be_virtual_your_assets.entity.Member;
import com.vyra.be_virtual_your_assets.entity.MemberToken;
import com.vyra.be_virtual_your_assets.exception.BusinessException;
import com.vyra.be_virtual_your_assets.exception.TooManyRequestsException;
import com.vyra.be_virtual_your_assets.ratelimit.DatabaseRateLimiter;
import com.vyra.be_virtual_your_assets.repository.MemberRepository;
import com.vyra.be_virtual_your_assets.repository.MemberTokenRepository;
import com.vyra.be_virtual_your_assets.repository.WalletStatementRepository;
import com.vyra.be_virtual_your_assets.service.MemberService;
import com.vyra.be_virtual_your_assets.service.OtpService;
import com.vyra.be_virtual_your_assets.service.WalletService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.PessimisticLockingFailureException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.verify;

/**
 * Real-database checks for the security hardening: cross-instance rate limiting, database lock
 * timeout and the OTP-verified e-mail / phone change. Opt-in (needs PostgreSQL):
 *
 * <pre>
 *   DATABASE_URL=jdbc:postgresql://localhost:5432/scratch DATABASE_USERNAME=... DATABASE_PASSWORD=...
 *   MAIL_USERNAME=x MAIL_PASSWORD=x CRYPTO_SECRET_KEY=0123456789abcdef0123456789abcdef
 *   DB_LOCK_TIMEOUT_MS=1500
 *   mvn test -Dtest=SecurityHardeningIT -Dvyra.it.db=true
 * </pre>
 */
@SpringBootTest
@EnabledIfSystemProperty(named = "vyra.it.db", matches = ".+")
class SecurityHardeningIT {

    @Autowired private JdbcTemplate jdbcTemplate;
    @Autowired private MemberService memberService;
    @Autowired private WalletService walletService;
    @Autowired private MemberRepository memberRepository;
    @Autowired private MemberTokenRepository memberTokenRepository;
    @Autowired private WalletStatementRepository walletStatementRepository;
    @Autowired private BCryptPasswordEncoder passwordEncoder;
    @Autowired private PlatformTransactionManager transactionManager;
    @Autowired private SecurityPolicy securityPolicy;

    /** Replaces the mail sender path; the OTP that "was e-mailed" is captured from this mock. */
    @MockitoBean private OtpService otpService;

    // ------------------------------------------------------------------ helpers
    private String randomPhone() {
        return "628" + (100_000_000 + ThreadLocalRandom.current().nextInt(899_999_999));
    }

    /** Short unique suffix: the e-mail column is 50 characters. */
    private static String uniq() {
        return UUID.randomUUID().toString().replace("-", "").substring(0, 10);
    }

    private Member newMember(String email, String phone) {
        Member member = new Member();
        member.setFirstName("Budi");
        member.setLastName("Santoso");
        member.setEmail(email);
        member.setPhoneNumber(phone);
        member.setPin(passwordEncoder.encode("123456"));
        member.setStatus(ActiveStatus.ACTIVE);
        member.setCreatedAt(LocalDateTime.now());
        member = memberRepository.save(member);

        CreateWalletRequest wallet = new CreateWalletRequest();
        wallet.setMemberId(member.getId());
        wallet.setPhoneNumber(phone);
        walletService.createWallet(wallet);
        return member;
    }

    private MemberToken token(String memberId, String value) {
        MemberToken token = new MemberToken();
        token.setUserId(memberId);
        token.setAccessToken(value);
        token.setRefreshToken("r-" + value);
        token.setDeviceId("d");
        token.setDeviceName("d");
        token.setIpAddress("127.0.0.1");
        token.setAccessTokenExpiredAt(LocalDateTime.now().plusMinutes(5));
        token.setRefreshTokenExpiredAt(LocalDateTime.now().plusDays(1));
        token.setCreatedAt(LocalDateTime.now());
        return memberTokenRepository.save(token);
    }

    private String lastSentOtp() {
        ArgumentCaptor<String> otp = ArgumentCaptor.forClass(String.class);
        verify(otpService, atLeastOnce()).sendOtp(anyString(), anyString(), anyString(), otp.capture());
        return otp.getValue();
    }

    private UpdateProfileRequest profileRequest(String email, String phone, String pin) {
        UpdateProfileRequest request = new UpdateProfileRequest();
        request.setEmail(email);
        request.setPhoneNumber(phone);
        request.setPin(pin);
        return request;
    }

    // ------------------------------------------------------------------ rate limiting
    @Test
    void rateLimit_isSharedByEveryInstanceThatUsesTheSameDatabase() throws Exception {
        // two limiter objects stand for two backend instances
        DatabaseRateLimiter instanceA = new DatabaseRateLimiter(jdbcTemplate, "database");
        DatabaseRateLimiter instanceB = new DatabaseRateLimiter(jdbcTemplate, "database");
        String key = "it-login|" + UUID.randomUUID();

        AtomicInteger allowed = new AtomicInteger();
        ExecutorService pool = Executors.newFixedThreadPool(4);
        List<Future<?>> futures = new ArrayList<>();
        for (int t = 0; t < 4; t++) {
            final DatabaseRateLimiter limiter = (t % 2 == 0) ? instanceA : instanceB;
            futures.add(pool.submit(() -> {
                for (int i = 0; i < 10; i++) {
                    if (limiter.tryAcquire(key, 20, 60_000) == 0) allowed.incrementAndGet();
                }
            }));
        }
        for (Future<?> f : futures) f.get(60, TimeUnit.SECONDS);
        pool.shutdown();

        // 40 attempts against a limit of 20: exactly 20 pass, however they were spread over instances
        assertThat(allowed.get()).isEqualTo(20);
        assertThat(instanceB.tryAcquire(key, 20, 60_000)).isBetween(1L, 60L);
    }

    @Test
    void rateLimit_windowResetsAfterItExpires() throws Exception {
        DatabaseRateLimiter limiter = new DatabaseRateLimiter(jdbcTemplate, "database");
        String key = "it-window|" + UUID.randomUUID();

        assertThat(limiter.tryAcquire(key, 1, 1_000)).isZero();
        assertThat(limiter.tryAcquire(key, 1, 1_000)).isPositive();
        Thread.sleep(1_200);

        assertThat(limiter.tryAcquire(key, 1, 1_000)).isZero();
    }

    // ------------------------------------------------------------------ lock timeout
    @Test
    void lockTimeout_isAppliedToEveryPooledConnection() {
        String lockTimeout = jdbcTemplate.queryForObject("SHOW lock_timeout", String.class);
        String statementTimeout = jdbcTemplate.queryForObject("SHOW statement_timeout", String.class);

        assertThat(lockTimeout).isNotEqualTo("0");
        assertThat(statementTimeout).isNotEqualTo("0");
    }

    @Test
    void walletStaysLockedForTheHolder_otherWritersFailFastInsteadOfHanging() throws Exception {
        String phone = randomPhone();
        newMember("lock-" + uniq() + "@example.com", phone);

        CountDownLatch lockHeld = new CountDownLatch(1);
        CountDownLatch release = new CountDownLatch(1);
        ExecutorService pool = Executors.newFixedThreadPool(2);

        // holder: takes the row lock and keeps its transaction open
        Future<?> holder = pool.submit(() -> new TransactionTemplate(transactionManager).executeWithoutResult(s -> {
            walletStatementRepository.findByPhoneNumberForUpdate(phone).orElseThrow();
            lockHeld.countDown();
            try {
                release.await(30, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }));
        assertThat(lockHeld.await(20, TimeUnit.SECONDS)).isTrue();

        TransactionWalletRequest request = new TransactionWalletRequest();
        request.setTransactionId(UUID.randomUUID().toString());
        request.setPhoneNumber(phone);
        request.setAmount(BigDecimal.valueOf(100));
        request.setTransactionType(TransactionType.INCOME);

        long start = System.nanoTime();
        assertThatThrownBy(() -> walletService.createTransactionWallet(request))
                .isInstanceOf(PessimisticLockingFailureException.class);
        long waitedMs = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - start);

        // fails after about lock_timeout (not after the 30 s the holder would keep it)
        assertThat(waitedMs).isLessThan(15_000);

        release.countDown();
        holder.get(20, TimeUnit.SECONDS);
        pool.shutdown();

        // once the lock is gone the same call goes through
        walletService.createTransactionWallet(request);
        assertThat(walletStatementRepository.findByPhoneNumber(phone).orElseThrow().getBalance())
                .isEqualByComparingTo(BigDecimal.valueOf(100));
    }

    // ------------------------------------------------------------------ OTP-verified contact change
    @Test
    void emailChange_isOnlyAppliedAfterTheOtpAndEndsTheOtherSessions() {
        String oldEmail = "old-" + uniq() + "@example.com";
        String newEmail = "new-" + uniq() + "@example.com";
        Member member = newMember(oldEmail, randomPhone());
        token(member.getId(), "current-" + member.getId());
        token(member.getId(), "other-" + member.getId());

        BaseResponse<UpdateProfileResponse> staged =
                memberService.updateProfile(member.getId(), profileRequest(newEmail, null, "123456"));

        // step 1: staged only
        assertThat(staged.getResponseStatus()).isEqualTo(ErrorConstant.UPDATE_PROFILE_PENDING.getCode());
        assertThat(staged.getData().isContactChangePending()).isTrue();
        assertThat(memberRepository.findByEmailIgnoreCase(oldEmail)).isPresent();
        assertThat(memberRepository.findByEmailIgnoreCase(newEmail)).isEmpty();
        verify(otpService).sendOtp(anyString(), eq(newEmail), eq(member.getPhoneNumber()), anyString());
        String otp = lastSentOtp();

        // a wrong code changes nothing
        VerifyContactChangeRequest wrong = new VerifyContactChangeRequest();
        wrong.setOtpCode(otp.equals("000000") ? "111111" : "000000");
        assertThatThrownBy(() -> memberService.verifyContactChange(member.getId(), wrong, "current-" + member.getId()))
                .isInstanceOf(BusinessException.class);
        assertThat(memberRepository.findByEmailIgnoreCase(newEmail)).isEmpty();
        assertThat(memberTokenRepository.findByAccessToken("other-" + member.getId())).isPresent();

        // step 2: the right code applies it
        VerifyContactChangeRequest right = new VerifyContactChangeRequest();
        right.setOtpCode(otp);
        BaseResponse<UpdateProfileResponse> done =
                memberService.verifyContactChange(member.getId(), right, "current-" + member.getId());

        assertThat(done.getResponseStatus()).isEqualTo(ErrorConstant.UPDATE_PROFILE_SUCCESS.getCode());
        assertThat(memberRepository.findByEmailIgnoreCase(newEmail)).isPresent();
        assertThat(memberRepository.findByEmailIgnoreCase(oldEmail)).isEmpty();
        assertThat(memberTokenRepository.findByAccessToken("current-" + member.getId())).isPresent();
        assertThat(memberTokenRepository.findByAccessToken("other-" + member.getId())).isEmpty();

        // the code is single use
        assertThatThrownBy(() -> memberService.verifyContactChange(member.getId(), right, "current-" + member.getId()))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    void phoneChange_movesTheWalletOnlyAfterTheOtp() {
        String oldPhone = randomPhone();
        String newPhone = randomPhone();
        Member member = newMember("phone-" + uniq() + "@example.com", oldPhone);

        memberService.updateProfile(member.getId(), profileRequest(null, newPhone, "123456"));

        // OTP of a phone change goes to the CURRENT e-mail (no SMS channel); nothing moved yet
        verify(otpService, atLeastOnce()).sendOtp(anyString(), eq(member.getEmail()), eq(oldPhone), anyString());
        assertThat(walletStatementRepository.findByPhoneNumber(oldPhone)).isPresent();
        assertThat(walletStatementRepository.findByPhoneNumber(newPhone)).isEmpty();
        assertThat(memberRepository.findByPhoneNumber(newPhone)).isEmpty();

        VerifyContactChangeRequest code = new VerifyContactChangeRequest();
        code.setOtpCode(lastSentOtp());
        memberService.verifyContactChange(member.getId(), code, null);

        assertThat(memberRepository.findByPhoneNumber(newPhone)).isPresent();
        assertThat(memberRepository.findByPhoneNumber(oldPhone)).isEmpty();
        assertThat(walletStatementRepository.findByPhoneNumber(newPhone)).isPresent();
        assertThat(walletStatementRepository.findByPhoneNumber(oldPhone)).isEmpty();
    }

    @Test
    void contactChange_wrongPinNeverStagesAnything() {
        Member member = newMember("pin-" + uniq() + "@example.com", randomPhone());

        assertThatThrownBy(() -> memberService.updateProfile(member.getId(),
                profileRequest("x-" + uniq() + "@example.com", null, "000000")))
                .isInstanceOf(BusinessException.class);

        // the failed PIN is counted in its own transaction, so it survives the rollback
        Integer attempts = jdbcTemplate.queryForObject(
                "SELECT COALESCE(failed_pin_attempts, 0) FROM idp.member WHERE id = ?", Integer.class, member.getId());
        assertThat(attempts).isEqualTo(1);
        Integer staged = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM idp.member_contact_change WHERE member_id = ?", Integer.class, member.getId());
        assertThat(staged).isZero();
    }

    @Test
    void contactChange_tooManyWrongCodes_locksOtpVerificationButDoesNotSuspend() {
        Member member = newMember("lockotp-" + uniq() + "@example.com", randomPhone());
        memberService.updateProfile(member.getId(),
                profileRequest("n-" + uniq() + "@example.com", null, "123456"));
        String realOtp = lastSentOtp();
        VerifyContactChangeRequest wrong = new VerifyContactChangeRequest();
        wrong.setOtpCode(realOtp.equals("000000") ? "111111" : "000000");

        for (int i = 0; i < 3; i++) {
            assertThatThrownBy(() -> memberService.verifyContactChange(member.getId(), wrong, null))
                    .isInstanceOf(BusinessException.class);
        }
        assertThatThrownBy(() -> memberService.verifyContactChange(member.getId(), wrong, null))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("temporarily locked");

        // even the correct code is refused while the lock runs; the account stays ACTIVE
        VerifyContactChangeRequest right = new VerifyContactChangeRequest();
        right.setOtpCode(realOtp);
        assertThatThrownBy(() -> memberService.verifyContactChange(member.getId(), right, null))
                .isInstanceOf(TooManyRequestsException.class);
        assertThat(memberRepository.findById(member.getId()).orElseThrow().getStatus()).isEqualTo(ActiveStatus.ACTIVE);
        assertThat(securityPolicy.getOtpLockMinutes()).isPositive();
    }
}
