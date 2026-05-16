package com.vyra.virtual_your_assets.controller;

import com.vyra.virtual_your_assets.constant.ApiPath;
import com.vyra.virtual_your_assets.dto.BaseResponse;
import com.vyra.virtual_your_assets.dto.wallet.CreateWalletRequest;
import com.vyra.virtual_your_assets.dto.wallet.CreateWalletResponse;
import com.vyra.virtual_your_assets.dto.wallet.GetMemberWalletResponse;
import com.vyra.virtual_your_assets.service.WalletService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import static com.vyra.virtual_your_assets.constant.ApiPath.CREATE_WALLET;
import static com.vyra.virtual_your_assets.constant.ApiPath.GET_WALLET;

@RestController
@RequiredArgsConstructor
@RequestMapping(ApiPath.V1_WALLET)
public class WalletController {
    private final WalletService walletService;

    @PostMapping(CREATE_WALLET)
    public BaseResponse<CreateWalletResponse> createWallet(@Valid CreateWalletRequest request) {
        return walletService.createWallet(request);
    }

    @GetMapping(GET_WALLET)
    public BaseResponse<GetMemberWalletResponse> getMemberWallet(@PathVariable String phoneNumber) {
        return walletService.getMemberWallet(phoneNumber);
    }
}
