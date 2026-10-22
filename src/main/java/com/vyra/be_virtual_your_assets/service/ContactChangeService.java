package com.vyra.be_virtual_your_assets.service;

import com.vyra.be_virtual_your_assets.config.SecurityPolicy;
import com.vyra.be_virtual_your_assets.constant.ActiveStatus;
import com.vyra.be_virtual_your_assets.constant.ErrorConstant;
import com.vyra.be_virtual_your_assets.constant.MemberActivityEvent;
import com.vyra.be_virtual_your_assets.constant.OtpType;
import com.vyra.be_virtual_your_assets.dto.auth.VerifyOtpRequest;
import com.vyra.be_virtual_your_assets.dto.member.UpdateProfileResponse;
import com.vyra.be_virtual_your_assets.dto.transaction.TransactionUpdateRequest;
import com.vyra.be_virtual_your_assets.dto.wallet.WalletUpdateRequest;
import com.vyra.be_virtual_your_assets.entity.Member;
import com.vyra.be_virtual_your_assets.entity.MemberContactChange;
import com.vyra.be_virtual_your_assets.entity.MemberOtp;
import com.vyra.be_virtual_your_assets.exception.BusinessException;
import com.vyra.be_virtual_your_assets.exception.TooManyRequestsException;
import com.vyra.be_virtual_your_assets.repository.MemberContactChangeRepository;
import com.vyra.be_virtual_your_assets.repository.MemberOtpRepository;
import com.vyra.be_virtual_your_assets.repository.MemberRepository;
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
 * Changing the e-mail address or phone number of a member is a two-step operation:
 * <ol>
 *   <li>{@link #stage}: after the PIN check, the new value is only recorded and an OTP is
 *       e-mailed - to the NEW address when the e-mail changes (proves the member owns it),
 *       otherwise to the current address (OTPs are delivered by e-mail only, so a new phone
 *       number cannot receive one).</li>
 *   <li>{@link #verify}: the correct OTP applies the change, synchronises wallet and
 *       transaction records and ends every other session of the member.</li>
 * </ol>
 * Until step 2 nothing about the member's login identity changes.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ContactChangeService {

    private final MemberRepository memberRepository;
    private final MemberOtpRepository memberOtpRepository;
    private final MemberContactChangeRepository contactChangeRepository;
    private final MemberTokenRepository memberTokenRepository;
    private final ValidationService validationService;
    private final LockoutService lockoutService;
    private final OtpService otpService;
    private final WalletService walletService;
    private final TransactionService transactionService;
    private final MemberActivityService memberActivityService;
    private final BCryptPasswordEncoder passwordEncoder;
    private final SecurityPolicy securityPolicy;

    /**
     * Records the requested change and sends the OTP. The caller has already verified the PIN.
     *
     * @param newEmail normalised new e-mail address or null when unchanged
     * @param newPhone new phone number or null when unchanged
     * @return the e-mail address the OTP was sent to
     */
    @Transactional
    public String stage(Member member, String newEmail, String newPhone) {
        lockoutService.assertOtpNotLocked(member);
        assertCooldown(member);
        validationService.assertContactAvailable(member, newEmail, newPhone);

        MemberContactChange pending = contactChangeRepository.findByMemberId(member.getId())
                .orElseGet(MemberContactChange::new);
        pending.setMemberId(member.getId());
        pending.setNewEmail(newEmail);
        pending.setNewPhoneNumber(newPhone);
        pending.setCreatedAt(LocalDateTime.now());
        contactChangeRepository.save(pending);

        memberActivityService.createMemberActivity(member.getPhoneNumber(), MemberActivityEvent.REQUEST_CHANGE_CONTACT);
        return sendOtp(member, targetEmail(member, pending));
    }

    @Transactional(noRollbackFor = {BusinessException.class, TooManyRequestsException.class})
    public UpdateProfileResponse verify(String memberId, String otpCode, String currentAccessToken) {
        Member member = activeMember(memberId);
        lockoutService.assertOtpNotLocked(member);

        MemberContactChange pending = contactChangeRepository.findByMemberId(memberId)
                .orElseThrow(() -> new BusinessException(ErrorConstant.OTP_NOT_FOUND));

        VerifyOtpRequest verification = new VerifyOtpRequest();
        verification.setEmail(member.getEmail());
        verification.setOtpCode(otpCode);
        verification.setOtpType(OtpType.CHANGE_CONTACT);
        MemberOtp otp = validationService.verifyOtp(member, verification);

        String oldPhone = member.getPhoneNumber();
        String newEmail = pending.getNewEmail() != null ? pending.getNewEmail() : member.getEmail();
        String newPhone = pending.getNewPhoneNumber() != null ? pending.getNewPhoneNumber() : oldPhone;

        // Somebody else may have registered the address / number since the request was staged.
        validationService.assertContactAvailable(member, pending.getNewEmail(), pending.getNewPhoneNumber());

        if (!newPhone.equals(oldPhone)) {
            WalletUpdateRequest walletRequest = new WalletUpdateRequest();
            walletRequest.setOldPhoneNumber(oldPhone);
            walletRequest.setNewPhoneNumber(newPhone);
            walletService.updateMemberWallet(walletRequest);
        }
        TransactionUpdateRequest transactionRequest = new TransactionUpdateRequest();
        transactionRequest.setOldPhoneNumber(oldPhone);
        transactionRequest.setNewPhoneNumber(newPhone);
        transactionRequest.setEmail(newEmail);
        transactionService.updateTransaction(transactionRequest);

        member.setEmail(newEmail);
        member.setPhoneNumber(newPhone);
        member.setModifiedBy(newPhone);
        member.setUpdatedAt(LocalDateTime.now());
        memberRepository.save(member);

        memberOtpRepository.delete(otp);
        contactChangeRepository.delete(pending);

        // The login identity changed: every other device / stolen session has to log in again.
        if (currentAccessToken != null) {
            memberTokenRepository.deleteByUserIdAndAccessTokenNot(member.getId(), currentAccessToken);
        } else {
            memberTokenRepository.deleteByUserId(member.getId());
        }

        memberActivityService.createMemberActivity(newPhone, MemberActivityEvent.SUCCESS_UPDATE_PROFILE);

        UpdateProfileResponse response = new UpdateProfileResponse();
        response.setFirstName(member.getFirstName());
        response.setLastName(member.getLastName());
        response.setEmail(member.getEmail());
        response.setPhoneNumber(member.getPhoneNumber());
        return response;
    }

    /** Sends a fresh OTP for the pending change; returns the address it went to. */
    @Transactional
    public String resend(String memberId) {
        Member member = activeMember(memberId);
        lockoutService.assertOtpNotLocked(member);
        MemberContactChange pending = contactChangeRepository.findByMemberId(memberId)
                .orElseThrow(() -> new BusinessException(ErrorConstant.OTP_NOT_FOUND));
        assertCooldown(member);
        return sendOtp(member, targetEmail(member, pending));
    }

    // ------------------------------------------------------------------------

    private Member activeMember(String memberId) {
        Member member = validationService.getMemberById(memberId);
        if (ActiveStatus.ACTIVE != member.getStatus()) {
            throw new BusinessException(ErrorConstant.MEMBER_NOT_ACTIVE);
        }
        return member;
    }

    private static String targetEmail(Member member, MemberContactChange pending) {
        return pending.getNewEmail() != null ? pending.getNewEmail() : member.getEmail();
    }

    private String sendOtp(Member member, String email) {
        memberOtpRepository.deleteByPhoneNumberAndOtpType(member.getPhoneNumber(), OtpType.CHANGE_CONTACT);

        String otp = OtpUtil.generateOtp();
        MemberOtp memberOtp = new MemberOtp();
        memberOtp.setPhoneNumber(member.getPhoneNumber());
        memberOtp.setOtpCode(passwordEncoder.encode(otp));
        memberOtp.setOtpType(OtpType.CHANGE_CONTACT);
        memberOtp.setAttempts(0);
        memberOtp.setExpiredAt(OtpUtil.getExpiredTime());
        memberOtp.setCreatedAt(LocalDateTime.now());
        memberOtpRepository.save(memberOtp);

        String fullName = member.getFirstName().trim() + " " + member.getLastName().trim();
        otpService.sendOtp(fullName, email, member.getPhoneNumber(), otp);
        return email;
    }

    private void assertCooldown(Member member) {
        memberOtpRepository
                .findTopByPhoneNumberAndOtpTypeOrderByCreatedAtDesc(member.getPhoneNumber(), OtpType.CHANGE_CONTACT)
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
