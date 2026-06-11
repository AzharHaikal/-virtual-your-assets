package com.vyra.be_virtual_your_assets.service;

import com.vyra.be_virtual_your_assets.constant.ErrorConstant;
import com.vyra.be_virtual_your_assets.constant.MemberStatus;
import com.vyra.be_virtual_your_assets.constant.OtpType;
import com.vyra.be_virtual_your_assets.dto.auth.VerifyOtpRequest;
import com.vyra.be_virtual_your_assets.dto.member.UpdateProfileRequest;
import com.vyra.be_virtual_your_assets.dto.member.ValidateIsUpdateProfile;
import com.vyra.be_virtual_your_assets.entity.Member;
import com.vyra.be_virtual_your_assets.entity.MemberOtp;
import com.vyra.be_virtual_your_assets.exception.BusinessException;
import com.vyra.be_virtual_your_assets.repository.MemberOtpRepository;
import com.vyra.be_virtual_your_assets.repository.MemberRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ValidationServiceTest {

    @Mock
    private MemberRepository memberRepository;
    @Mock
    private MemberOtpRepository memberOtpRepository;
    @Mock
    private BCryptPasswordEncoder passwordEncoder;

    @InjectMocks
    private ValidationService validationService;

    private Member member;

    @BeforeEach
    void setUp() {
        member = new Member();
        member.setId("member-id-001");
        member.setFirstName("Budi");
        member.setLastName("Santoso");
        member.setEmail("budi@example.com");
        member.setPhoneNumber("628123456789");
        member.setPin("hashed-pin");
        member.setStatus(MemberStatus.ACTIVE);
    }

    // =========================================================================
    // validateDataExistRegister
    // =========================================================================
    @Test
    void validateDataExistRegister_phoneExists_shouldThrowPhoneAlreadyExist() {
        when(memberRepository.findByPhoneNumber("628123456789")).thenReturn(Optional.of(member));

        BusinessException ex = assertThrows(BusinessException.class,
                () -> validationService.validateDataExistRegister("628123456789", "new@example.com"));

        assertThat(ex.getErrorConstant()).isEqualTo(ErrorConstant.PHONE_NUMBER_ALREADY_EXIST);
        verify(memberRepository, never()).findByEmailIgnoreCase(any());
    }

    @Test
    void validateDataExistRegister_emailExists_shouldThrowEmailAlreadyExist() {
        when(memberRepository.findByPhoneNumber("628999999999")).thenReturn(Optional.empty());
        when(memberRepository.findByEmailIgnoreCase("budi@example.com")).thenReturn(Optional.of(member));

        BusinessException ex = assertThrows(BusinessException.class,
                () -> validationService.validateDataExistRegister("628999999999", "budi@example.com"));

        assertThat(ex.getErrorConstant()).isEqualTo(ErrorConstant.EMAIL_ALREADY_EXIST);
    }

    @Test
    void validateDataExistRegister_neitherExists_shouldNotThrow() {
        when(memberRepository.findByPhoneNumber("628999999999")).thenReturn(Optional.empty());
        when(memberRepository.findByEmailIgnoreCase("new@example.com")).thenReturn(Optional.empty());

        validationService.validateDataExistRegister("628999999999", "new@example.com");
        // no exception = pass
    }

    // =========================================================================
    // getMemberById
    // =========================================================================
    @Test
    void getMemberById_memberExists_shouldReturnMember() {
        when(memberRepository.findById("member-id-001")).thenReturn(Optional.of(member));

        Member result = validationService.getMemberById("member-id-001");

        assertThat(result).isSameAs(member);
    }

    @Test
    void getMemberById_memberNotFound_shouldThrowMemberNotFound() {
        when(memberRepository.findById("unknown-id")).thenReturn(Optional.empty());

        BusinessException ex = assertThrows(BusinessException.class,
                () -> validationService.getMemberById("unknown-id"));

        assertThat(ex.getErrorConstant()).isEqualTo(ErrorConstant.MEMBER_NOT_FOUND);
    }

    // =========================================================================
    // getMemberByEmailOrPhoneNumber
    // =========================================================================
    @Test
    void getMemberByEmailOrPhoneNumber_emailNotBlank_shouldLookupByEmail() {
        when(memberRepository.findByEmailIgnoreCase("budi@example.com")).thenReturn(Optional.of(member));

        Member result = validationService.getMemberByEmailOrPhoneNumber("budi@example.com", null);

        assertThat(result).isSameAs(member);
        verify(memberRepository).findByEmailIgnoreCase("budi@example.com");
        verify(memberRepository, never()).findByPhoneNumber(any());
    }

    @Test
    void getMemberByEmailOrPhoneNumber_emailBlank_shouldLookupByPhone() {
        when(memberRepository.findByPhoneNumber("628123456789")).thenReturn(Optional.of(member));

        Member result = validationService.getMemberByEmailOrPhoneNumber("", "628123456789");

        assertThat(result).isSameAs(member);
        verify(memberRepository).findByPhoneNumber("628123456789");
        verify(memberRepository, never()).findByEmailIgnoreCase(any());
    }

    @Test
    void getMemberByEmailOrPhoneNumber_emailNull_shouldLookupByPhone() {
        when(memberRepository.findByPhoneNumber("628123456789")).thenReturn(Optional.of(member));

        Member result = validationService.getMemberByEmailOrPhoneNumber(null, "628123456789");

        assertThat(result).isSameAs(member);
        verify(memberRepository).findByPhoneNumber("628123456789");
    }

    // =========================================================================
    // getMemberByPhoneNumber
    // =========================================================================
    @Test
    void getMemberByPhoneNumber_found_shouldReturnMember() {
        when(memberRepository.findByPhoneNumber("628123456789")).thenReturn(Optional.of(member));

        Member result = validationService.getMemberByPhoneNumber("628123456789");

        assertThat(result).isSameAs(member);
    }

    @Test
    void getMemberByPhoneNumber_notFound_shouldThrowMemberNotFound() {
        when(memberRepository.findByPhoneNumber("628000000000")).thenReturn(Optional.empty());

        BusinessException ex = assertThrows(BusinessException.class,
                () -> validationService.getMemberByPhoneNumber("628000000000"));

        assertThat(ex.getErrorConstant()).isEqualTo(ErrorConstant.MEMBER_NOT_FOUND);
    }

    // =========================================================================
    // getMemberByEmailIgnoreCase
    // =========================================================================
    @Test
    void getMemberByEmailIgnoreCase_found_shouldReturnMember() {
        when(memberRepository.findByEmailIgnoreCase("budi@example.com")).thenReturn(Optional.of(member));

        Member result = validationService.getMemberByEmailIgnoreCase("budi@example.com");

        assertThat(result).isSameAs(member);
    }

    @Test
    void getMemberByEmailIgnoreCase_notFound_shouldThrowMemberNotFound() {
        when(memberRepository.findByEmailIgnoreCase("unknown@example.com")).thenReturn(Optional.empty());

        BusinessException ex = assertThrows(BusinessException.class,
                () -> validationService.getMemberByEmailIgnoreCase("unknown@example.com"));

        assertThat(ex.getErrorConstant()).isEqualTo(ErrorConstant.MEMBER_NOT_FOUND);
    }

    // =========================================================================
    // verifyOtp — OTP not found
    // =========================================================================
    @Test
    void verifyOtp_otpNotFound_shouldThrowOtpNotFound() {
        VerifyOtpRequest request = buildVerifyRequest("999999", OtpType.REGISTER);
        when(memberOtpRepository.findTopByPhoneNumberAndOtpTypeOrderByCreatedAtDesc(
                member.getPhoneNumber(), OtpType.REGISTER))
                .thenReturn(Optional.empty());

        BusinessException ex = assertThrows(BusinessException.class,
                () -> validationService.verifyOtp(member, request));

        assertThat(ex.getErrorConstant()).isEqualTo(ErrorConstant.OTP_NOT_FOUND);
    }

    // =========================================================================
    // verifyOtp — OTP expired
    // =========================================================================
    @Test
    void verifyOtp_otpExpired_shouldThrowOtpExpired() {
        VerifyOtpRequest request = buildVerifyRequest("123456", OtpType.REGISTER);
        MemberOtp otp = buildOtp("hashed", OtpType.REGISTER, LocalDateTime.now().minusMinutes(1), 0);
        when(memberOtpRepository.findTopByPhoneNumberAndOtpTypeOrderByCreatedAtDesc(
                member.getPhoneNumber(), OtpType.REGISTER))
                .thenReturn(Optional.of(otp));

        BusinessException ex = assertThrows(BusinessException.class,
                () -> validationService.verifyOtp(member, request));

        assertThat(ex.getErrorConstant()).isEqualTo(ErrorConstant.OTP_EXPIRED);
    }

    // =========================================================================
    // verifyOtp — wrong OTP, attempts < 4
    // =========================================================================
    @Test
    void verifyOtp_wrongOtp_attemptsLessThan4_shouldIncrementAndThrowOtpInvalid() {
        VerifyOtpRequest request = buildVerifyRequest("wrong", OtpType.REGISTER);
        MemberOtp otp = buildOtp("hashed", OtpType.REGISTER, LocalDateTime.now().plusMinutes(5), 1);
        when(memberOtpRepository.findTopByPhoneNumberAndOtpTypeOrderByCreatedAtDesc(
                member.getPhoneNumber(), OtpType.REGISTER))
                .thenReturn(Optional.of(otp));
        when(passwordEncoder.matches("wrong", "hashed")).thenReturn(false);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> validationService.verifyOtp(member, request));

        assertThat(ex.getErrorConstant()).isEqualTo(ErrorConstant.OTP_INVALID);
        assertThat(ex.getMessage()).contains("2"); // remaining = 4 - 2
        verify(memberOtpRepository).save(otp);
        assertThat(otp.getAttempts()).isEqualTo(2);
    }

    @Test
    void verifyOtp_wrongOtp_attemptsNullTreatedAsZero_shouldIncrementAndThrowOtpInvalid() {
        VerifyOtpRequest request = buildVerifyRequest("wrong", OtpType.REGISTER);
        MemberOtp otp = buildOtp("hashed", OtpType.REGISTER, LocalDateTime.now().plusMinutes(5), null);
        when(memberOtpRepository.findTopByPhoneNumberAndOtpTypeOrderByCreatedAtDesc(
                member.getPhoneNumber(), OtpType.REGISTER))
                .thenReturn(Optional.of(otp));
        when(passwordEncoder.matches("wrong", "hashed")).thenReturn(false);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> validationService.verifyOtp(member, request));

        assertThat(ex.getErrorConstant()).isEqualTo(ErrorConstant.OTP_INVALID);
        assertThat(otp.getAttempts()).isEqualTo(1);
    }

    // =========================================================================
    // verifyOtp — wrong OTP, attempts reach 4 → suspend
    // =========================================================================
    @Test
    void verifyOtp_wrongOtp_attemptsReach4_shouldSuspendMemberAndThrowMaxAttempts() {
        VerifyOtpRequest request = buildVerifyRequest("wrong", OtpType.REGISTER);
        MemberOtp otp = buildOtp("hashed", OtpType.REGISTER, LocalDateTime.now().plusMinutes(5), 3);
        when(memberOtpRepository.findTopByPhoneNumberAndOtpTypeOrderByCreatedAtDesc(
                member.getPhoneNumber(), OtpType.REGISTER))
                .thenReturn(Optional.of(otp));
        when(passwordEncoder.matches("wrong", "hashed")).thenReturn(false);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> validationService.verifyOtp(member, request));

        assertThat(ex.getErrorConstant()).isEqualTo(ErrorConstant.MAX_ATTEMPTS_REACHED);
        assertThat(member.getStatus()).isEqualTo(MemberStatus.SUSPENDED);
        verify(memberRepository).save(member);
        verify(memberOtpRepository).deleteByPhoneNumber(member.getPhoneNumber());
    }

    // =========================================================================
    // verifyOtp — correct OTP
    // =========================================================================
    @Test
    void verifyOtp_correctOtp_shouldReturnMemberOtp() {
        VerifyOtpRequest request = buildVerifyRequest("123456", OtpType.REGISTER);
        MemberOtp otp = buildOtp("hashed", OtpType.REGISTER, LocalDateTime.now().plusMinutes(5), 0);
        when(memberOtpRepository.findTopByPhoneNumberAndOtpTypeOrderByCreatedAtDesc(
                member.getPhoneNumber(), OtpType.REGISTER))
                .thenReturn(Optional.of(otp));
        when(passwordEncoder.matches("123456", "hashed")).thenReturn(true);

        MemberOtp result = validationService.verifyOtp(member, request);

        assertThat(result).isSameAs(otp);
        verify(memberOtpRepository, never()).save(any());
        verify(memberRepository, never()).save(any());
    }

    // =========================================================================
    // validateIsUpdateProfile — no changes
    // =========================================================================
    @Test
    void validateIsUpdateProfile_allFieldsBlank_shouldReturnNoChanges() {
        UpdateProfileRequest request = new UpdateProfileRequest();
        // all fields null/blank

        ValidateIsUpdateProfile result = validationService.validateIsUpdateProfile(member, request);

        assertThat(result.isEmailChanged()).isFalse();
        assertThat(result.isPhoneChanged()).isFalse();
    }

    // =========================================================================
    // validateIsUpdateProfile — firstName & lastName change only
    // =========================================================================
    @Test
    void validateIsUpdateProfile_firstNameLastNameChanged_shouldUpdateMemberButNoEmailPhoneChange() {
        UpdateProfileRequest request = new UpdateProfileRequest();
        request.setFirstName("  Andi  ");
        request.setLastName("  Wijaya  ");

        ValidateIsUpdateProfile result = validationService.validateIsUpdateProfile(member, request);

        assertThat(member.getFirstName()).isEqualTo("Andi");
        assertThat(member.getLastName()).isEqualTo("Wijaya");
        assertThat(result.isEmailChanged()).isFalse();
        assertThat(result.isPhoneChanged()).isFalse();
    }

    // =========================================================================
    // validateIsUpdateProfile — email changed, same member (no conflict)
    // =========================================================================
    @Test
    void validateIsUpdateProfile_emailChangedSameMember_shouldSetEmailChangedTrue() {
        UpdateProfileRequest request = new UpdateProfileRequest();
        request.setEmail("new@example.com");
        when(memberRepository.findByEmailIgnoreCase("new@example.com")).thenReturn(Optional.of(member)); // same member

        ValidateIsUpdateProfile result = validationService.validateIsUpdateProfile(member, request);

        assertThat(result.isEmailChanged()).isTrue();
        assertThat(member.getEmail()).isEqualTo("new@example.com");
    }

    @Test
    void validateIsUpdateProfile_emailChangedNotFound_shouldSetEmailChangedTrue() {
        UpdateProfileRequest request = new UpdateProfileRequest();
        request.setEmail("other@example.com");
        when(memberRepository.findByEmailIgnoreCase("other@example.com")).thenReturn(Optional.empty());

        ValidateIsUpdateProfile result = validationService.validateIsUpdateProfile(member, request);

        assertThat(result.isEmailChanged()).isTrue();
        assertThat(member.getEmail()).isEqualTo("other@example.com");
    }

    // =========================================================================
    // validateIsUpdateProfile — email belongs to different member → exception
    // =========================================================================
    @Test
    void validateIsUpdateProfile_emailAlreadyUsedByAnotherMember_shouldThrowEmailAlreadyExistV2() {
        Member otherMember = new Member();
        otherMember.setId("other-member-id");
        UpdateProfileRequest request = new UpdateProfileRequest();
        request.setEmail("taken@example.com");
        when(memberRepository.findByEmailIgnoreCase("taken@example.com")).thenReturn(Optional.of(otherMember));

        BusinessException ex = assertThrows(BusinessException.class,
                () -> validationService.validateIsUpdateProfile(member, request));

        assertThat(ex.getErrorConstant()).isEqualTo(ErrorConstant.EMAIL_ALREADY_EXIST_V2);
    }

    // =========================================================================
    // validateIsUpdateProfile — phone changed, same member (no conflict)
    // =========================================================================
    @Test
    void validateIsUpdateProfile_phoneChangedSameMember_shouldSetPhoneChangedTrue() {
        UpdateProfileRequest request = new UpdateProfileRequest();
        request.setPhoneNumber("628999999999");
        when(memberRepository.findByPhoneNumber("628999999999")).thenReturn(Optional.of(member)); // same id

        ValidateIsUpdateProfile result = validationService.validateIsUpdateProfile(member, request);

        assertThat(result.isPhoneChanged()).isTrue();
        assertThat(member.getPhoneNumber()).isEqualTo("628999999999");
    }

    @Test
    void validateIsUpdateProfile_phoneChangedNotFound_shouldSetPhoneChangedTrue() {
        UpdateProfileRequest request = new UpdateProfileRequest();
        request.setPhoneNumber("628888888888");
        when(memberRepository.findByPhoneNumber("628888888888")).thenReturn(Optional.empty());

        ValidateIsUpdateProfile result = validationService.validateIsUpdateProfile(member, request);

        assertThat(result.isPhoneChanged()).isTrue();
    }

    // =========================================================================
    // validateIsUpdateProfile — phone belongs to different member → exception
    // =========================================================================
    @Test
    void validateIsUpdateProfile_phoneAlreadyUsedByAnotherMember_shouldThrowPhoneAlreadyExistV2() {
        Member otherMember = new Member();
        otherMember.setId("other-member-id");
        UpdateProfileRequest request = new UpdateProfileRequest();
        request.setPhoneNumber("628777777777");
        when(memberRepository.findByPhoneNumber("628777777777")).thenReturn(Optional.of(otherMember));

        BusinessException ex = assertThrows(BusinessException.class,
                () -> validationService.validateIsUpdateProfile(member, request));

        assertThat(ex.getErrorConstant()).isEqualTo(ErrorConstant.PHONE_NUMBER_ALREADY_EXIST_V2);
    }

    // =========================================================================
    // helpers
    // =========================================================================
    private VerifyOtpRequest buildVerifyRequest(String otpCode, OtpType otpType) {
        VerifyOtpRequest req = new VerifyOtpRequest();
        req.setEmail(member.getEmail());
        req.setOtpCode(otpCode);
        req.setOtpType(otpType);
        return req;
    }

    private MemberOtp buildOtp(String hashedCode, OtpType otpType, LocalDateTime expiredAt, Integer attempts) {
        MemberOtp otp = new MemberOtp();
        otp.setPhoneNumber(member.getPhoneNumber());
        otp.setOtpCode(hashedCode);
        otp.setOtpType(otpType);
        otp.setExpiredAt(expiredAt);
        otp.setAttempts(attempts);
        return otp;
    }
}
