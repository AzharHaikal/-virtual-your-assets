package com.vyra.be_virtual_your_assets.controller;

import com.vyra.be_virtual_your_assets.constant.AdminRole;
import com.vyra.be_virtual_your_assets.dto.BaseResponse;
import com.vyra.be_virtual_your_assets.dto.settings.SystemConfigResponse;
import com.vyra.be_virtual_your_assets.dto.settings.UpdateSystemConfigRequest;
import com.vyra.be_virtual_your_assets.security.model.CustomAdminDetails;
import com.vyra.be_virtual_your_assets.service.SystemConfigService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminSettingsControllerTest {

    @Mock
    private SystemConfigService systemConfigService;

    @InjectMocks
    private AdminSettingsController adminSettingsController;

    @Test
    void getSettings_shouldDelegateToService() {
        BaseResponse<List<SystemConfigResponse>> expected = new BaseResponse<>("VYRA-CFG-LST-000", "Success", List.of());
        when(systemConfigService.getSettings()).thenReturn(expected);

        BaseResponse<List<SystemConfigResponse>> result = adminSettingsController.getSettings();

        assertThat(result).isSameAs(expected);
        verify(systemConfigService).getSettings();
    }

    @Test
    void updateSetting_shouldDelegateToServiceWithCurrentAdmin() {
        CustomAdminDetails admin = new CustomAdminDetails("super-admin-id", "token", AdminRole.SUPER_ADMIN);
        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(admin, null, admin.getAuthorities());
        UpdateSystemConfigRequest request = new UpdateSystemConfigRequest();
        request.setValue("false");
        BaseResponse<SystemConfigResponse> expected = new BaseResponse<>("VYRA-CFG-UPD-000", "Success", new SystemConfigResponse());
        when(systemConfigService.updateSetting("privacy.member_transaction_detail.enabled", request, admin)).thenReturn(expected);

        BaseResponse<SystemConfigResponse> result = adminSettingsController.updateSetting(
                "privacy.member_transaction_detail.enabled", request, authentication);

        assertThat(result).isSameAs(expected);
        verify(systemConfigService).updateSetting("privacy.member_transaction_detail.enabled", request, admin);
    }
}
