package com.vyra.be_virtual_your_assets.controller;

import com.vyra.be_virtual_your_assets.constant.ActiveStatus;
import com.vyra.be_virtual_your_assets.constant.AdminRole;
import com.vyra.be_virtual_your_assets.dto.BaseResponse;
import com.vyra.be_virtual_your_assets.dto.adminmember.AdminMemberDetailResponse;
import com.vyra.be_virtual_your_assets.dto.adminmember.AdminMemberListItemResponse;
import com.vyra.be_virtual_your_assets.dto.adminmember.AdminMemberStatusUpdateRequest;
import com.vyra.be_virtual_your_assets.dto.adminmember.AdminMemberTransactionResponse;
import com.vyra.be_virtual_your_assets.dto.common.PageResponse;
import com.vyra.be_virtual_your_assets.security.model.CustomAdminDetails;
import com.vyra.be_virtual_your_assets.service.AdminMemberService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminMemberControllerTest {

    @Mock private AdminMemberService adminMemberService;
    @Mock private Authentication authentication;

    @InjectMocks
    private AdminMemberController adminMemberController;

    private CustomAdminDetails currentAdmin;

    @BeforeEach
    void setUp() {
        currentAdmin = new CustomAdminDetails("admin-id-001", "token", AdminRole.OPERATOR);
    }

    @Test
    void getMemberList_shouldDelegateToServiceWithAllParams() {
        BaseResponse<PageResponse<AdminMemberListItemResponse>> expected = new BaseResponse<>("VYRA-AMM-LST-000", "Success", null);
        when(adminMemberService.getMemberList("budi", ActiveStatus.ACTIVE, 0, 10)).thenReturn(expected);

        BaseResponse<PageResponse<AdminMemberListItemResponse>> result =
                adminMemberController.getMemberList("budi", ActiveStatus.ACTIVE, 0, 10);

        assertThat(result).isSameAs(expected);
        verify(adminMemberService).getMemberList("budi", ActiveStatus.ACTIVE, 0, 10);
    }

    @Test
    void getMemberDetail_shouldDelegateToService() {
        BaseResponse<AdminMemberDetailResponse> expected = new BaseResponse<>("VYRA-AMM-DTL-000", "Success", new AdminMemberDetailResponse());
        when(adminMemberService.getMemberDetail("member-id-001")).thenReturn(expected);

        BaseResponse<AdminMemberDetailResponse> result = adminMemberController.getMemberDetail("member-id-001");

        assertThat(result).isSameAs(expected);
        verify(adminMemberService).getMemberDetail("member-id-001");
    }

    @Test
    void getMemberTransactions_shouldDelegateToService() {
        BaseResponse<PageResponse<AdminMemberTransactionResponse>> expected = new BaseResponse<>("VYRA-AMM-TRX-000", "Success", null);
        when(adminMemberService.getMemberTransactions("member-id-001", 0, 10)).thenReturn(expected);

        BaseResponse<PageResponse<AdminMemberTransactionResponse>> result =
                adminMemberController.getMemberTransactions("member-id-001", 0, 10);

        assertThat(result).isSameAs(expected);
        verify(adminMemberService).getMemberTransactions("member-id-001", 0, 10);
    }

    @Test
    void updateMemberStatus_shouldExtractPrincipalAndDelegateToService() {
        AdminMemberStatusUpdateRequest request = new AdminMemberStatusUpdateRequest();
        when(authentication.getPrincipal()).thenReturn(currentAdmin);
        BaseResponse<Void> expected = new BaseResponse<>("VYRA-AMM-STS-000", "Success", null);
        when(adminMemberService.updateMemberStatus("member-id-001", request, currentAdmin)).thenReturn(expected);

        BaseResponse<Void> result = adminMemberController.updateMemberStatus("member-id-001", request, authentication);

        assertThat(result).isSameAs(expected);
        verify(adminMemberService).updateMemberStatus("member-id-001", request, currentAdmin);
    }

    @Test
    void deleteMember_shouldExtractPrincipalAndDelegateToService() {
        when(authentication.getPrincipal()).thenReturn(currentAdmin);
        BaseResponse<Void> expected = new BaseResponse<>("VYRA-AMM-DEL-000", "Success", null);
        when(adminMemberService.deleteMember("member-id-001", currentAdmin)).thenReturn(expected);

        BaseResponse<Void> result = adminMemberController.deleteMember("member-id-001", authentication);

        assertThat(result).isSameAs(expected);
        verify(adminMemberService).deleteMember("member-id-001", currentAdmin);
    }
}
