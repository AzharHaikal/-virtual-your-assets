package com.vyra.be_virtual_your_assets.controller;

import java.util.List;
import com.vyra.be_virtual_your_assets.security.model.CustomUserDetails;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
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
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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
        when(walletService.createOwnWallet("member-uuid-001", request)).thenReturn(expected);

        BaseResponse<CreateWalletResponse> result = walletController.createWallet(request, authFor("member-uuid-001"));

        assertThat(result).isSameAs(expected);
        verify(walletService).createOwnWallet("member-uuid-001", request);
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
        when(walletService.getOwnWallet("member-uuid-001", phoneNumber)).thenReturn(expected);

        BaseResponse<GetMemberWalletResponse> result =
                walletController.getMemberWallet(phoneNumber, authFor("member-uuid-001"));

        assertThat(result).isSameAs(expected);
        verify(walletService).getOwnWallet("member-uuid-001", phoneNumber);
    }

    private static Authentication authFor(String memberId) {
        return new UsernamePasswordAuthenticationToken(new CustomUserDetails(memberId, "access-token"), null, List.of());
    }
}
