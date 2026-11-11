package com.vyra.be_virtual_your_assets.service;

import com.vyra.be_virtual_your_assets.config.SecurityPolicy;
import com.vyra.be_virtual_your_assets.constant.ActiveStatus;
import com.vyra.be_virtual_your_assets.constant.ErrorConstant;
import com.vyra.be_virtual_your_assets.constant.MemberActivityEvent;
import com.vyra.be_virtual_your_assets.constant.OtpType;
import com.vyra.be_virtual_your_assets.dto.BaseResponse;
import com.vyra.be_virtual_your_assets.dto.auth.*;
import com.vyra.be_virtual_your_assets.dto.wallet.CreateWalletRequest;
import com.vyra.be_virtual_your_assets.dto.wallet.CreateWalletResponse;
import com.vyra.be_virtual_your_assets.entity.Member;
import com.vyra.be_virtual_your_assets.entity.MemberOtp;
import com.vyra.be_virtual_your_assets.entity.MemberToken;
import com.vyra.be_virtual_your_assets.exception.BusinessException;
import com.vyra.be_virtual_your_assets.exception.TooManyRequestsException;
import com.vyra.be_virtual_your_assets.repository.MemberOtpRepository;
import com.vyra.be_virtual_your_assets.repository.MemberRepository;
import com.vyra.be_virtual_your_assets.repository.MemberTokenRepository;
import com.vyra.be_virtual_your_assets.util.OtpUtil;
import com.vyra.be_virtual_your_assets.util.TokenUtil;
import io.micrometer.tracing.annotation.NewSpan;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.logging.log4j.util.InternalException;
import org.springframework.beans.BeanUtils;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthenticationService {
    static final String UNKNOWN_DEVICE = "Unknown device";

    private final MemberRepository memberRepository;
    private final MemberOtpRepository memberOtpRepository;
    private final MemberTokenRepository memberTokenRepository;
    private final MemberActivityService memberActivityService;
    private final OtpService otpService;
    private final ValidationService validationService;
    private final WalletService walletService;
    private final BCryptPasswordEncoder passwordEncoder;
    private final LockoutService lockoutService;
    private final SecurityPolicy securityPolicy;
    private final TimeService timeService;
    private final AnnouncementService announcementService;

    @NewSpan
    @Transactional
    public BaseResponse<RegisterResponse> registerMember(RegisterRequest request) {
        log.info("[START] authenticationService.registerMember");
        createMemberActivity(request.getPhoneNumber(), MemberActivityEvent.ATTEMPT_REGISTER);

        log.info("Validate member already exist");
        validationService.validateDataExistRegister(request.getPhoneNumber(), request.getEmail());
        String timezone = StringUtils.isBlank(request.getTimezone())
                ? timeService.defaultZone().getId() : timeService.requireValidZoneId(request.getTimezone());

        Member member = new Member();
        BeanUtils.copyProperties(request, member);
        member.setTimezone(timezone);
        member.setEmail(request.getEmail().toLowerCase().trim());
        member.setPin(passwordEncoder.encode(request.getPin()));
        member.setStatus(ActiveStatus.INACTIVE);
        member.setCreatedBy(request.getPhoneNumber());
        member.setCreatedAt(LocalDateTime.now());
        memberRepository.save(member);
        log.info("Save data member");

        String otp = generateOtp(OtpType.REGISTER, request.getPhoneNumber());
        String fullName = request.getFirstName().trim() + " " + request.getLastName().trim();

        // Create wallet
        try {
            log.info("Create member wallet");
            CreateWalletRequest createWalletRequest = new CreateWalletRequest();
            createWalletRequest.setPhoneNumber(request.getPhoneNumber());
            createWalletRequest.setMemberId(member.getId());

            BaseResponse<CreateWalletResponse> createWalletResponse = walletService.createWallet(createWalletRequest);
            if (!ErrorConstant.CREATE_WALLET_SUCCESS.getCode().equals(createWalletResponse.getResponseStatus())) {
                log.info("Failed when creating member wallet");
                throw new BusinessException(ErrorConstant.CREATE_WALLET_FAILED);
            }
        } catch (BusinessException e) {
            log.warn("register encountered an exception: {}", e.getMessage());
            createMemberActivity(request.getPhoneNumber(), MemberActivityEvent.FAILED_REGISTER);
            throw e;
        } catch (Exception e) {
            log.error("Unexpected error during register, message: {}{}", e.getMessage(), e);
            createMemberActivity(request.getPhoneNumber(), MemberActivityEvent.FAILED_REGISTER);
            throw new InternalException(ErrorConstant.INTERNAL_SERVER_ERROR.getMessage());
        }

        log.info("Send async OTP to email");
        createMemberActivity(request.getPhoneNumber(), MemberActivityEvent.ATTEMPT_GENERATE_OTP_REGISTER);

        // Send OTP async
        otpService.sendOtp(fullName, request.getEmail(), request.getPhoneNumber(), otp);
        log.info("Successfully send OTP");

        RegisterResponse response = new RegisterResponse();
        response.setMemberId(member.getId());
        response.setFullName(fullName);
        response.setEmail(member.getEmail());
        response.setPhoneNumber(member.getPhoneNumber());

        createMemberActivity(request.getPhoneNumber(), MemberActivityEvent.SUCCESS_REGISTER);
        log.info("[END] authenticationService.registerMember successfully");

        return new BaseResponse<>(
                ErrorConstant.REGISTER_SUCCESS.getCode(),
                ErrorConstant.REGISTER_SUCCESS.getMessage(),
                response
        );
    }

    @NewSpan
    @Transactional
    public BaseResponse<Void> resendOtp(ResendOtpRequest request) {
        log.info("[START] authenticationService.resendOtp email: {} ", request.getEmail());
        Member member = validationService.getMemberByEmailIgnoreCase(request.getEmail());

        lockoutService.assertOtpNotLocked(member);

        if (!isOtpAllowed(member, request.getOtpType())) {
            // Same generic answer as a normal resend, but nothing is generated or sent.
            log.warn("Resend OTP ignored, member status {} not eligible for {}. memberId: {}",
                    member.getStatus(), request.getOtpType(), member.getId());
            return new BaseResponse<>(
                    ErrorConstant.RESEND_OTP.getCode(),
                    ErrorConstant.RESEND_OTP.getMessage(),
                    null
            );
        }

        assertOtpCooldown(member, request.getOtpType());

        memberOtpRepository.deleteByPhoneNumberAndOtpType(member.getPhoneNumber(), request.getOtpType());

        String otp = generateOtp(request.getOtpType(), member.getPhoneNumber());
        String fullName = member.getFirstName().trim() + " " + member.getLastName().trim();

        log.info("Resend async OTP to email. memberId: {}", member.getId());
        createMemberActivity(member.getPhoneNumber(), MemberActivityEvent.ATTEMPT_RESEND_OTP);

        // Send OTP async
        otpService.sendOtp(fullName, request.getEmail(), member.getPhoneNumber(), otp);
        log.info("Successfully resend OTP to email for memberId: {}", member.getId());

        createMemberActivity(member.getPhoneNumber(), MemberActivityEvent.SUCCESS_RESEND_OTP);
        log.info("[END] authenticationService.resendOtp successfully memberId: {}", member.getId());

        return new BaseResponse<>(
                ErrorConstant.RESEND_OTP.getCode(),
                ErrorConstant.RESEND_OTP.getMessage(),
                null
        );
    }

    @NewSpan
    @Transactional(noRollbackFor = BusinessException.class)
    public BaseResponse<Void> verifyOtp(VerifyOtpRequest request) {
        Member member = validationService.getMemberByEmailIgnoreCase(request.getEmail());
        log.info("[START] authenticationService.verifyOtp memberId: {}", member.getId());

        log.info("Attempt verify otp. memberId: {}", member.getId());
        createMemberActivity(member.getPhoneNumber(), MemberActivityEvent.ATTEMPT_VERIFY_OTP);

        lockoutService.assertOtpNotLocked(member);

        // An OTP type is only valid for the account state it was issued for. Without this a
        // SUSPENDED member could request any OTP and verify it to reactivate itself.
        if (!isOtpAllowed(member, request.getOtpType())) {
            log.warn("Verify OTP rejected, member status {} not eligible for {}. memberId: {}",
                    member.getStatus(), request.getOtpType(), member.getId());
            throw new BusinessException(ErrorConstant.OTP_NOT_FOUND);
        }

        MemberOtp memberOtp = validationService.verifyOtp(member, request);

        if (OtpType.FORGOT_PIN == request.getOtpType()) {
            // Keep the OTP as proof of verification: /auth/reset-pin consumes it. Account status is untouched.
            memberOtp.setVerifiedAt(LocalDateTime.now());
            memberOtp.setExpiredAt(OtpUtil.getResetWindowExpiredTime());
            memberOtpRepository.save(memberOtp);
        } else {
            if (OtpType.REGISTER == request.getOtpType()) {
                member.setStatus(ActiveStatus.ACTIVE);
                memberRepository.save(member);
                // Announcements already running go to the new inbox too, once the account is saved.
                afterCommit(() -> announcementService.deliverActiveTo(member));
            }
            memberOtpRepository.delete(memberOtp);
        }

        createMemberActivity(member.getPhoneNumber(), MemberActivityEvent.SUCCESS_VERIFY_OTP);
        log.info("[END] authenticationService.verifyOtp successfully memberId: {}", member.getId());

        return new BaseResponse<>(
                ErrorConstant.VERIFY_OTP_SUCCESS.getCode(),
                ErrorConstant.VERIFY_OTP_SUCCESS.getMessage(),
                null
        );
    }

    @NewSpan
    @Transactional
    public BaseResponse<LoginResponse> loginMember(LoginRequest request) {
        Member member = validationService.getMemberByEmailOrPhoneNumber(request.getEmail(), request.getPhoneNumber());
        log.info("[START] authenticationService.loginMember memberId: {}", member.getId());

        log.info("Attempt login member. memberId: {}", member.getId());
        createMemberActivity(member.getPhoneNumber(), MemberActivityEvent.ATTEMPT_LOGIN);

        if (ActiveStatus.ACTIVE != member.getStatus()) {
            log.info("Login failed member not active. memberId: {}", member.getId());
            createMemberActivity(member.getPhoneNumber(), MemberActivityEvent.FAILED_LOGIN);
            throw new BusinessException(ErrorConstant.MEMBER_NOT_ACTIVE);
        }

        // Locked members are refused without even looking at the PIN.
        lockoutService.assertPinNotLocked(member);

        if (!passwordEncoder.matches(request.getPin(), member.getPin())) {
            log.info("Login failed invalid pin. memberId: {}", member.getId());
            createMemberActivity(member.getPhoneNumber(), MemberActivityEvent.FAILED_LOGIN);
            lockoutService.onPinFailure(member); // throws 429 when this attempt reaches the limit
            throw new BusinessException(ErrorConstant.INVALID_PIN);
        }
        lockoutService.onPinSuccess(member);

        String accessToken = TokenUtil.generateAccessToken();
        String refreshToken = TokenUtil.generateRefreshToken();

        MemberToken memberToken = new MemberToken();
        memberToken.setUserId(member.getId());
        memberToken.setAccessToken(TokenUtil.hash(accessToken));
        memberToken.setRefreshToken(TokenUtil.hash(refreshToken));
        memberToken.setAccessTokenExpiredAt(TokenUtil.accessTokenExpiredAt());
        memberToken.setRefreshTokenExpiredAt(TokenUtil.refreshTokenExpiredAt());
        memberToken.setDeviceId(request.getDeviceId());
        // device_name is NOT NULL: without a default a login without it was a 500.
        memberToken.setDeviceName(StringUtils.defaultIfBlank(request.getDeviceName(), UNKNOWN_DEVICE));
        memberToken.setIpAddress(request.getIpAddress());
        memberToken.setCreatedAt(LocalDateTime.now());
        memberTokenRepository.save(memberToken);

        LoginResponse response = new LoginResponse();
        response.setMemberId(member.getId());
        response.setPhoneNumber(member.getPhoneNumber());
        response.setEmail(member.getEmail());
        response.setAccessToken(accessToken);
        response.setRefreshToken(refreshToken);
        response.setAccessTokenExpiredAt(memberToken.getAccessTokenExpiredAt());
        response.setRefreshTokenExpiredAt(memberToken.getRefreshTokenExpiredAt());

        createMemberActivity(member.getPhoneNumber(), MemberActivityEvent.SUCCESS_LOGIN);
        log.info("[END] authenticationService.loginMember successfully memberId: {}", member.getId());

        return new BaseResponse<>(
                ErrorConstant.LOGIN_SUCCESS.getCode(),
                ErrorConstant.LOGIN_SUCCESS.getMessage(),
                response
        );
    }

    @NewSpan
    @Transactional(noRollbackFor = BusinessException.class)
    public BaseResponse<RefreshTokenResponse> refreshToken(RefreshTokenRequest request) {
        MemberToken memberToken = memberTokenRepository.findByRefreshToken(TokenUtil.hash(request.getRefreshToken()))
                .orElseThrow(() -> new BusinessException(ErrorConstant.INVALID_REFRESH_TOKEN));

        if (memberToken.getRefreshTokenExpiredAt().isBefore(LocalDateTime.now())) {
            throw new BusinessException(ErrorConstant.REFRESH_TOKEN_EXPIRED);
        }

        // Suspended / deleted members cannot renew a session; the dead token is removed.
        Member tokenOwner = memberRepository.findById(memberToken.getUserId()).orElse(null);
        if (tokenOwner == null || ActiveStatus.ACTIVE != tokenOwner.getStatus()) {
            memberTokenRepository.delete(memberToken);
            throw new BusinessException(ErrorConstant.MEMBER_NOT_ACTIVE);
        }

        String newAccessToken = TokenUtil.generateAccessToken();
        String newRefreshToken = TokenUtil.generateRefreshToken();

        memberToken.setAccessToken(TokenUtil.hash(newAccessToken));
        memberToken.setRefreshToken(TokenUtil.hash(newRefreshToken));
        memberToken.setAccessTokenExpiredAt(TokenUtil.accessTokenExpiredAt());
        memberToken.setRefreshTokenExpiredAt(TokenUtil.refreshTokenExpiredAt());

        RefreshTokenResponse response = new RefreshTokenResponse();
        response.setAccessToken(newAccessToken);
        response.setRefreshToken(newRefreshToken);
        response.setAccessTokenExpiredAt(memberToken.getAccessTokenExpiredAt());
        response.setRefreshTokenExpiredAt(memberToken.getRefreshTokenExpiredAt());

        return new BaseResponse<>(
                ErrorConstant.REFRESH_TOKEN_SUCCESS.getCode(),
                ErrorConstant.REFRESH_TOKEN_SUCCESS.getMessage(),
                response
        );
    }

    @NewSpan
    @Transactional
    public BaseResponse<Void> forgotPin(ForgotPinRequest request) {
        Member member = validationService.getMemberByEmailIgnoreCase(request.getEmail());
        log.info("[START] authenticationService.forgotPin memberId: {}", member.getId());

        if (ActiveStatus.ACTIVE != member.getStatus()) {
            log.info("Forgot PIN rejected, member not active. memberId: {}", member.getId());
            throw new BusinessException(ErrorConstant.MEMBER_NOT_ACTIVE);
        }

        lockoutService.assertOtpNotLocked(member);
        assertOtpCooldown(member, OtpType.FORGOT_PIN);

        // A new request invalidates any earlier (possibly already verified) reset OTP.
        memberOtpRepository.deleteByPhoneNumberAndOtpType(member.getPhoneNumber(), OtpType.FORGOT_PIN);
        String otp = generateOtp(OtpType.FORGOT_PIN, member.getPhoneNumber());
        String fullName = member.getFirstName().trim() + " " + member.getLastName().trim();

        log.info("Send async OTP to email. memberId: {}", member.getId());
        createMemberActivity(member.getPhoneNumber(), MemberActivityEvent.ATTEMPT_GENERATE_FORGOT_PIN);

        // Send OTP async
        otpService.sendOtp(fullName, request.getEmail(), member.getPhoneNumber(), otp);
        log.info("Successfully send OTP to email for memberId: {}", member.getId());

        createMemberActivity(member.getPhoneNumber(), MemberActivityEvent.SUCCESS_GENERATE_FORGOT_PIN);
        log.info("[END] authenticationService.forgotPin successfully memberId: {}", member.getId());

        return new BaseResponse<>(
                ErrorConstant.FORGOT_PIN_OTP_SENT.getCode(),
                ErrorConstant.FORGOT_PIN_OTP_SENT.getMessage(),
                null
        );
    }

    @NewSpan
    @Transactional(noRollbackFor = BusinessException.class)
    public BaseResponse<Void> resetPin(ResetPinRequest request) {
        Member member = validationService.getMemberByEmailOrPhoneNumber(request.getEmail(), request.getPhoneNumber());
        log.info("[START] authenticationService.resetPin memberId: {}", member.getId());

        log.info("Attempt reset pin. memberId: {}", member.getId());
        createMemberActivity(member.getPhoneNumber(), MemberActivityEvent.ATTEMPT_RESET_PIN);

        MemberOtp verifiedOtp = requireVerifiedResetOtp(member);

        member.setPin(passwordEncoder.encode(request.getNewPin()));
        member.setUpdatedAt(LocalDateTime.now());
        memberRepository.save(member);

        // The proof of verification is single-use, and every existing session of the
        // member is ended so a previously stolen token does not survive the PIN reset.
        memberOtpRepository.delete(verifiedOtp);
        memberTokenRepository.deleteByUserId(member.getId());

        createMemberActivity(member.getPhoneNumber(), MemberActivityEvent.SUCCESS_RESET_PIN);
        log.info("[END] authenticationService.resetPin successfully memberId: {}", member.getId());

        return new BaseResponse<>(
                ErrorConstant.RESET_PIN_SUCCESS.getCode(),
                ErrorConstant.RESET_PIN_SUCCESS.getMessage(),
                null
        );
    }

    @NewSpan
    @Transactional
    public BaseResponse<Void> logoutMember(String memberId, String accessToken) {
        Member member = validationService.getMemberById(memberId);
        log.info("[START] authenticationService.logoutMember memberId: {}", member.getId());
        createMemberActivity(member.getPhoneNumber(), MemberActivityEvent.ATTEMPT_LOGOUT);
        memberTokenRepository.deleteByAccessToken(accessToken);
        createMemberActivity(member.getPhoneNumber(), MemberActivityEvent.SUCCESS_LOGOUT);
        log.info("[END] authenticationService.logoutMember successfully memberId: {}", member.getId());

        return new BaseResponse<>(
                ErrorConstant.LOGOUT_SUCCESS.getCode(),
                ErrorConstant.LOGOUT_SUCCESS.getMessage(),
                null
        );
    }

    /**
     * Which account states may request / verify which OTP type:
     * REGISTER only while INACTIVE (not yet verified); FORGOT_PIN and LOGIN only while ACTIVE.
     * SUSPENDED / DELETED members can never obtain a working OTP.
     */
    /** One OTP e-mail per type per cool-down period: stops e-mail flooding of a member. */
    private void assertOtpCooldown(Member member, OtpType otpType) {
        memberOtpRepository
                .findTopByPhoneNumberAndOtpTypeOrderByCreatedAtDesc(member.getPhoneNumber(), otpType)
                .map(MemberOtp::getCreatedAt)
                .ifPresent(createdAt -> {
                    LocalDateTime next = createdAt.plusSeconds(securityPolicy.getOtpResendCooldownSeconds());
                    LocalDateTime now = LocalDateTime.now();
                    if (next.isAfter(now)) {
                        long wait = java.time.Duration.between(now, next).getSeconds() + 1;
                        throw new TooManyRequestsException(ErrorConstant.OTP_COOLDOWN, wait);
                    }
                });
    }

    private boolean isOtpAllowed(Member member, OtpType otpType) {
        if (otpType == null) return false;
        return switch (otpType) {
            case REGISTER -> ActiveStatus.INACTIVE == member.getStatus();
            case FORGOT_PIN, LOGIN -> ActiveStatus.ACTIVE == member.getStatus();
            // Only reachable through the authenticated profile / account flows, never the public OTP endpoints.
            case CHANGE_CONTACT, DELETE_ACCOUNT -> false;
        };
    }

    /** Returns the member's latest FORGOT_PIN OTP, which must be verified and still inside the reset window. */
    private MemberOtp requireVerifiedResetOtp(Member member) {
        if (ActiveStatus.ACTIVE != member.getStatus()) {
            createMemberActivity(member.getPhoneNumber(), MemberActivityEvent.FAILED_RESET_PIN);
            throw new BusinessException(ErrorConstant.MEMBER_NOT_ACTIVE);
        }

        MemberOtp otp = memberOtpRepository
                .findTopByPhoneNumberAndOtpTypeOrderByCreatedAtDesc(member.getPhoneNumber(), OtpType.FORGOT_PIN)
                .filter(o -> o.getVerifiedAt() != null)
                .orElse(null);
        if (otp == null) {
            log.warn("Reset pin rejected, no verified OTP. memberId: {}", member.getId());
            createMemberActivity(member.getPhoneNumber(), MemberActivityEvent.FAILED_RESET_PIN);
            throw new BusinessException(ErrorConstant.OTP_NOT_FOUND);
        }
        if (otp.getExpiredAt().isBefore(LocalDateTime.now())) {
            log.warn("Reset pin rejected, verified OTP expired. memberId: {}", member.getId());
            createMemberActivity(member.getPhoneNumber(), MemberActivityEvent.FAILED_RESET_PIN);
            throw new BusinessException(ErrorConstant.OTP_EXPIRED);
        }
        return otp;
    }

    private String generateOtp(OtpType otpType, String phoneNumber) {
        String otp = OtpUtil.generateOtp();

        MemberOtp memberOtp = new MemberOtp();
        memberOtp.setPhoneNumber(phoneNumber);
        memberOtp.setOtpCode(passwordEncoder.encode(otp));
        memberOtp.setOtpType(otpType);
        memberOtp.setAttempts(0);
        memberOtp.setExpiredAt(OtpUtil.getExpiredTime());
        memberOtp.setCreatedAt(LocalDateTime.now());
        memberOtpRepository.save(memberOtp);

        return otp;
    }

    private void createMemberActivity(String phoneNumber, MemberActivityEvent activityEvent) {
        memberActivityService.createMemberActivity(phoneNumber, activityEvent);
    }

    /** Runs after the current transaction commits (right away when there is none). */
    static void afterCommit(Runnable action) {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    action.run();
                }
            });
        } else {
            action.run();
        }
    }
}
