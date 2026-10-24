package com.vyra.be_virtual_your_assets.controller;

import com.vyra.be_virtual_your_assets.dto.BaseResponse;
import com.vyra.be_virtual_your_assets.dto.common.PageResponse;
import com.vyra.be_virtual_your_assets.dto.export.ExportFile;
import com.vyra.be_virtual_your_assets.dto.transaction.TransactionHistoryResponse;
import com.vyra.be_virtual_your_assets.dto.transaction.TransactionMutationResponse;
import com.vyra.be_virtual_your_assets.dto.transaction.UpdateTransactionRequest;
import com.vyra.be_virtual_your_assets.security.model.CustomUserDetails;
import com.vyra.be_virtual_your_assets.service.FinanceService;
import com.vyra.be_virtual_your_assets.service.TransactionExportService;
import com.vyra.be_virtual_your_assets.service.TransactionService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MemberTransactionControllerTest {

    @Mock private TransactionService transactionService;
    @Mock private FinanceService financeService;
    @Mock private TransactionExportService transactionExportService;

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
    void list_shouldPassTheFiltersForTheAuthenticatedMember() {
        BaseResponse<PageResponse<TransactionHistoryResponse>> expected =
                new BaseResponse<>("VYRA-TRX-LST-000", "ok", null);
        when(financeService.listTransactions("member-1", null, "c-1", null, null, "kopi", "amount,desc", 0, 20))
                .thenReturn(expected);

        assertThat(controller.listTransactions(null, "c-1", null, null, "kopi", "amount,desc", 0, 20, authentication))
                .isSameAs(expected);
    }

    @Test
    void delete_shouldUseTheAuthenticatedMember() {
        BaseResponse<TransactionMutationResponse> expected = new BaseResponse<>("VYRA-DTS-000", "ok", null);
        when(transactionService.deleteOwnTransaction("member-1", "trx-1")).thenReturn(expected);

        assertThat(controller.deleteTransaction("trx-1", authentication)).isSameAs(expected);
    }

    @Test
    void export_isACsvAttachment_forTheAuthenticatedMember() {
        LocalDate from = LocalDate.of(2026, 10, 1);
        LocalDate to = LocalDate.of(2026, 10, 31);
        when(transactionExportService.exportCsv("member-1", from, to))
                .thenReturn(new ExportFile("vyra-transactions-2026-10-01_2026-10-31.csv", "text/csv", new byte[]{1}));

        ResponseEntity<byte[]> response = controller.exportTransactions(from, to, authentication);

        assertThat(response.getHeaders().getContentType()).hasToString("text/csv");
        assertThat(response.getHeaders().getContentDisposition().getFilename())
                .isEqualTo("vyra-transactions-2026-10-01_2026-10-31.csv");
        assertThat(response.getBody()).containsExactly(1);
    }
}
