package com.vyra.virtual_your_assets.service;

import com.vyra.virtual_your_assets.constant.ErrorConstant;
import com.vyra.virtual_your_assets.constant.MemberActivityEvent;
import com.vyra.virtual_your_assets.constant.MemberStatus;
import com.vyra.virtual_your_assets.constant.OtpType;
import com.vyra.virtual_your_assets.dto.BaseResponse;
import com.vyra.virtual_your_assets.dto.login.LoginRequest;
import com.vyra.virtual_your_assets.dto.login.LoginResponse;
import com.vyra.virtual_your_assets.dto.register.RegisterRequest;
import com.vyra.virtual_your_assets.dto.register.RegisterResponse;
import com.vyra.virtual_your_assets.dto.register.ResendOtpRequest;
import com.vyra.virtual_your_assets.dto.register.VerifyOtpRequest;
import com.vyra.virtual_your_assets.dto.wallet.CreateWalletRequest;
import com.vyra.virtual_your_assets.dto.wallet.CreateWalletResponse;
import com.vyra.virtual_your_assets.entity.Member;
import com.vyra.virtual_your_assets.entity.MemberOtp;
import com.vyra.virtual_your_assets.entity.MemberToken;
import com.vyra.virtual_your_assets.exception.BusinessException;
import com.vyra.virtual_your_assets.repository.MemberOtpRepository;
import com.vyra.virtual_your_assets.repository.MemberRepository;
import com.vyra.virtual_your_assets.repository.MemberTokenRepository;
import com.vyra.virtual_your_assets.util.OtpUtil;
import com.vyra.virtual_your_assets.util.TokenUtil;
import io.micrometer.tracing.annotation.NewSpan;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.logging.log4j.util.InternalException;
import org.springframework.beans.BeanUtils;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthenticationService {
    private final MemberRepository memberRepository;
    private final MemberOtpRepository memberOtpRepository;
    private final MemberTokenRepository memberTokenRepository;
    private final MemberActivityService memberActivityService;
    private final OtpService otpService;
    private final ValidationService validationService;
    private final WalletService walletService;
    private final BCryptPasswordEncoder passwordEncoder;

    @Transactional
    @NewSpan
    public BaseResponse<RegisterResponse> registerMember(RegisterRequest request) {
        log.info("[START] authenticationService.registerMember. phoneNumber: {} ", request.getPhoneNumber());
        memberActivityService.createMemberActivity(request.getPhoneNumber(), MemberActivityEvent.ATTEMPT_REGISTER);

        log.info("Validate member already exist. phoneNumber: {} ", request.getPhoneNumber());
        validationService.validateDataExistRegister(request.getPhoneNumber(), request.getEmail());

        Member member = new Member();
        BeanUtils.copyProperties(request, member);
        member.setPin(passwordEncoder.encode(request.getPin()));
        member.setStatus(MemberStatus.INACTIVE);
        member.setCreatedAt(LocalDateTime.now());
        memberRepository.save(member);
        log.info("Save data member. phoneNumber: {} ", request.getPhoneNumber());

        String otp = OtpUtil.generateOtp();

        MemberOtp memberOtp = new MemberOtp();
        memberOtp.setPhoneNumber(request.getPhoneNumber());
        memberOtp.setOtpCode(passwordEncoder.encode(otp));
        memberOtp.setAttempts(0);
        memberOtp.setOtpType(OtpType.REGISTER);
        memberOtp.setExpiredAt(OtpUtil.getExpiredTime());
        memberOtp.setCreatedAt(LocalDateTime.now());
        memberOtpRepository.save(memberOtp);

        String fullName = request.getFirstName().trim() + " " + request.getLastName().trim();

        // Create wallet and send OTP
        try {
            log.info("Create member wallet. phoneNumber: {} ", request.getPhoneNumber());
            CreateWalletRequest createWalletRequest = new CreateWalletRequest();
            createWalletRequest.setPhoneNumber(request.getPhoneNumber());

            BaseResponse<CreateWalletResponse> createWalletResponse = walletService.createMemberWallet(createWalletRequest);
            if (!ErrorConstant.CREATE_WALLET_SUCCESS.getCode().equals(createWalletResponse.getResponseStatus())) {
                log.info("Failed when creating member wallet. phoneNumber: {} ", request.getPhoneNumber());
                throw new BusinessException(ErrorConstant.CREATE_WALLET_FAILED);
            }

            log.info("Attempt send otp to email. phoneNumber: {}, otp: {} ", request.getPhoneNumber(), otp);
            memberActivityService.createMemberActivity(request.getPhoneNumber(), MemberActivityEvent.ATTEMPT_GENERATE_OTP_REGISTER);

            // Send OTP async
            otpService.sendOtp(fullName, request.getEmail(), otp);

            memberActivityService.createMemberActivity(request.getPhoneNumber(), MemberActivityEvent.SUCCESS_GENERATE_OTP_REGISTER);
            log.info("Success send otp to email. phoneNumber: {}, otp: {} ", request.getPhoneNumber(), otp);

        } catch (BusinessException e) {
            log.error("[ERROR] register {} encountered an exception: {}", request.getPhoneNumber(), e.getMessage(), e);
            memberActivityService.createMemberActivity(request.getPhoneNumber(), MemberActivityEvent.FAILED_REGISTER);
            throw e;
        } catch (Exception e) {
            log.error("[ERROR] Unexpected error during register {}, message: {}", request.getPhoneNumber(), e.getMessage(), e);
            memberActivityService.createMemberActivity(request.getPhoneNumber(), MemberActivityEvent.FAILED_REGISTER);
            throw new InternalException(ErrorConstant.INTERNAL_SERVER_ERROR.getMessage());
        }

        RegisterResponse response = new RegisterResponse();
        response.setMemberId(member.getMemberId());
        response.setFullName(fullName);
        response.setEmail(member.getEmail());
        response.setPhoneNumber(member.getPhoneNumber());

        memberActivityService.createMemberActivity(request.getPhoneNumber(), MemberActivityEvent.SUCCESS_REGISTER);
        log.info("[END] authenticationService.registerMember successfully. phoneNumber: {} ", request.getPhoneNumber());

        return new BaseResponse<>(
                ErrorConstant.REGISTER_SUCCESS.getCode(),
                ErrorConstant.REGISTER_SUCCESS.getMessage(),
                response
        );
    }

    @Transactional
    @NewSpan
    public BaseResponse<Void> resendOtp(ResendOtpRequest request) {
        // TODO : Try testing delete
        log.info("[START] authenticationService.resendOtp email: {} ", request.getEmail());
        Member member = validationService.getEmailIgnoreCase(request.getEmail());

        log.info("Attempt resend otp to email. phoneNumber: {}", member.getPhoneNumber());
        memberActivityService.createMemberActivity(member.getPhoneNumber(), MemberActivityEvent.ATTEMPT_RESEND_OTP);

        memberOtpRepository.deleteByPhoneNumberAndOtpType(member.getPhoneNumber(), request.getOtpType());

        String otp = OtpUtil.generateOtp();

        MemberOtp memberOtp = new MemberOtp();
        memberOtp.setPhoneNumber(member.getPhoneNumber());
        memberOtp.setOtpCode(passwordEncoder.encode(otp));
        memberOtp.setOtpType(request.getOtpType());
        memberOtp.setAttempts(0);
        memberOtp.setCreatedAt(LocalDateTime.now());
        memberOtp.setExpiredAt(LocalDateTime.now().plusMinutes(5));
        memberOtpRepository.save(memberOtp);

        // Send OTP
        try {
            log.info("Attempt resend otp to email. phoneNumber: {}, otp: {} ", member.getPhoneNumber(), otp);
            otpService.sendOtp(member.getFirstName().trim() + " " + member.getLastName().trim(), request.getEmail(), otp);
        } catch (Exception e) {
            log.error("[ERROR] resendOtp {} encountered an exception: {}", member.getPhoneNumber(), e.getMessage(), e);
            throw e;
        }

        memberActivityService.createMemberActivity(member.getPhoneNumber(), MemberActivityEvent.SUCCESS_RESEND_OTP);
        log.info("Success resend otp to email. phoneNumber: {}", member.getPhoneNumber());

        log.info("[END] authenticationService.resendOtp successfully. phoneNumber: {}", member.getPhoneNumber());
        return new BaseResponse<>(
                ErrorConstant.RESEND_OTP.getCode(),
                ErrorConstant.RESEND_OTP.getMessage(),
                null
        );
    }

    @Transactional(noRollbackFor = BusinessException.class)
    @NewSpan
    public BaseResponse<Void> verifyOtp(VerifyOtpRequest request) {
        log.info("[START] authenticationService.verifyOtp email: {} ", request.getEmail());
        Member member = validationService.getEmailIgnoreCase(request.getEmail());

        log.info("Attempt verify otp. phoneNumber: {}", member.getPhoneNumber());
        memberActivityService.createMemberActivity(member.getPhoneNumber(), MemberActivityEvent.ATTEMPT_VERIFY_OTP);

        MemberOtp memberOtp = validationService.verifyOtp(member, request);
        member.setStatus(MemberStatus.ACTIVE);
        member.setUpdatedAt(LocalDateTime.now());
        memberRepository.save(member);
        memberOtpRepository.delete(memberOtp);

        memberActivityService.createMemberActivity(member.getPhoneNumber(), MemberActivityEvent.SUCCESS_VERIFY_OTP);
        log.info("[START] authenticationService.verifyOtp successfully. phoneNumber: {}", member.getPhoneNumber());

        return new BaseResponse<>(
                ErrorConstant.VERIFY_OTP_SUCCESS.getCode(),
                ErrorConstant.VERIFY_OTP_SUCCESS.getMessage(),
                null
        );
    }

    @NewSpan
    public BaseResponse<LoginResponse> login(LoginRequest request) {
        log.info("[START] authenticationService.login phoneNumber: {} ", request.getIdentifier());
        memberActivityService.createMemberActivity(request.getIdentifier(), MemberActivityEvent.ATTEMPT_LOGIN);

        Member member = validationService.getMemberByEmailOrPhoneNumber(request.getIdentifier());

        if (MemberStatus.ACTIVE != member.getStatus()) {
            log.info("login failed member not active. phoneNumber: {} ", member.getPhoneNumber());
            throw new BusinessException(ErrorConstant.MEMBER_NOT_ACTIVE);
        }

        if (!passwordEncoder.matches(request.getPin(), member.getPin())) {
            log.info("login failed invalid pin. phoneNumber: {} ", member.getPhoneNumber());
            throw new BusinessException(ErrorConstant.INVALID_PIN);
        }

        String token = TokenUtil.generateToken();

        MemberToken memberToken = new MemberToken();
        memberToken.setMemberId(member.getMemberId());
        memberToken.setAccessToken(token);
        memberToken.setExpiredAt(TokenUtil.expiredAt());
        memberToken.setCreatedAt(LocalDateTime.now());

        memberTokenRepository.save(memberToken);

        LoginResponse response = new LoginResponse();
        response.setPhoneNumber(member.getPhoneNumber());
        response.setEmail(member.getEmail());
        response.setToken(token);

        memberActivityService.createMemberActivity(request.getIdentifier(), MemberActivityEvent.SUCCESS_LOGIN);

        log.info("[END] authenticationService.login successfully response: {} ", response);
        return new BaseResponse<>(
                ErrorConstant.LOGIN_SUCCESS.getCode(),
                ErrorConstant.LOGIN_SUCCESS.getMessage(),
                response
        );
    }
}
