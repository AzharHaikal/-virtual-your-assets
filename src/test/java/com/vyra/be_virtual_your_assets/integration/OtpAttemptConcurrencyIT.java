package com.vyra.be_virtual_your_assets.integration;

import com.vyra.be_virtual_your_assets.constant.ErrorConstant;
import com.vyra.be_virtual_your_assets.constant.OtpType;
import com.vyra.be_virtual_your_assets.dto.auth.VerifyOtpRequest;
import com.vyra.be_virtual_your_assets.entity.Member;
import com.vyra.be_virtual_your_assets.entity.MemberOtp;
import com.vyra.be_virtual_your_assets.exception.BusinessException;
import com.vyra.be_virtual_your_assets.repository.MemberOtpRepository;
import com.vyra.be_virtual_your_assets.repository.MemberRepository;
import com.vyra.be_virtual_your_assets.service.AuthenticationService;
import com.vyra.be_virtual_your_assets.service.WalletService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Parallel wrong OTP guesses must be counted one by one: at most 4 guesses are checked against an OTP,
 * then it is deleted and verification is locked. Before the row lock in {@code findLatestForUpdate} the
 * guesses all read the same attempt count and none of them reached the limit.
 * <p>
 * Opt-in, needs an empty PostgreSQL database (see {@link SchemaMigrationIT}): {@code -Dvyra.it.db=true}.
 */
@SpringBootTest
@EnabledIfSystemProperty(named = "vyra.it.db", matches = ".+")
class OtpAttemptConcurrencyIT {

    @Autowired private AuthenticationService authenticationService;
    @Autowired private MemberRepository memberRepository;
    @Autowired private MemberOtpRepository memberOtpRepository;
    @Autowired private WalletService walletService;
    @Autowired private BCryptPasswordEncoder passwordEncoder;

    @Test
    void parallelWrongGuesses_stopAtTheAttemptLimit() throws Exception {
        Member member = ItMembers.create(memberRepository, walletService);
        MemberOtp otp = new MemberOtp();
        otp.setPhoneNumber(member.getPhoneNumber());
        otp.setOtpCode(passwordEncoder.encode("123456"));
        otp.setOtpType(OtpType.FORGOT_PIN);
        otp.setAttempts(0);
        otp.setExpiredAt(LocalDateTime.now().plusMinutes(5));
        otp.setCreatedAt(LocalDateTime.now());
        memberOtpRepository.saveAndFlush(otp);

        List<Callable<?>> guesses = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            String code = String.format("%06d", 900_000 + i);
            guesses.add(() -> {
                VerifyOtpRequest request = new VerifyOtpRequest();
                request.setEmail(member.getEmail());
                request.setOtpCode(code);
                request.setOtpType(OtpType.FORGOT_PIN);
                return authenticationService.verifyOtp(request);
            });
        }
        List<Throwable> errors = ItMembers.runTogether(guesses);

        assertThat(errors).allSatisfy(e -> assertThat(e).isInstanceOf(BusinessException.class));
        List<ErrorConstant> outcomes = errors.stream().map(e -> ((BusinessException) e).getErrorConstant()).toList();
        assertThat(outcomes).filteredOn(ErrorConstant.OTP_INVALID::equals).hasSize(3);
        assertThat(outcomes).filteredOn(ErrorConstant.MAX_ATTEMPTS_REACHED::equals).hasSize(1);
        assertThat(outcomes).filteredOn(ErrorConstant.OTP_NOT_FOUND::equals).hasSize(1);
        assertThat(memberRepository.findById(member.getId()).orElseThrow().getOtpLockedUntil()).isNotNull();
        assertThat(memberOtpRepository.findTopByPhoneNumberAndOtpTypeOrderByCreatedAtDesc(
                member.getPhoneNumber(), OtpType.FORGOT_PIN)).isEmpty();
    }
}
