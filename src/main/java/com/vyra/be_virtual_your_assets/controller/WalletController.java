package com.vyra.be_virtual_your_assets.controller;

import com.vyra.be_virtual_your_assets.constant.ApiPath;
import com.vyra.be_virtual_your_assets.dto.BaseResponse;
import com.vyra.be_virtual_your_assets.dto.wallet.CreateWalletRequest;
import com.vyra.be_virtual_your_assets.dto.wallet.CreateWalletResponse;
import com.vyra.be_virtual_your_assets.dto.wallet.GetMemberWalletResponse;
import com.vyra.be_virtual_your_assets.security.model.CustomUserDetails;
import com.vyra.be_virtual_your_assets.service.WalletService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import static com.vyra.be_virtual_your_assets.constant.ApiPath.CREATE_WALLET;
import static com.vyra.be_virtual_your_assets.constant.ApiPath.GET_WALLET;

@RestController
@RequiredArgsConstructor
@RequestMapping(ApiPath.V1_WALLET)
public class WalletController {
    private final WalletService walletService;

    @PostMapping(CREATE_WALLET)
    public BaseResponse<CreateWalletResponse> createWallet(
            @Valid @RequestBody CreateWalletRequest request,
            Authentication authentication
    ) {
        CustomUserDetails user = (CustomUserDetails) authentication.getPrincipal();
        return walletService.createOwnWallet(user.getMemberId(), request);
    }

    @GetMapping(GET_WALLET)
    public BaseResponse<GetMemberWalletResponse> getMemberWallet(
            @PathVariable String phoneNumber,
            Authentication authentication
    ) {
        CustomUserDetails user = (CustomUserDetails) authentication.getPrincipal();
        return walletService.getOwnWallet(user.getMemberId(), phoneNumber);
    }
}
