package com.vyra.be_virtual_your_assets.service;

import com.vyra.be_virtual_your_assets.constant.SystemConfigKey;
import com.vyra.be_virtual_your_assets.entity.SystemConfig;
import com.vyra.be_virtual_your_assets.repository.SystemConfigRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Read side of {@code admin_idp.system_config} for code that runs on every request (maintenance filter,
 * OTP channels). Values are cached for a few seconds and dropped right away when the portal changes one,
 * so a change reaches every request of this instance within {@link #TTL_MILLIS}.
 */
@Service
@RequiredArgsConstructor
public class SystemSettings {

    static final long TTL_MILLIS = 10_000;

    private final SystemConfigRepository systemConfigRepository;
    private final Clock clock;

    /** No WhatsApp Business API provider is wired yet (WhatsAppClient is a placeholder). */
    @Value("${vyra.otp.whatsapp-provider-available:false}")
    private boolean whatsAppProviderAvailable;

    private volatile Map<String, String> cache;
    private volatile long loadedAt;

    public boolean isEnabled(SystemConfigKey key) {
        return Boolean.parseBoolean(value(key));
    }

    public String text(SystemConfigKey key) {
        return value(key);
    }

    public boolean isWhatsAppProviderAvailable() {
        return whatsAppProviderAvailable;
    }

    /** Called after a setting changed. */
    public void evict() {
        cache = null;
    }

    private String value(SystemConfigKey key) {
        Map<String, String> values = cache;
        long now = clock.millis();
        if (values == null || now - loadedAt > TTL_MILLIS) {
            values = systemConfigRepository.findAll().stream()
                    .collect(Collectors.toMap(SystemConfig::getConfigKey, SystemConfig::getConfigValue, (a, b) -> a));
            cache = values;
            loadedAt = now;
        }
        return values.getOrDefault(key.getKey(), key.getDefaultValue());
    }
}
