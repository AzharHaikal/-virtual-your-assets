//package com.vyra.virtual_your_assets.controller;
//
//import com.vyra.virtual_your_assets.constant.ErrorConstant;
//import com.vyra.virtual_your_assets.dto.BaseResponse;
//import com.vyra.virtual_your_assets.dto.wallet.CreateWalletRequest;
//import com.vyra.virtual_your_assets.dto.wallet.CreateWalletResponse;
//import com.vyra.virtual_your_assets.service.WalletService;
//import org.junit.jupiter.api.Test;
//import org.junit.jupiter.api.extension.ExtendWith;
//import org.mockito.InjectMocks;
//import org.mockito.Mock;
//import org.mockito.junit.jupiter.MockitoExtension;
//
//import static org.junit.jupiter.api.Assertions.*;
//import static org.mockito.ArgumentMatchers.any;
//import static org.mockito.Mockito.when;
//
//@ExtendWith(MockitoExtension.class)
//class WalletControllerTest {
//    @Mock private WalletService service;
//    @InjectMocks WalletController controller;
//
//    @Test
//    void createWalletSuccess() {
//        CreateWalletRequest request = new CreateWalletRequest();
//        request.setPhoneNumber("62895337276087");
//
//        CreateWalletResponse createWalletResponse = new CreateWalletResponse();
//        createWalletResponse.setPhoneNumber(request.getPhoneNumber());
//
//        BaseResponse<CreateWalletResponse> mockResponse = new BaseResponse<>(
//                ErrorConstant.CREATE_WALLET_SUCCESS.getCode(),
//                ErrorConstant.CREATE_WALLET_SUCCESS.getMessage(),
//                createWalletResponse
//        );
//
//        when(service.createMemberWallet(any(CreateWalletRequest.class))).thenReturn(mockResponse);
//
//        BaseResponse<CreateWalletResponse> response = controller.createWallet(request);
//        assertEquals(ErrorConstant.CREATE_WALLET_SUCCESS.getCode(), response.getResponseStatus());
//        assertEquals(ErrorConstant.CREATE_WALLET_SUCCESS.getMessage(), response.getResponseMessage());
//    }
//}