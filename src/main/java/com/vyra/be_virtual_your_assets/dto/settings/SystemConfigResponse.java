package com.vyra.be_virtual_your_assets.dto.settings;

import com.vyra.be_virtual_your_assets.constant.SystemConfigKey;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class SystemConfigResponse {
    private String key;
    private String value;
    private SystemConfigKey.ValueType type;
    private String description;
    private String updatedBy;
    private LocalDateTime updatedAt;
}
