package com.vyra.be_virtual_your_assets.controller;

import com.vyra.be_virtual_your_assets.constant.transaction.TransactionType;
import com.vyra.be_virtual_your_assets.dto.BaseResponse;
import com.vyra.be_virtual_your_assets.dto.chart.GetChartResponse;
import com.vyra.be_virtual_your_assets.dto.transaction.CreateTransactionRequest;
import com.vyra.be_virtual_your_assets.dto.transaction.CreateTransactionResponse;
import com.vyra.be_virtual_your_assets.dto.transaction.TransactionHistoryResponse;
import com.vyra.be_virtual_your_assets.security.model.CustomUserDetails;
import com.vyra.be_virtual_your_assets.service.TransactionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.security.core.Authentication;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TransactionControllerTest {

    @Mock
    private TransactionService transactionService;

    @Mock
    private Authentication authentication;

    @InjectMocks
    private TransactionController transactionController;

    private final String MEMBER_ID = "member-uuid-003";
    private final String ACCESS_TOKEN = "access-token-trx";

    private CustomUserDetails userDetails;

    @BeforeEach
    void setUp() {
        userDetails = new CustomUserDetails(MEMBER_ID, ACCESS_TOKEN);
    }

    // =========================================================================
    // getChart
    // =========================================================================
    @Test
    void getChart_shouldExtractPrincipalAndDelegateToService() {
        String period = "1W";
        GetChartResponse chartData = new GetChartResponse();
        BaseResponse<GetChartResponse> expected = new BaseResponse<>("VYRA-GCS-000", "Chart data", chartData);
        when(authentication.getPrincipal()).thenReturn(userDetails);
        when(transactionService.getChart(MEMBER_ID, period)).thenReturn(expected);

        BaseResponse<GetChartResponse> result = transactionController.getChart(period, authentication);

        assertThat(result).isSameAs(expected);
        verify(authentication).getPrincipal();
        verify(transactionService).getChart(MEMBER_ID, period);
    }

    // =========================================================================
    // createTransaction
    // =========================================================================
    @Test
    void createTransaction_shouldExtractPrincipalAndDelegateToService() {
        CreateTransactionRequest request = new CreateTransactionRequest();
        CreateTransactionResponse data = new CreateTransactionResponse();
        BaseResponse<CreateTransactionResponse> expected = new BaseResponse<>("VYRA-CTS-000", "Created", data);
        when(authentication.getPrincipal()).thenReturn(userDetails);
        when(transactionService.createTransaction(MEMBER_ID, request)).thenReturn(expected);

        BaseResponse<CreateTransactionResponse> result = transactionController.createTransaction(request, authentication);

        assertThat(result).isSameAs(expected);
        verify(authentication).getPrincipal();
        verify(transactionService).createTransaction(MEMBER_ID, request);
    }

    // =========================================================================
    // getTopTransactionHistory
    // =========================================================================
    @Test
    void getTopTransactionHistory_shouldExtractPrincipalAndDelegateToService() {
        List<TransactionHistoryResponse> list = List.of(new TransactionHistoryResponse());
        BaseResponse<List<TransactionHistoryResponse>> expected = new BaseResponse<>("VYRA-GTT-000", "Top history", list);
        when(authentication.getPrincipal()).thenReturn(userDetails);
        when(transactionService.getTopTransactionHistory(MEMBER_ID)).thenReturn(expected);

        BaseResponse<List<TransactionHistoryResponse>> result = transactionController.getTopTransactionHistory(authentication);

        assertThat(result).isSameAs(expected);
        verify(authentication).getPrincipal();
        verify(transactionService).getTopTransactionHistory(MEMBER_ID);
    }

    // =========================================================================
    // getTransactionHistory — with type filter
    // =========================================================================
    @Test
    void getTransactionHistory_withType_shouldPassAllParamsToService() {
        String startDate = "2026-01-01";
        String endDate = "2026-06-01";
        TransactionType type = TransactionType.INCOME;
        int page = 0;
        int size = 10;

        Page<TransactionHistoryResponse> pageData = new PageImpl<>(List.of(new TransactionHistoryResponse()));
        BaseResponse<Page<TransactionHistoryResponse>> expected = new BaseResponse<>("VYRA-GTH-000", "History", pageData);

        when(authentication.getPrincipal()).thenReturn(userDetails);
        when(transactionService.getTransactionHistory(startDate, endDate, type, page, size, MEMBER_ID))
                .thenReturn(expected);

        BaseResponse<Page<TransactionHistoryResponse>> result =
                transactionController.getTransactionHistory(startDate, endDate, type, page, size, authentication);

        assertThat(result).isSameAs(expected);
        verify(authentication).getPrincipal();
        verify(transactionService).getTransactionHistory(startDate, endDate, type, page, size, MEMBER_ID);
    }

    // =========================================================================
    // getTransactionHistory — without type filter (null)
    // =========================================================================
    @Test
    void getTransactionHistory_withoutType_shouldPassNullTypeToService() {
        String startDate = "2026-01-01";
        String endDate = "2026-06-01";
        int page = 1;
        int size = 5;

        Page<TransactionHistoryResponse> pageData = new PageImpl<>(List.of());
        BaseResponse<Page<TransactionHistoryResponse>> expected = new BaseResponse<>("VYRA-GTH-000", "History", pageData);

        when(authentication.getPrincipal()).thenReturn(userDetails);
        when(transactionService.getTransactionHistory(startDate, endDate, null, page, size, MEMBER_ID))
                .thenReturn(expected);

        BaseResponse<Page<TransactionHistoryResponse>> result =
                transactionController.getTransactionHistory(startDate, endDate, null, page, size, authentication);

        assertThat(result).isSameAs(expected);
        verify(transactionService).getTransactionHistory(startDate, endDate, null, page, size, MEMBER_ID);
    }
}
