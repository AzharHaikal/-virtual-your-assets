package com.vyra.be_virtual_your_assets.controller;

import com.vyra.be_virtual_your_assets.dto.BaseResponse;
import com.vyra.be_virtual_your_assets.dto.audit.AdminAuditItemResponse;
import com.vyra.be_virtual_your_assets.dto.audit.MemberAuditItemResponse;
import com.vyra.be_virtual_your_assets.dto.common.PageResponse;
import com.vyra.be_virtual_your_assets.service.AuditService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuditControllerTest {

    @Mock
    private AuditService auditService;

    @InjectMocks
    private AuditController auditController;

    private final LocalDate START = LocalDate.of(2026, 6, 1);
    private final LocalDate END = LocalDate.of(2026, 6, 18);

    @Test
    void getUserAuditTrail_shouldDelegateToService() {
        BaseResponse<PageResponse<AdminAuditItemResponse>> expected = new BaseResponse<>("VYRA-AUD-USR-000", "Success", null);
        when(auditService.getUserAuditTrail("budi", START, END, 0, 10)).thenReturn(expected);

        BaseResponse<PageResponse<AdminAuditItemResponse>> result =
                auditController.getUserAuditTrail("budi", START, END, 0, 10);

        assertThat(result).isSameAs(expected);
        verify(auditService).getUserAuditTrail("budi", START, END, 0, 10);
    }

    @Test
    void getMemberAuditTrail_shouldDelegateToService() {
        BaseResponse<PageResponse<MemberAuditItemResponse>> expected = new BaseResponse<>("VYRA-AUD-MBR-000", "Success", null);
        when(auditService.getMemberAuditTrail(null, null, null, 0, 10)).thenReturn(expected);

        BaseResponse<PageResponse<MemberAuditItemResponse>> result =
                auditController.getMemberAuditTrail(null, null, null, 0, 10);

        assertThat(result).isSameAs(expected);
        verify(auditService).getMemberAuditTrail(null, null, null, 0, 10);
    }
}
