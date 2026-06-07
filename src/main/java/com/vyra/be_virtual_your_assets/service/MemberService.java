package com.vyra.be_virtual_your_assets.service;

import com.vyra.be_virtual_your_assets.constant.ErrorConstant;
import com.vyra.be_virtual_your_assets.constant.MemberActivityEvent;
import com.vyra.be_virtual_your_assets.dto.BaseResponse;
import com.vyra.be_virtual_your_assets.dto.auth.ChangePinRequest;
import com.vyra.be_virtual_your_assets.dto.member.GetMemberResponse;
import com.vyra.be_virtual_your_assets.dto.member.UpdateProfileRequest;
import com.vyra.be_virtual_your_assets.dto.member.UpdateProfileResponse;
import com.vyra.be_virtual_your_assets.dto.member.ValidateIsUpdateProfile;
import com.vyra.be_virtual_your_assets.dto.transaction.TransactionUpdateRequest;
import com.vyra.be_virtual_your_assets.dto.wallet.GetMemberWalletResponse;
import com.vyra.be_virtual_your_assets.dto.wallet.WalletUpdateRequest;
import com.vyra.be_virtual_your_assets.entity.Member;
import com.vyra.be_virtual_your_assets.exception.BusinessException;
import com.vyra.be_virtual_your_assets.repository.TransactionRepository;
import io.micrometer.tracing.annotation.NewSpan;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.logging.log4j.util.InternalException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.TemporalAdjusters;

@Service
@RequiredArgsConstructor
@Slf4j
public class MemberService {
    private final TransactionRepository transactionRepository;
    private final MemberActivityService memberActivityService;
    private final TransactionService transactionService;
    private final ValidationService validationService;
    private final WalletService walletService;
    private final BCryptPasswordEncoder passwordEncoder;

    @NewSpan
    public BaseResponse<GetMemberResponse> getMember(String memberId) {
        Member member = validationService.getMemberById(memberId);
        log.info("[START] memberService.getMember phoneNumber: {} ", member.getPhoneNumber());

        BaseResponse<GetMemberWalletResponse> getWalletResponse;
        try {
            log.info("Get member wallet. phoneNumber: {} ", member.getPhoneNumber());
            getWalletResponse = walletService.getMemberWallet(member.getPhoneNumber());
            if (!ErrorConstant.GET_MEMBER_WALLET_SUCCESS.getCode().equals(getWalletResponse.getResponseStatus())) {
                log.info("Failed when get member wallet. phoneNumber: {} ", member.getPhoneNumber());
                throw new BusinessException(ErrorConstant.CREATE_WALLET_FAILED);
            }
        } catch (BusinessException e) {
            log.error("[ERROR] get member wallet {} encountered an exception: {}", member.getPhoneNumber(), e.getMessage(), e);
            throw e;
        } catch (Exception e) {
            log.error("[ERROR] Unexpected error during get member wallet {}, message: {}", member.getPhoneNumber(), e.getMessage(), e);
            throw new InternalException(ErrorConstant.INTERNAL_SERVER_ERROR.getMessage());
        }

        GetMemberResponse response = getGetMemberResponse(member, getWalletResponse);
        log.info("[END] memberService.getMember successfully phoneNumber: {} ", member.getPhoneNumber());

        return new BaseResponse<>(
                ErrorConstant.GET_MEMBER_SUCCESS.getCode(),
                ErrorConstant.GET_MEMBER_SUCCESS.getMessage(),
                response
        );
    }

    @NewSpan
    @Transactional
    public BaseResponse<UpdateProfileResponse> updateProfile(String memberId, UpdateProfileRequest request) {
        Member member = validationService.getMemberById(memberId);

        String existingPhone = member.getPhoneNumber();
        String existingEmail = member.getEmail();

        log.info("[START] memberService.updateProfile phoneNumber: {}", existingPhone);
        createMemberActivity(existingPhone, MemberActivityEvent.ATTEMPT_UPDATE_PROFILE);

        ValidateIsUpdateProfile isUpdate = updateTransactionAndWallet(member, request, existingPhone, existingEmail);
        member.setModifiedBy(isUpdate.isPhoneChanged() ? request.getPhoneNumber(): existingPhone);
        member.setUpdatedAt(LocalDateTime.now());

        UpdateProfileResponse response = new UpdateProfileResponse();
        response.setFirstName(member.getFirstName());
        response.setLastName(member.getLastName());
        response.setEmail(member.getEmail());
        response.setPhoneNumber(member.getPhoneNumber());

        createMemberActivity(member.getPhoneNumber(), MemberActivityEvent.SUCCESS_UPDATE_PROFILE);
        log.info("[END] memberService.updateProfile successfully phoneNumber: {}", member.getPhoneNumber());

        return new BaseResponse<>(
                ErrorConstant.UPDATE_PROFILE_SUCCESS.getCode(),
                ErrorConstant.UPDATE_PROFILE_SUCCESS.getMessage(),
                response
        );
    }

