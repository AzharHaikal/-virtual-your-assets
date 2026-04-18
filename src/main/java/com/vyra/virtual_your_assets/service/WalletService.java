package com.vyra.virtual_your_assets.service;

import com.vyra.virtual_your_assets.constant.ErrorConstant;
import com.vyra.virtual_your_assets.constant.MemberActivityEvent;
import com.vyra.virtual_your_assets.dto.BaseResponse;
import com.vyra.virtual_your_assets.dto.wallet.CreateWalletRequest;
import com.vyra.virtual_your_assets.dto.wallet.CreateWalletResponse;
import com.vyra.virtual_your_assets.entity.MemberWallet;
import com.vyra.virtual_your_assets.repository.MemberWalletRepository;
import io.micrometer.tracing.annotation.NewSpan;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Slf4j
public class WalletService {
    private final MemberWalletRepository memberWalletRepository;
    private final MemberActivityService memberActivityService;

    @Transactional
    @NewSpan
    public BaseResponse<CreateWalletResponse> createMemberWallet(CreateWalletRequest request) {
        log.info("[START] memberWalletService.createMemberWallet. phoneNumber: {} ", request.getPhoneNumber());
        memberActivityService.createMemberActivity(request.getPhoneNumber(), MemberActivityEvent.ATTEMPT_CREATE_WALLET);

        MemberWallet memberWallet = new MemberWallet();
        BeanUtils.copyProperties(request, memberWallet);
        memberWallet.setCreatedAt(LocalDateTime.now());
        memberWallet.setTotalCredit(BigDecimal.ZERO);
        memberWallet.setTotalDebit(BigDecimal.ZERO);
        memberWallet.setBalance(BigDecimal.ZERO);
        memberWalletRepository.save(memberWallet);

        CreateWalletResponse response = new CreateWalletResponse();
        response.setPhoneNumber(request.getPhoneNumber());

        memberActivityService.createMemberActivity(request.getPhoneNumber(), MemberActivityEvent.SUCCESS_CREATE_WALLET);
        log.info("[END] memberWalletService.createMemberWallet successfully. phoneNumber: {} ", request.getPhoneNumber());

        return new BaseResponse<>(
                ErrorConstant.CREATE_WALLET_SUCCESS.getCode(),
                ErrorConstant.CREATE_WALLET_SUCCESS.getMessage(),
                response
        );
    }
}
