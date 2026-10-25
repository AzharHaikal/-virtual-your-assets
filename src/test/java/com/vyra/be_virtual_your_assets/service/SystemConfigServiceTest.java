package com.vyra.be_virtual_your_assets.service;

import com.vyra.be_virtual_your_assets.constant.AdminActivityEvent;
import com.vyra.be_virtual_your_assets.constant.AdminPermission;
import com.vyra.be_virtual_your_assets.constant.AdminRole;
import com.vyra.be_virtual_your_assets.constant.ErrorConstant;
import com.vyra.be_virtual_your_assets.constant.SystemConfigKey;
import com.vyra.be_virtual_your_assets.dto.BaseResponse;
import com.vyra.be_virtual_your_assets.dto.settings.SystemConfigResponse;
import com.vyra.be_virtual_your_assets.dto.settings.UpdateSystemConfigRequest;
import com.vyra.be_virtual_your_assets.entity.SystemConfig;
import com.vyra.be_virtual_your_assets.exception.BadRequestException;
import com.vyra.be_virtual_your_assets.exception.ForbiddenException;
import com.vyra.be_virtual_your_assets.repository.SystemConfigRepository;
import com.vyra.be_virtual_your_assets.security.model.CustomAdminDetails;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SystemConfigServiceTest {

    private static final String KEY = SystemConfigKey.MEMBER_TRANSACTION_DETAIL_ENABLED.getKey();

    @Mock private SystemConfigRepository systemConfigRepository;
    @Mock private AdminAuthorizationService adminAuthorizationService;
    @Mock private AdminActivityService adminActivityService;
    @Mock private SystemSettings systemSettings;

    @InjectMocks
    private SystemConfigService systemConfigService;

    private final CustomAdminDetails superAdmin = new CustomAdminDetails("super-admin-id", "token", AdminRole.SUPER_ADMIN);

    // =========================================================================
    // getSettings
    // =========================================================================
    @Test
    void getSettings_storedValue_shouldBeReturnedWithMetadata() {
        when(systemConfigRepository.findAll()).thenReturn(List.of(config("false")));

        BaseResponse<List<SystemConfigResponse>> result = systemConfigService.getSettings();

        verify(adminAuthorizationService).requirePermission(AdminPermission.SETTINGS_VIEW);
        assertThat(result.getResponseStatus()).isEqualTo(ErrorConstant.SYSTEM_CONFIG_LIST_SUCCESS.getCode());
        SystemConfigResponse item = result.getData().get(0);
        assertThat(item.getKey()).isEqualTo(KEY);
        assertThat(item.getValue()).isEqualTo("false");
        assertThat(item.getType()).isEqualTo(SystemConfigKey.ValueType.BOOLEAN);
        assertThat(item.getUpdatedBy()).isEqualTo("super-admin-id");
    }

    @Test
    void getSettings_missingRow_shouldFallBackToDefault() {
        when(systemConfigRepository.findAll()).thenReturn(List.of());

        SystemConfigResponse item = systemConfigService.getSettings().getData().get(0);

        assertThat(item.getValue()).isEqualTo(SystemConfigKey.MEMBER_TRANSACTION_DETAIL_ENABLED.getDefaultValue());
        assertThat(item.getUpdatedAt()).isNull();
    }

    // =========================================================================
    // updateSetting
    // =========================================================================
    @Test
    void updateSetting_validBoolean_shouldSaveNormalizedValueAndAudit() {
        when(systemConfigRepository.findById(KEY)).thenReturn(Optional.of(config("true")));

        BaseResponse<SystemConfigResponse> result =
                systemConfigService.updateSetting(KEY, request(" FALSE "), superAdmin);

        verify(adminAuthorizationService).requirePermission(AdminPermission.SETTINGS_MANAGE);
        ArgumentCaptor<SystemConfig> saved = ArgumentCaptor.forClass(SystemConfig.class);
        verify(systemConfigRepository).save(saved.capture());
        assertThat(saved.getValue().getConfigValue()).isEqualTo("false");
        assertThat(saved.getValue().getUpdatedBy()).isEqualTo("super-admin-id");
        assertThat(saved.getValue().getUpdatedAt()).isNotNull();
        assertThat(result.getData().getValue()).isEqualTo("false");
        verify(adminActivityService).createAdminActivity("super-admin-id", null,
                AdminActivityEvent.UPDATE_SYSTEM_CONFIG, KEY + " = false", null, null);
    }

    @Test
    void updateSetting_missingRow_shouldCreateIt() {
        when(systemConfigRepository.findById(KEY)).thenReturn(Optional.empty());

        systemConfigService.updateSetting(KEY, request("true"), superAdmin);

        ArgumentCaptor<SystemConfig> saved = ArgumentCaptor.forClass(SystemConfig.class);
        verify(systemConfigRepository).save(saved.capture());
        assertThat(saved.getValue().getConfigKey()).isEqualTo(KEY);
        assertThat(saved.getValue().getConfigValue()).isEqualTo("true");
    }

    @Test
    void updateSetting_unknownKey_shouldThrowNotFound() {
        BadRequestException ex = assertThrows(BadRequestException.class,
                () -> systemConfigService.updateSetting("unknown.key", request("true"), superAdmin));

        assertThat(ex.getErrorConstant()).isEqualTo(ErrorConstant.SYSTEM_CONFIG_NOT_FOUND);
        verify(systemConfigRepository, never()).save(any());
    }

    @Test
    void updateSetting_nonBooleanValue_shouldThrowInvalidValue() {
        BadRequestException ex = assertThrows(BadRequestException.class,
                () -> systemConfigService.updateSetting(KEY, request("yes"), superAdmin));

        assertThat(ex.getErrorConstant()).isEqualTo(ErrorConstant.SYSTEM_CONFIG_INVALID_VALUE);
        verify(systemConfigRepository, never()).save(any());
    }

    @Test
    void updateSetting_withoutSettingsManage_shouldThrowForbidden() {
        doThrow(new ForbiddenException(ErrorConstant.ADMIN_UNAUTHORIZED))
                .when(adminAuthorizationService).requirePermission(AdminPermission.SETTINGS_MANAGE);
        CustomAdminDetails administrator = new CustomAdminDetails("admin-id", "token", AdminRole.ADMINISTRATOR);

        assertThrows(ForbiddenException.class,
                () -> systemConfigService.updateSetting(KEY, request("false"), administrator));

        verifyNoInteractions(systemConfigRepository, adminActivityService);
    }

    // =========================================================================
    // OTP channels: at least one usable channel stays on
    // =========================================================================
    @Test
    void updateSetting_whatsAppOnWithoutProvider_shouldBeRejected() {
        when(systemSettings.isWhatsAppProviderAvailable()).thenReturn(false);
        String key = SystemConfigKey.OTP_WHATSAPP_ENABLED.getKey();

        BadRequestException e = assertThrows(BadRequestException.class,
                () -> systemConfigService.updateSetting(key, request("true"), superAdmin));

        assertThat(e.getErrorConstant()).isEqualTo(ErrorConstant.OTP_CHANNEL_REQUIRED);
        verify(systemConfigRepository, never()).save(any());
    }

    @Test
    void updateSetting_emailOffWhileWhatsAppUnusable_shouldBeRejected() {
        when(systemSettings.isWhatsAppProviderAvailable()).thenReturn(false);
        String key = SystemConfigKey.OTP_EMAIL_ENABLED.getKey();

        assertThrows(BadRequestException.class, () -> systemConfigService.updateSetting(key, request("false"), superAdmin));
    }

    @Test
    void updateSetting_emailOffWithWhatsAppOn_shouldBeSavedAndEvictTheCache() {
        when(systemSettings.isWhatsAppProviderAvailable()).thenReturn(true);
        when(systemSettings.isEnabled(SystemConfigKey.OTP_WHATSAPP_ENABLED)).thenReturn(true);
        String key = SystemConfigKey.OTP_EMAIL_ENABLED.getKey();
        when(systemConfigRepository.findById(key)).thenReturn(Optional.empty());

        systemConfigService.updateSetting(key, request("false"), superAdmin);

        verify(systemConfigRepository).save(any(SystemConfig.class));
        verify(systemSettings).evict();
    }

    @Test
    void updateSetting_maintenanceMessage_keepsCaseAndTrims() {
        String key = SystemConfigKey.MAINTENANCE_MESSAGE.getKey();
        when(systemConfigRepository.findById(key)).thenReturn(Optional.empty());

        SystemConfigResponse result = systemConfigService.updateSetting(key, request("  Back at 10:00 WIB "), superAdmin)
                .getData();

        assertThat(result.getValue()).isEqualTo("Back at 10:00 WIB");
    }

    // =========================================================================
    // helpers
    // =========================================================================
    private static SystemConfig config(String value) {
        SystemConfig config = new SystemConfig();
        config.setConfigKey(KEY);
        config.setConfigValue(value);
        config.setUpdatedBy("super-admin-id");
        return config;
    }

    private static UpdateSystemConfigRequest request(String value) {
        UpdateSystemConfigRequest request = new UpdateSystemConfigRequest();
        request.setValue(value);
        return request;
    }
}