    private ValidateIsUpdateProfile updateTransactionAndWallet(Member member, UpdateProfileRequest request, String existingPhone, String existingEmail) {
        ValidateIsUpdateProfile updateProfile = validationService.validateIsUpdateProfile(member, request);

        if (!updateProfile.isEmailChanged() && !updateProfile.isPhoneChanged()) return updateProfile;

        String effectiveNewPhone = updateProfile.isPhoneChanged() ? request.getPhoneNumber() : existingPhone;
        String effectiveNewEmail = updateProfile.isEmailChanged() ? request.getEmail() : existingEmail;

        if (updateProfile.isPhoneChanged()) {
            log.info("Syncing wallet data due to phone number change from {} to {}", existingPhone, effectiveNewPhone);
            WalletUpdateRequest walletRequest = new WalletUpdateRequest();
            walletRequest.setOldPhoneNumber(existingPhone);
            walletRequest.setNewPhoneNumber(effectiveNewPhone);
            walletService.updateMemberWallet(walletRequest);
        }

        log.info("Syncing transaction data for phoneNumber: {}", existingPhone);
        TransactionUpdateRequest txRequest = new TransactionUpdateRequest();
        txRequest.setOldPhoneNumber(existingPhone);
        txRequest.setNewPhoneNumber(effectiveNewPhone);
        txRequest.setEmail(effectiveNewEmail);
        transactionService.updateTransaction(txRequest);
        return updateProfile;
    }

    @NewSpan
    @Transactional
    public BaseResponse<Void> changePin(String memberId, ChangePinRequest request) {
        Member member = validationService.getMemberById(memberId);
        log.info("[START] authenticationService.changePin phoneNumber: {} ", member.getPhoneNumber());
        createMemberActivity(member.getPhoneNumber(), MemberActivityEvent.ATTEMPT_CHANGE_PIN);

        if (!passwordEncoder.matches(request.getOldPin(), member.getPin())) {
            log.info("Failed pin doesn't match. phoneNumber: {} ", member.getPhoneNumber());
            createMemberActivity(member.getPhoneNumber(), MemberActivityEvent.FAILED_CHANGE_PIN);
            throw new BusinessException(ErrorConstant.INVALID_PIN_MISS_MATCH);
        }

        member.setModifiedBy(member.getPhoneNumber());
        member.setUpdatedAt(LocalDateTime.now());
        member.setPin(passwordEncoder.encode(request.getNewPin()));

        createMemberActivity(member.getPhoneNumber(), MemberActivityEvent.SUCCESS_CHANGE_PIN);
        log.info("[END] authenticationService.changePin successfully phoneNumber: {} ", member.getPhoneNumber());

        return new BaseResponse<>(
                ErrorConstant.CHANGE_PIN_SUCCESS.getCode(),
                ErrorConstant.CHANGE_PIN_SUCCESS.getMessage(),
                null
        );
    }

    private GetMemberResponse getGetMemberResponse(Member member, BaseResponse<GetMemberWalletResponse> getWalletResponse) {
        GetMemberResponse response = new GetMemberResponse();
        response.setFirstName(member.getFirstName());
        response.setLastName(member.getLastName());
        response.setFullName(String.format("%s %s", member.getFirstName(), member.getLastName()).trim());
        response.setEmail(member.getEmail());
        response.setPhoneNumber(member.getPhoneNumber());
        response.setTotalIncome(getWalletResponse.getData().getTotalIncome());
        response.setTotalExpense(getWalletResponse.getData().getTotalExpense());
        response.setBalance(getWalletResponse.getData().getBalance());

        // Calculate Growth
        LocalDateTime startDate = LocalDate.now().minusMonths(1).withDayOfMonth(1).atStartOfDay();
        LocalDateTime endDate = LocalDate.now().minusMonths(1).with(TemporalAdjusters.lastDayOfMonth()).atTime(LocalTime.MAX);
        BigDecimal previousMonth = transactionRepository.getPreviousMonthIncome(member.getId(), startDate, endDate);

        response.setGrowthPercentage(calculateGrowth(getWalletResponse.getData().getBalance(), previousMonth));
        return response;
    }

    private BigDecimal calculateGrowth(BigDecimal current, BigDecimal previous) {
        if (previous.compareTo(BigDecimal.ZERO) == 0) {
            if (current.compareTo(BigDecimal.ZERO) == 0) {
                return BigDecimal.ZERO;
            }
            return BigDecimal.valueOf(100);
        }
        return current.subtract(previous).multiply(BigDecimal.valueOf(100)).divide(previous, 2, RoundingMode.HALF_UP);
    }

    private void createMemberActivity(String phoneNumber, MemberActivityEvent activityEvent) {
        memberActivityService.createMemberActivity(phoneNumber, activityEvent);
    }
}
