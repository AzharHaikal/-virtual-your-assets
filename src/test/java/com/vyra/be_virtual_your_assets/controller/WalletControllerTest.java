package com.vyra.be_virtual_your_assets.controller;

import com.vyra.be_virtual_your_assets.dto.BaseResponse;
import com.vyra.be_virtual_your_assets.dto.wallet.CreateWalletRequest;
import com.vyra.be_virtual_your_assets.dto.wallet.CreateWalletResponse;
import com.vyra.be_virtual_your_assets.dto.wallet.GetMemberWalletResponse;
import com.vyra.be_virtual_your_assets.service.WalletService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class WalletControllerTest {

    @Mock
    private WalletService walletService;

    @InjectMocks
    private WalletController walletController;

    // =========================================================================
    // createWallet
    // =========================================================================
    @Test
    void createWallet_shouldDelegateToServiceAndReturnResponse() {
        CreateWalletRequest request = new CreateWalletRequest();
        request.setPhoneNumber("628123456789");
        request.setMemberId("member-uuid-001");

        CreateWalletResponse data = new CreateWalletResponse();
        data.setMemberId("member-uuid-001");
        data.setPhoneNumber("628123456789");
        data.setBalance(BigDecimal.ZERO);

        BaseResponse<CreateWalletResponse> expected = new BaseResponse<>("VYRA-WLT-000", "Wallet created", data);
        when(walletService.createWallet(request)).thenReturn(expected);

        BaseResponse<CreateWalletResponse> result = walletController.createWallet(request);

        assertThat(result).isSameAs(expected);
        verify(walletService).createWallet(request);
    }

    // =========================================================================
    // getMemberWallet
    // =========================================================================
    @Test
    void getMemberWallet_shouldDelegateToServiceWithPhoneNumberAndReturnResponse() {
        String phoneNumber = "628123456789";

        GetMemberWalletResponse data = new GetMemberWalletResponse();
        data.setPhoneNumber(phoneNumber);
        data.setBalance(BigDecimal.valueOf(5000000));
        data.setTotalIncome(BigDecimal.valueOf(5000000));
        data.setTotalExpense(BigDecimal.ZERO);

        BaseResponse<GetMemberWalletResponse> expected = new BaseResponse<>("VYRA-GWS-000", "Wallet found", data);
        when(walletService.getMemberWallet(phoneNumber)).thenReturn(expected);

        BaseResponse<GetMemberWalletResponse> result = walletController.getMemberWallet(phoneNumber);

        assertThat(result).isSameAs(expected);
        verify(walletService).getMemberWallet(phoneNumber);
    }
}
