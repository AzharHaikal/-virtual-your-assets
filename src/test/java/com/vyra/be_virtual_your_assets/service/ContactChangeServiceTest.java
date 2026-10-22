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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ContactChangeServiceTest {

    @Mock private MemberRepository memberRepository;
    @Mock private MemberOtpRepository memberOtpRepository;
    @Mock private MemberContactChangeRepository contactChangeRepository;
    @Mock private MemberTokenRepository memberTokenRepository;
    @Mock private ValidationService validationService;
    @Mock private LockoutService lockoutService;
    @Mock private OtpService otpService;
    @Mock private WalletService walletService;
    @Mock private TransactionService transactionService;
    @Mock private MemberActivityService memberActivityService;
    @Mock private BCryptPasswordEncoder passwordEncoder;
    @Spy private SecurityPolicy securityPolicy = new SecurityPolicy();

    @InjectMocks
    private ContactChangeService service;

    private Member member;

    @BeforeEach
    void setUp() {
        member = new Member();
        member.setId("member-1");
        member.setFirstName("Budi");
        member.setLastName("Santoso");
        member.setEmail("budi@example.com");
        member.setPhoneNumber("628123456789");
        member.setStatus(ActiveStatus.ACTIVE);
    }

    private MemberContactChange pending(String email, String phone) {
        MemberContactChange change = new MemberContactChange();
        change.setMemberId("member-1");
        change.setNewEmail(email);
        change.setNewPhoneNumber(phone);
        return change;
    }

    // ---- stage -------------------------------------------------------------
    @Test
    void stage_newEmail_shouldRecordChangeAndSendOtpToTheNewAddressOnly() {
        when(contactChangeRepository.findByMemberId("member-1")).thenReturn(Optional.empty());
        when(passwordEncoder.encode(anyString())).thenReturn("hashed-otp");

        String sentTo = service.stage(member, "new@example.com", null);

        assertThat(sentTo).isEqualTo("new@example.com");
        ArgumentCaptor<MemberContactChange> saved = ArgumentCaptor.forClass(MemberContactChange.class);
        verify(contactChangeRepository).save(saved.capture());
        assertThat(saved.getValue().getNewEmail()).isEqualTo("new@example.com");
        assertThat(saved.getValue().getNewPhoneNumber()).isNull();
        verify(otpService).sendOtp(eq("Budi Santoso"), eq("new@example.com"), eq("628123456789"), anyString());
        ArgumentCaptor<MemberOtp> otp = ArgumentCaptor.forClass(MemberOtp.class);
        verify(memberOtpRepository).save(otp.capture());
        assertThat(otp.getValue().getOtpType()).isEqualTo(OtpType.CHANGE_CONTACT);
        assertThat(otp.getValue().getOtpCode()).isEqualTo("hashed-otp");
        // nothing about the member itself changed yet
        assertThat(member.getEmail()).isEqualTo("budi@example.com");
        verify(memberRepository, never()).save(any());
    }

    @Test
    void stage_phoneOnly_shouldSendOtpToTheCurrentEmail() {
        when(contactChangeRepository.findByMemberId("member-1")).thenReturn(Optional.empty());
        when(passwordEncoder.encode(anyString())).thenReturn("hashed-otp");

        String sentTo = service.stage(member, null, "628999999999");

        assertThat(sentTo).isEqualTo("budi@example.com");
        verify(otpService).sendOtp(anyString(), eq("budi@example.com"), eq("628123456789"), anyString());
    }

    @Test
    void stage_replacesAnEarlierPendingRequest() {
        MemberContactChange earlier = pending("old-request@example.com", null);
        when(contactChangeRepository.findByMemberId("member-1")).thenReturn(Optional.of(earlier));
        when(passwordEncoder.encode(anyString())).thenReturn("hashed-otp");

        service.stage(member, "second@example.com", null);

        assertThat(earlier.getNewEmail()).isEqualTo("second@example.com");
        verify(contactChangeRepository).save(earlier);
        verify(memberOtpRepository).deleteByPhoneNumberAndOtpType("628123456789", OtpType.CHANGE_CONTACT);
    }

    @Test
    void stage_addressBelongsToAnotherMember_shouldRejectAndSendNothing() {
        doThrow(new BusinessException(ErrorConstant.EMAIL_ALREADY_EXIST_V2))
                .when(validationService).assertContactAvailable(member, "taken@example.com", null);

        assertThrows(BusinessException.class, () -> service.stage(member, "taken@example.com", null));

        verify(otpService, never()).sendOtp(anyString(), anyString(), anyString(), anyString());
        verify(contactChangeRepository, never()).save(any());
    }

    @Test
    void stage_withinCooldown_shouldBeRefused() {
        MemberOtp recent = new MemberOtp();
        recent.setCreatedAt(LocalDateTime.now().minusSeconds(5));
        when(memberOtpRepository.findTopByPhoneNumberAndOtpTypeOrderByCreatedAtDesc("628123456789", OtpType.CHANGE_CONTACT))
                .thenReturn(Optional.of(recent));

        TooManyRequestsException ex = assertThrows(TooManyRequestsException.class,
                () -> service.stage(member, "new@example.com", null));

        assertThat(ex.getErrorConstant()).isEqualTo(ErrorConstant.OTP_COOLDOWN);
        verify(otpService, never()).sendOtp(anyString(), anyString(), anyString(), anyString());
    }

    @Test
    void stage_whileOtpLocked_shouldBeRefused() {
        doThrow(new TooManyRequestsException(ErrorConstant.OTP_LOCKED, 300)).when(lockoutService).assertOtpNotLocked(member);

        assertThrows(TooManyRequestsException.class, () -> service.stage(member, "new@example.com", null));

        verify(contactChangeRepository, never()).save(any());
    }

    // ---- verify ------------------------------------------------------------
    @Test
    void verify_correctOtp_shouldApplyEmailAndPhoneSyncRecordsAndEndOtherSessions() {
        MemberContactChange change = pending("new@example.com", "628999999999");
        MemberOtp otp = new MemberOtp();
        when(validationService.getMemberById("member-1")).thenReturn(member);
        when(contactChangeRepository.findByMemberId("member-1")).thenReturn(Optional.of(change));
        when(validationService.verifyOtp(any(Member.class), any(VerifyOtpRequest.class))).thenReturn(otp);

        UpdateProfileResponse response = service.verify("member-1", "123456", "current-token");

        assertThat(response.getEmail()).isEqualTo("new@example.com");
        assertThat(response.getPhoneNumber()).isEqualTo("628999999999");
        assertThat(member.getEmail()).isEqualTo("new@example.com");
        assertThat(member.getPhoneNumber()).isEqualTo("628999999999");

        ArgumentCaptor<WalletUpdateRequest> wallet = ArgumentCaptor.forClass(WalletUpdateRequest.class);
        verify(walletService).updateMemberWallet(wallet.capture());
        assertThat(wallet.getValue().getOldPhoneNumber()).isEqualTo("628123456789");
        assertThat(wallet.getValue().getNewPhoneNumber()).isEqualTo("628999999999");
        ArgumentCaptor<TransactionUpdateRequest> tx = ArgumentCaptor.forClass(TransactionUpdateRequest.class);
        verify(transactionService).updateTransaction(tx.capture());
        assertThat(tx.getValue().getEmail()).isEqualTo("new@example.com");

        verify(memberRepository).save(member);
        verify(memberOtpRepository).delete(otp);
        verify(contactChangeRepository).delete(change);
        verify(memberTokenRepository).deleteByUserIdAndAccessTokenNot("member-1", "current-token");
        verify(memberActivityService).createMemberActivity("628999999999", MemberActivityEvent.SUCCESS_UPDATE_PROFILE);
    }

    @Test
    void verify_emailOnly_shouldNotTouchTheWalletPhone() {
        MemberContactChange change = pending("new@example.com", null);
        when(validationService.getMemberById("member-1")).thenReturn(member);
        when(contactChangeRepository.findByMemberId("member-1")).thenReturn(Optional.of(change));
        when(validationService.verifyOtp(any(Member.class), any(VerifyOtpRequest.class))).thenReturn(new MemberOtp());

        UpdateProfileResponse response = service.verify("member-1", "123456", "current-token");

        assertThat(response.getEmail()).isEqualTo("new@example.com");
        assertThat(response.getPhoneNumber()).isEqualTo("628123456789");
        verify(walletService, never()).updateMemberWallet(any());
    }

    @Test
    void verify_wrongOtp_shouldChangeNothing() {
        MemberContactChange change = pending("new@example.com", null);
        when(validationService.getMemberById("member-1")).thenReturn(member);
        when(contactChangeRepository.findByMemberId("member-1")).thenReturn(Optional.of(change));
        when(validationService.verifyOtp(any(Member.class), any(VerifyOtpRequest.class)))
                .thenThrow(new BusinessException(ErrorConstant.OTP_INVALID, "3"));

        assertThrows(BusinessException.class, () -> service.verify("member-1", "000000", "current-token"));

        assertThat(member.getEmail()).isEqualTo("budi@example.com");
        verify(memberRepository, never()).save(any());
        verify(memberTokenRepository, never()).deleteByUserIdAndAccessTokenNot(anyString(), anyString());
        verify(contactChangeRepository, never()).delete(any());
    }

    @Test
    void verify_withoutAPendingChange_shouldBeRejected() {
        when(validationService.getMemberById("member-1")).thenReturn(member);
        when(contactChangeRepository.findByMemberId("member-1")).thenReturn(Optional.empty());

        BusinessException ex = assertThrows(BusinessException.class, () -> service.verify("member-1", "123456", "t"));

        assertThat(ex.getErrorConstant()).isEqualTo(ErrorConstant.OTP_NOT_FOUND);
        verify(validationService, never()).verifyOtp(any(), any());
    }

    @Test
    void verify_addressTakenInTheMeantime_shouldRefuseAndKeepTheOldIdentity() {
        MemberContactChange change = pending("new@example.com", null);
        when(validationService.getMemberById("member-1")).thenReturn(member);
        when(contactChangeRepository.findByMemberId("member-1")).thenReturn(Optional.of(change));
        when(validationService.verifyOtp(any(Member.class), any(VerifyOtpRequest.class))).thenReturn(new MemberOtp());
        doThrow(new BusinessException(ErrorConstant.EMAIL_ALREADY_EXIST_V2))
                .when(validationService).assertContactAvailable(member, "new@example.com", null);

        assertThrows(BusinessException.class, () -> service.verify("member-1", "123456", "t"));

        assertThat(member.getEmail()).isEqualTo("budi@example.com");
        verify(memberRepository, never()).save(any());
    }

    @Test
    void verify_inactiveMember_shouldBeRejected() {
        member.setStatus(ActiveStatus.SUSPENDED);
        when(validationService.getMemberById("member-1")).thenReturn(member);

        BusinessException ex = assertThrows(BusinessException.class, () -> service.verify("member-1", "123456", "t"));

        assertThat(ex.getErrorConstant()).isEqualTo(ErrorConstant.MEMBER_NOT_ACTIVE);
    }

    // ---- resend ------------------------------------------------------------
    @Test
    void resend_afterCooldown_shouldSendANewOtpToTheSameTarget() {
        MemberContactChange change = pending("new@example.com", null);
        MemberOtp old = new MemberOtp();
        old.setCreatedAt(LocalDateTime.now().minusMinutes(3));
        when(validationService.getMemberById("member-1")).thenReturn(member);
        when(contactChangeRepository.findByMemberId("member-1")).thenReturn(Optional.of(change));
        when(memberOtpRepository.findTopByPhoneNumberAndOtpTypeOrderByCreatedAtDesc("628123456789", OtpType.CHANGE_CONTACT))
                .thenReturn(Optional.of(old));
        when(passwordEncoder.encode(anyString())).thenReturn("hashed");

        String sentTo = service.resend("member-1");

        assertThat(sentTo).isEqualTo("new@example.com");
        verify(otpService).sendOtp(anyString(), eq("new@example.com"), anyString(), anyString());
    }

    @Test
    void resend_withoutPendingChange_shouldBeRejected() {
        when(validationService.getMemberById("member-1")).thenReturn(member);
        when(contactChangeRepository.findByMemberId("member-1")).thenReturn(Optional.empty());

        assertThrows(BusinessException.class, () -> service.resend("member-1"));

        verify(otpService, never()).sendOtp(anyString(), anyString(), anyString(), anyString());
    }
}
