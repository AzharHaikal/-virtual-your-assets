package com.vyra.be_virtual_your_assets.service;

import com.vyra.be_virtual_your_assets.constant.AdminActivityEvent;
import com.vyra.be_virtual_your_assets.constant.AdminPermission;
import com.vyra.be_virtual_your_assets.constant.ErrorConstant;
import com.vyra.be_virtual_your_assets.constant.SystemConfigKey;
import com.vyra.be_virtual_your_assets.dto.BaseResponse;
import com.vyra.be_virtual_your_assets.dto.settings.SystemConfigResponse;
import com.vyra.be_virtual_your_assets.dto.settings.UpdateSystemConfigRequest;
import com.vyra.be_virtual_your_assets.entity.SystemConfig;
import com.vyra.be_virtual_your_assets.exception.BadRequestException;
import com.vyra.be_virtual_your_assets.repository.SystemConfigRepository;
import com.vyra.be_virtual_your_assets.security.model.CustomAdminDetails;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Portal settings stored in {@code admin_idp.system_config}. Only keys defined in
 * {@link SystemConfigKey} exist; a missing row falls back to the key's default value.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class SystemConfigService {

    private final SystemConfigRepository systemConfigRepository;
    private final AdminAuthorizationService adminAuthorizationService;
    private final AdminActivityService adminActivityService;
    private final SystemSettings systemSettings;

    public BaseResponse<List<SystemConfigResponse>> getSettings() {
        adminAuthorizationService.requirePermission(AdminPermission.SETTINGS_VIEW);

        Map<String, SystemConfig> stored = systemConfigRepository.findAll()
                .stream()
                .collect(Collectors.toMap(SystemConfig::getConfigKey, Function.identity()));

        List<SystemConfigResponse> items = Arrays.stream(SystemConfigKey.values())
                .map(key -> toResponse(key, stored.get(key.getKey())))
                .collect(Collectors.toList());

        return new BaseResponse<>(
                ErrorConstant.SYSTEM_CONFIG_LIST_SUCCESS.getCode(),
                ErrorConstant.SYSTEM_CONFIG_LIST_SUCCESS.getMessage(),
                items
        );
    }

    @Transactional
    public BaseResponse<SystemConfigResponse> updateSetting(String configKey, UpdateSystemConfigRequest request,
                                                            CustomAdminDetails currentAdmin) {
        adminAuthorizationService.requirePermission(AdminPermission.SETTINGS_MANAGE);

        SystemConfigKey key = SystemConfigKey.fromKey(configKey)
                .orElseThrow(() -> new BadRequestException(ErrorConstant.SYSTEM_CONFIG_NOT_FOUND));
        String value = normalize(key, request.getValue());
        checkOtpChannels(key, value);

        SystemConfig config = systemConfigRepository.findById(key.getKey()).orElseGet(() -> {
            SystemConfig created = new SystemConfig();
            created.setConfigKey(key.getKey());
            return created;
        });
        config.setConfigValue(value);
        config.setUpdatedBy(currentAdmin.getAdminId());
        config.setUpdatedAt(LocalDateTime.now());
        systemConfigRepository.save(config);
        systemSettings.evict();

        adminActivityService.createAdminActivity(currentAdmin.getAdminId(), null,
                AdminActivityEvent.UPDATE_SYSTEM_CONFIG, key.getKey() + " = " + value, null, null);
        log.info("System setting {} set to {} by admin {}", key.getKey(), value, currentAdmin.getAdminId());

        return new BaseResponse<>(
                ErrorConstant.SYSTEM_CONFIG_UPDATE_SUCCESS.getCode(),
                ErrorConstant.SYSTEM_CONFIG_UPDATE_SUCCESS.getMessage(),
                toResponse(key, config)
        );
    }

    private static String normalize(SystemConfigKey key, String raw) {
        if (key.getType() == SystemConfigKey.ValueType.TEXT) {
            return raw.trim();
        }
        String value = raw.trim().toLowerCase(Locale.ROOT);
        if (key.getType() == SystemConfigKey.ValueType.BOOLEAN && !value.equals("true") && !value.equals("false")) {
            throw new BadRequestException(ErrorConstant.SYSTEM_CONFIG_INVALID_VALUE);
        }
        return value;
    }

    /**
     * Members must always be able to receive an OTP: WhatsApp can only be switched on with a provider, and
     * e-mail can only be switched off while WhatsApp is on.
     */
    private void checkOtpChannels(SystemConfigKey key, String value) {
        boolean whatsAppUsable = systemSettings.isWhatsAppProviderAvailable();
        if (key == SystemConfigKey.OTP_WHATSAPP_ENABLED && "true".equals(value) && !whatsAppUsable) {
            throw new BadRequestException(ErrorConstant.OTP_CHANNEL_REQUIRED);
        }
        if (key == SystemConfigKey.OTP_EMAIL_ENABLED && "false".equals(value)
                && !(whatsAppUsable && systemSettings.isEnabled(SystemConfigKey.OTP_WHATSAPP_ENABLED))) {
            throw new BadRequestException(ErrorConstant.OTP_CHANNEL_REQUIRED);
        }
        if (key == SystemConfigKey.OTP_WHATSAPP_ENABLED && "false".equals(value)
                && !systemSettings.isEnabled(SystemConfigKey.OTP_EMAIL_ENABLED)) {
            throw new BadRequestException(ErrorConstant.OTP_CHANNEL_REQUIRED);
        }
    }

    private static SystemConfigResponse toResponse(SystemConfigKey key, SystemConfig stored) {
        SystemConfigResponse response = new SystemConfigResponse();
        response.setKey(key.getKey());
        response.setType(key.getType());
        response.setDescription(key.getDescription());
        response.setValue(stored != null ? stored.getConfigValue() : key.getDefaultValue());
        if (stored != null) {
            response.setUpdatedBy(stored.getUpdatedBy());
            response.setUpdatedAt(stored.getUpdatedAt());
        }
        return response;
    }
}
