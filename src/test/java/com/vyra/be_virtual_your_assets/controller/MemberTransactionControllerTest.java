package com.vyra.be_virtual_your_assets.controller;

import com.vyra.be_virtual_your_assets.dto.BaseResponse;
import com.vyra.be_virtual_your_assets.dto.transaction.TransactionMutationResponse;
import com.vyra.be_virtual_your_assets.dto.transaction.UpdateTransactionRequest;
import com.vyra.be_virtual_your_assets.security.model.CustomUserDetails;
import com.vyra.be_virtual_your_assets.service.TransactionService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MemberTransactionControllerTest {

    @Mock private TransactionService transactionService;

    @InjectMocks
    private MemberTransactionController controller;

    private final UsernamePasswordAuthenticationToken authentication =
            new UsernamePasswordAuthenticationToken(new CustomUserDetails("member-1", "token"), null);

    @Test
    void update_shouldUseTheAuthenticatedMember() {
        UpdateTransactionRequest request = new UpdateTransactionRequest();
        BaseResponse<TransactionMutationResponse> expected = new BaseResponse<>("VYRA-UTS-000", "ok", null);
        when(transactionService.updateOwnTransaction("member-1", "trx-1", request)).thenReturn(expected);

        assertThat(controller.updateTransaction("trx-1", request, authentication)).isSameAs(expected);
    }

    @Test
    void delete_shouldUseTheAuthenticatedMember() {
        BaseResponse<TransactionMutationResponse> expected = new BaseResponse<>("VYRA-DTS-000", "ok", null);
        when(transactionService.deleteOwnTransaction("member-1", "trx-1")).thenReturn(expected);

        assertThat(controller.deleteTransaction("trx-1", authentication)).isSameAs(expected);
    }
}
