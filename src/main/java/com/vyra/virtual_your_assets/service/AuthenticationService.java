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
import com.vyra.virtual_your_assets.entity.Member;
import com.vyra.virtual_your_assets.entity.MemberOtp;
import com.vyra.virtual_your_assets.entity.MemberToken;
import com.vyra.virtual_your_assets.exception.BusinessException;
import com.vyra.virtual_your_assets.repository.MemberActivityRepository;
import com.vyra.virtual_your_assets.repository.MemberOtpRepository;
import com.vyra.virtual_your_assets.repository.MemberRepository;
import com.vyra.virtual_your_assets.repository.MemberTokenRepository;
import com.vyra.virtual_your_assets.util.OtpUtil;
import com.vyra.virtual_your_assets.util.TokenUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
    private final MemberActivityRepository memberActivityRepository;

    private final MemberActivityService memberActivityService;
    private final OtpService otpService;
    private final BCryptPasswordEncoder passwordEncoder;

    public BaseResponse<RegisterResponse> register(RegisterRequest request) {
        /* TODO:
            - Send OTP via WhatsApp
         */
        log.info("[START] authenticationService.register request : {} ", request);
        memberActivityService.createMemberActivity(request.getPhoneNumber(), MemberActivityEvent.ATTEMPT_REGISTER);

        if (memberRepository.findByPhoneNumber(request.getPhoneNumber()).isPresent()) {
            throw new BusinessException(ErrorConstant.PHONE_NUMBER_ALREADY_EXIST);
        }

        if (memberRepository.findByEmail(request.getEmail()).isPresent()) {
            throw new BusinessException(ErrorConstant.EMAIL_ALREADY_EXIST);
        }

        Member member = new Member();
        member.setFirstName(request.getFirstName());
        member.setLastName(request.getLastName());
        member.setEmail(request.getEmail());
        member.setPhoneNumber(request.getPhoneNumber());
        member.setPin(passwordEncoder.encode(request.getPin()));
        member.setStatus(MemberStatus.INACTIVE);
        member.setCreatedAt(LocalDateTime.now());
        memberRepository.save(member);

        // Generate OTP
        String otp = OtpUtil.generateOtp();

        MemberOtp memberOtp = new MemberOtp();
        memberOtp.setPhoneNumber(request.getPhoneNumber());
        memberOtp.setOtpCode(passwordEncoder.encode(otp));
        memberOtp.setAttempts(0);
        memberOtp.setOtpType(OtpType.REGISTER);
        memberOtp.setExpiredAt(OtpUtil.getExpiredTime());
        memberOtp.setCreatedAt(LocalDateTime.now());
        memberOtpRepository.save(memberOtp);

        String fullName = request.getFirstName().trim() + request.getLastName().trim();
        memberActivityService.createMemberActivity(request.getPhoneNumber(), MemberActivityEvent.ATTEMPT_GENERATE_OTP_REGISTER);
        otpService.sendOtp(fullName, request.getEmail(), otp);
        memberActivityService.createMemberActivity(request.getPhoneNumber(), MemberActivityEvent.SUCCESS_GENERATE_OTP_REGISTER);

        RegisterResponse response = new RegisterResponse();
        response.setMemberId(UUID.fromString(member.getMemberId()));
        response.setEmail(member.getEmail());
        response.setPhoneNumber(member.getPhoneNumber());

        memberActivityService.createMemberActivity(request.getPhoneNumber(), MemberActivityEvent.SUCCESS_REGISTER);
        log.info("Generate OTP: {}, phoneNumber: {}", otp, request.getPhoneNumber());
        log.info("[END] authenticationService.register successfully response : {} ", response);

        return new BaseResponse<>(
                ErrorConstant.REGISTER_SUCCESS.getCode(),
                ErrorConstant.REGISTER_SUCCESS.getMessage(),
                response
        );
    }

    @Transactional
    public BaseResponse<Void> resendOtp(ResendOtpRequest request) {
        // TODO : Try testing delete
        log.info("[START] otpService.resendOtp request : {} ", request);

        Member member = memberRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new BusinessException(ErrorConstant.MEMBER_NOT_FOUND));

        memberActivityService.createMemberActivity(member.getPhoneNumber(), MemberActivityEvent.ATTEMPT_RESEND_OTP);

        memberOtpRepository.deleteByPhoneNumberAndOtpType(member.getPhoneNumber(), request.getOtpType());

        String otp = OtpUtil.generateOtp();
        MemberOtp memberOtp = MemberOtp.builder()
                .phoneNumber(member.getPhoneNumber())
                .otpCode(passwordEncoder.encode(otp))
                .otpType(request.getOtpType())
                .attempts(0)
                .createdAt(LocalDateTime.now())
                .expiredAt(LocalDateTime.now().plusMinutes(5))
                .build();

        memberOtpRepository.save(memberOtp);

        String fullName = member.getFirstName().trim() + member.getLastName().trim();
        otpService.sendOtp(fullName, request.getEmail(), otp);

        memberActivityService.createMemberActivity(member.getPhoneNumber(), MemberActivityEvent.SUCCESS_RESEND_OTP);

        log.info("[END] otpService.resendOtp successfully request : {} ", request);
        return new BaseResponse<>(
                ErrorConstant.RESEND_OTP.getCode(),
                ErrorConstant.RESEND_OTP.getMessage(),
                null
        );
    }

    @Transactional
    public BaseResponse<Void> verifyOtp(VerifyOtpRequest request) {
        log.info("[START] otpService.verifyOtp request: {} ", request);
        memberActivityService.createMemberActivity(request.getPhoneNumber(), MemberActivityEvent.ATTEMPT_VERIFY_OTP);

        MemberOtp memberOtp = memberOtpRepository.findTopByPhoneNumberAndOtpTypeOrderByCreatedAtDesc(request.getPhoneNumber(), request.getOtpType())
                .orElseThrow(() -> new BusinessException(ErrorConstant.OTP_NOT_FOUND));

        if (memberOtp.getExpiredAt().isBefore(LocalDateTime.now())) {
            throw new BusinessException(ErrorConstant.OTP_EXPIRED);
        }

        if (!passwordEncoder.matches(request.getOtpCode(), memberOtp.getOtpCode())) {
            int currentAttempts = (memberOtp.getAttempts() == null ? 0 : memberOtp.getAttempts()) + 1;
            memberOtp.setAttempts(currentAttempts);

            if (currentAttempts >= 3) {
                memberOtpRepository.deleteByPhoneNumber(request.getPhoneNumber());
                memberRepository.deleteByPhoneNumber(request.getPhoneNumber());
                memberActivityRepository.deleteByPhoneNumber(request.getPhoneNumber());

                log.warn("Max attempts reached for {}. Data deleted.", request.getPhoneNumber());
                throw new BusinessException(ErrorConstant.MAX_ATTEMPTS_REACHED);
            } else {
                memberOtpRepository.save(memberOtp);
                throw new BusinessException(ErrorConstant.OTP_INVALID);
            }
        }

        Member member = memberRepository.findByPhoneNumber(request.getPhoneNumber())
                .orElseThrow(() -> new BusinessException(ErrorConstant.MEMBER_NOT_FOUND));

        member.setStatus(MemberStatus.ACTIVE);
        member.setUpdatedAt(LocalDateTime.now());
        memberRepository.save(member);
        memberOtpRepository.delete(memberOtp);

        return new BaseResponse<>(
                ErrorConstant.VERIFY_OTP_SUCCESS.getCode(),
                ErrorConstant.VERIFY_OTP_SUCCESS.getMessage(),
                null
        );
    }

    public BaseResponse<LoginResponse> login(LoginRequest request) {
        log.info("[START] authenticationService.login phoneNumber : {} ", request.getIdentifier());

        memberActivityService.createMemberActivity(request.getIdentifier(), MemberActivityEvent.ATTEMPT_LOGIN);

        Member member = memberRepository.findByIdentifier(request.getIdentifier())
                .orElseThrow(() -> new BusinessException(ErrorConstant.MEMBER_NOT_FOUND));

        if (member.getStatus() != MemberStatus.ACTIVE) {
            throw new BusinessException(ErrorConstant.MEMBER_NOT_ACTIVE);
        }

        if (!passwordEncoder.matches(request.getPin(), member.getPin())) {
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

        log.info("[END] authenticationService.login successfully response : {} ", response);
        return new BaseResponse<>(
                ErrorConstant.LOGIN_SUCCESS.getCode(),
                ErrorConstant.LOGIN_SUCCESS.getMessage(),
                response
        );
    }
}
