package com.vyra.virtual_your_assets.service;

import com.vyra.virtual_your_assets.constant.ErrorConstant;
import com.vyra.virtual_your_assets.constant.MemberActivityEvent;
import com.vyra.virtual_your_assets.dto.BaseResponse;
import com.vyra.virtual_your_assets.dto.member.GetMemberResponse;
import com.vyra.virtual_your_assets.dto.wallet.GetMemberWalletResponse;
import com.vyra.virtual_your_assets.entity.Member;
import com.vyra.virtual_your_assets.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.logging.log4j.util.InternalException;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class MemberService {
    private final MemberActivityService memberActivityService;
    private final ValidationService validationService;
    private final WalletService walletService;

    public BaseResponse<GetMemberResponse> getMember(String phoneNumber) {
        log.info("[START] memberService.getMember phoneNumber: {} ", phoneNumber);
        createMemberActivity(phoneNumber, MemberActivityEvent.ATTEMPT_GET_MEMBER_DETAIL);

        Member member = validationService.getMemberByPhoneNumber(phoneNumber);
        BaseResponse<GetMemberWalletResponse> getWalletResponse;
        try {
            log.info("Get member wallet. phoneNumber: {} ", phoneNumber);
            getWalletResponse = walletService.getMemberWallet(phoneNumber);
            if (!ErrorConstant.GET_MEMBER_WALLET_SUCCESS.getCode().equals(getWalletResponse.getResponseStatus())) {
                log.info("Failed when get member wallet. phoneNumber: {} ", phoneNumber);
                throw new BusinessException(ErrorConstant.CREATE_WALLET_FAILED);
            }

        } catch (BusinessException e) {
            log.error("[ERROR] get member wallet {} encountered an exception: {}", phoneNumber, e.getMessage(), e);
            createMemberActivity(phoneNumber, MemberActivityEvent.FAILED_GET_MEMBER_DETAIL);
            throw e;
        } catch (Exception e) {
            log.error("[ERROR] Unexpected error during get member wallet {}, message: {}", phoneNumber, e.getMessage(), e);
            createMemberActivity(phoneNumber, MemberActivityEvent.FAILED_GET_MEMBER_DETAIL);
            throw new InternalException(ErrorConstant.INTERNAL_SERVER_ERROR.getMessage());
        }

        GetMemberResponse response = new GetMemberResponse();
        response.setFirstName(member.getFirstName());
        response.setLastName(member.getLastName());
        response.setFullName(member.getFirstName() + member.getLastName());
        response.setEmail(member.getPhoneNumber());
        response.setPhoneNumber(member.getPhoneNumber());
        response.setTotalCredit(getWalletResponse.getData().getTotalCredit());
        response.setTotalDebit(getWalletResponse.getData().getTotalDebit());
        response.setBalance(getWalletResponse.getData().getBalance());

        createMemberActivity(phoneNumber, MemberActivityEvent.SUCCESS_GET_MEMBER_DETAIL);
        log.info("[END] memberService.getMember successfully phoneNumber: {} ", phoneNumber);

        return new BaseResponse<>(
                ErrorConstant.GET_MEMBER_SUCCESS.getCode(),
                ErrorConstant.GET_MEMBER_SUCCESS.getMessage(),
                response
        );
    }

    private void createMemberActivity(Object object, String activityEvent) {
        memberActivityService.createMemberActivity(object.toString(), activityEvent);
    }
}
