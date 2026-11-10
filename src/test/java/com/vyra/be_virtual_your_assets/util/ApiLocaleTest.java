package com.vyra.be_virtual_your_assets.util;

import com.vyra.be_virtual_your_assets.constant.ErrorConstant;
import org.junit.jupiter.api.Test;

import java.util.Locale;

import static org.assertj.core.api.Assertions.assertThat;

class ApiLocaleTest {

    @Test
    void header_picksIndonesianOnlyWhenAskedFor() {
        assertThat(ApiLocale.fromHeader("id")).isEqualTo(ApiLocale.INDONESIAN);
        assertThat(ApiLocale.fromHeader("id-ID,id;q=0.9,en-US;q=0.8")).isEqualTo(ApiLocale.INDONESIAN);
        assertThat(ApiLocale.fromHeader("en")).isEqualTo(Locale.ENGLISH);
        assertThat(ApiLocale.fromHeader("en-US,en;q=0.9,id;q=0.8")).isEqualTo(Locale.ENGLISH);
        assertThat(ApiLocale.fromHeader("fr")).isEqualTo(Locale.ENGLISH);
        assertThat(ApiLocale.fromHeader(null)).isEqualTo(Locale.ENGLISH);
        assertThat(ApiLocale.fromHeader(";;;")).isEqualTo(Locale.ENGLISH);
    }

    @Test
    void localize_swapsAConstantsEnglishText() {
        ErrorConstant c = ErrorConstant.EMAIL_ALREADY_EXIST_V2;
        assertThat(ApiLocale.localize(c.getCode(), c.getMessage(), ApiLocale.INDONESIAN)).isEqualTo(c.getMessageId());
        assertThat(ApiLocale.localize(c.getCode(), c.getMessage(), Locale.ENGLISH)).isEqualTo(c.getMessage());
    }

    @Test
    void localize_picksTheRightConstantWhenTwoShareACode() {
        ErrorConstant trend = ErrorConstant.INVALID_TREND_PERIOD;
        ErrorConstant sort = ErrorConstant.INVALID_SORT;
        assertThat(trend.getCode()).isEqualTo(sort.getCode());
        assertThat(ApiLocale.localize(trend.getCode(), trend.getMessage(), ApiLocale.INDONESIAN)).isEqualTo(trend.getMessageId());
        assertThat(ApiLocale.localize(sort.getCode(), sort.getMessage(), ApiLocale.INDONESIAN)).isEqualTo(sort.getMessageId());
    }

    @Test
    void localize_translatesTheAppendedWait() {
        ErrorConstant c = ErrorConstant.MAX_ATTEMPTS_REACHED;
        assertThat(ApiLocale.localize(c.getCode(), c.getMessage() + "15 minutes.", ApiLocale.INDONESIAN))
                .isEqualTo(c.getMessageId() + "15 menit.");
        assertThat(ApiLocale.localize(ErrorConstant.OTP_COOLDOWN.getCode(),
                ErrorConstant.OTP_COOLDOWN.getMessage() + " 1 second.", ApiLocale.INDONESIAN))
                .isEqualTo(ErrorConstant.OTP_COOLDOWN.getMessageId() + " 1 detik.");
    }

    @Test
    void localize_leavesOtherTextAlone() {
        String portalText = "Kami sedang upgrade server sampai jam 10.";
        assertThat(ApiLocale.localize(ErrorConstant.MAINTENANCE_MODE.getCode(), portalText, ApiLocale.INDONESIAN))
                .isEqualTo(portalText);
        assertThat(ApiLocale.localize("UNKNOWN", "Something", ApiLocale.INDONESIAN)).isEqualTo("Something");
    }
}
