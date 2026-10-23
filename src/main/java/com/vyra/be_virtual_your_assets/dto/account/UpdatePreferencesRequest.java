package com.vyra.be_virtual_your_assets.dto.account;

import com.vyra.be_virtual_your_assets.constant.AppLanguage;
import com.vyra.be_virtual_your_assets.constant.AppTheme;
import jakarta.validation.constraints.Size;
import lombok.Data;

/** Only the fields that are sent change. */
@Data
public class UpdatePreferencesRequest {
    private AppTheme theme;

    private AppLanguage language;

    /** IANA region id, e.g. Asia/Jakarta. Only data written afterwards uses the new zone. */
    @Size(max = 64, message = "Invalid time zone")
    private String timezone;

    /** One of the supported ISO 4217 codes, e.g. IDR. */
    @Size(min = 3, max = 3, message = "Invalid currency")
    private String currency;
}
