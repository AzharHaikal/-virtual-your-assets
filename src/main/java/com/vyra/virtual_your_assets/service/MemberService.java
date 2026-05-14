package com.vyra.virtual_your_assets.service;

import com.vyra.virtual_your_assets.constant.ErrorConstant;
import com.vyra.virtual_your_assets.constant.MemberActivityEvent;
import com.vyra.virtual_your_assets.dto.BaseResponse;
import com.vyra.virtual_your_assets.dto.member.GetMemberResponse;
import com.vyra.virtual_your_assets.dto.member.UpdateProfileRequest;
import com.vyra.virtual_your_assets.dto.member.UpdateProfileResponse;
import com.vyra.virtual_your_assets.dto.wallet.GetMemberWalletResponse;
import com.vyra.virtual_your_assets.entity.Member;
import com.vyra.virtual_your_assets.exception.BusinessException;
import com.vyra.virtual_your_assets.repository.MemberRepository;
import io.micrometer.tracing.annotation.NewSpan;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.logging.log4j.util.InternalException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Slf4j
public class MemberService {
    private final MemberRepository memberRepository;
    private final MemberActivityService memberActivityService;
    private final ValidationService validationService;
    private final WalletService walletService;

    @NewSpan
    public BaseResponse<GetMemberResponse> getMember(String memberId) {
        Member member = validationService.getMemberById(memberId);
        log.info("[START] memberService.getMember phoneNumber: {} ", member.getPhoneNumber());

        createMemberActivity(member.getPhoneNumber(), MemberActivityEvent.ATTEMPT_GET_MEMBER_DETAIL);

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
            createMemberActivity(member.getPhoneNumber(), MemberActivityEvent.FAILED_GET_MEMBER_DETAIL);
            throw e;
        } catch (Exception e) {
            log.error("[ERROR] Unexpected error during get member wallet {}, message: {}", member.getPhoneNumber(), e.getMessage(), e);
            createMemberActivity(member.getPhoneNumber(), MemberActivityEvent.FAILED_GET_MEMBER_DETAIL);
            throw new InternalException(ErrorConstant.INTERNAL_SERVER_ERROR.getMessage());
        }

        GetMemberResponse response = getGetMemberResponse(member, getWalletResponse);

        createMemberActivity(memberId, MemberActivityEvent.SUCCESS_GET_MEMBER_DETAIL);
        log.info("[END] memberService.getMember successfully memberId: {} ", memberId);

        return new BaseResponse<>(
                ErrorConstant.GET_MEMBER_SUCCESS.getCode(),
                ErrorConstant.GET_MEMBER_SUCCESS.getMessage(),
                response
        );
    }

    @Transactional
    public BaseResponse<UpdateProfileResponse> updateProfile(String memberId, UpdateProfileRequest request) {
        Member member = validationService.getMemberById(memberId);
        log.info("[START] memberService.updateProfile phoneNumber: {} ", member.getPhoneNumber());

        createMemberActivity(member.getPhoneNumber(), MemberActivityEvent.ATTEMPT_UPDATE_PROFILE);

        validationService.validateUpdateProfile(member, request);
        member.setModifiedBy(member.getPhoneNumber());
        member.setUpdatedAt(LocalDateTime.now());
        memberRepository.save(member);

        UpdateProfileResponse response = new UpdateProfileResponse();
        response.setFirstName(member.getFirstName());
        response.setLastName(member.getLastName());
        response.setEmail(member.getEmail());
        response.setPhoneNumber(member.getPhoneNumber());

        createMemberActivity(member.getPhoneNumber(), MemberActivityEvent.SUCCESS_UPDATE_PROFILE);
        log.info("[END] memberService.updateProfile successfully phoneNumber: {} ", member.getPhoneNumber());

        return new BaseResponse<>(
                ErrorConstant.UPDATE_PROFILE_SUCCESS.getCode(),
                ErrorConstant.UPDATE_PROFILE_SUCCESS.getMessage(),
                null
        );
    }

    private GetMemberResponse getGetMemberResponse(Member member, BaseResponse<GetMemberWalletResponse> getWalletResponse) {
        GetMemberResponse response = new GetMemberResponse();
        response.setFirstName(member.getFirstName());
        response.setLastName(member.getLastName());
        response.setFullName(member.getFirstName() + member.getLastName());
        response.setEmail(member.getPhoneNumber());
        response.setPhoneNumber(member.getPhoneNumber());
        response.setTotalCredit(getWalletResponse.getData().getTotalCredit());
        response.setTotalDebit(getWalletResponse.getData().getTotalDebit());
        response.setBalance(getWalletResponse.getData().getBalance());
        return response;
    }

    private void createMemberActivity(String phoneNumber, MemberActivityEvent activityEvent) {
        memberActivityService.createMemberActivity(phoneNumber, activityEvent);
    }
}
