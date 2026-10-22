package com.vyra.be_virtual_your_assets.controller;

import com.vyra.be_virtual_your_assets.constant.ApiPath;
import com.vyra.be_virtual_your_assets.dto.BaseResponse;
import com.vyra.be_virtual_your_assets.dto.settings.SystemConfigResponse;
import com.vyra.be_virtual_your_assets.dto.settings.UpdateSystemConfigRequest;
import com.vyra.be_virtual_your_assets.security.model.CustomAdminDetails;
import com.vyra.be_virtual_your_assets.service.SystemConfigService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

import static com.vyra.be_virtual_your_assets.constant.ApiPath.SETTINGS_LIST;
import static com.vyra.be_virtual_your_assets.constant.ApiPath.SETTINGS_UPDATE;

/**
 * Portal-managed system settings (SETTINGS_VIEW to read, SETTINGS_MANAGE to change).
 */
@RestController
@RequiredArgsConstructor
@RequestMapping(ApiPath.V1_ADMIN_SETTINGS)
public class AdminSettingsController {

    private final SystemConfigService systemConfigService;

    @GetMapping(SETTINGS_LIST)
    public BaseResponse<List<SystemConfigResponse>> getSettings() {
        return systemConfigService.getSettings();
    }

    @PutMapping(SETTINGS_UPDATE)
    public BaseResponse<SystemConfigResponse> updateSetting(
            @PathVariable String configKey,
            @Valid @RequestBody UpdateSystemConfigRequest request,
            Authentication authentication
    ) {
        CustomAdminDetails currentAdmin = (CustomAdminDetails) authentication.getPrincipal();
        return systemConfigService.updateSetting(configKey, request, currentAdmin);
    }
}
