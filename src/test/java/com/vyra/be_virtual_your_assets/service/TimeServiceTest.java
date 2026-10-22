package com.vyra.be_virtual_your_assets.service;

import com.vyra.be_virtual_your_assets.constant.ErrorConstant;
import com.vyra.be_virtual_your_assets.entity.Member;
import com.vyra.be_virtual_your_assets.exception.BadRequestException;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

class TimeServiceTest {

    /** 2026-10-22 17:30 UTC = 2026-10-23 00:30 WIB = 2026-10-23 01:30 WITA. */
    private final TimeService timeService =
            new TimeService(Clock.fixed(Instant.parse("2026-10-22T17:30:00Z"), ZoneOffset.UTC), "Asia/Jakarta");

    private static final ZoneId JAKARTA = ZoneId.of("Asia/Jakarta");

    @Test
    void nowUtc_isTheClockInstantInUtc() {
        assertThat(timeService.nowUtc()).isEqualTo(LocalDateTime.of(2026, 10, 22, 17, 30));
    }

    @Test
    void today_dependsOnTheZone() {
        assertThat(timeService.today(JAKARTA)).isEqualTo(LocalDate.of(2026, 10, 23));
        assertThat(timeService.today(ZoneId.of("Europe/London"))).isEqualTo(LocalDate.of(2026, 10, 22));
    }

    @Test
    void zoneOf_usesMemberZoneAndFallsBackToDefault() {
        Member member = new Member();
        member.setTimezone("Asia/Makassar");
        assertThat(timeService.zoneOf(member)).isEqualTo(ZoneId.of("Asia/Makassar"));

        member.setTimezone("Not/AZone");
        assertThat(timeService.zoneOf(member)).isEqualTo(JAKARTA);

        member.setTimezone(null);
        assertThat(timeService.zoneOf(member)).isEqualTo(JAKARTA);
        assertThat(timeService.zoneOf(null)).isEqualTo(JAKARTA);
    }

    @Test
    void startOfDayUtc_andToLocalDate_areInverse() {
        LocalDateTime start = timeService.startOfDayUtc(LocalDate.of(2026, 10, 23), JAKARTA);

        assertThat(start).isEqualTo(LocalDateTime.of(2026, 10, 22, 17, 0));
        assertThat(timeService.toLocalDate(start, JAKARTA)).isEqualTo(LocalDate.of(2026, 10, 23));
        assertThat(timeService.toLocalDate(start.minusNanos(1), JAKARTA)).isEqualTo(LocalDate.of(2026, 10, 22));
    }

    @Test
    void parseClientDateTime_withOffsetIsExact() {
        assertThat(timeService.parseClientDateTime("2026-10-23T00:30:00+07:00", ZoneId.of("Asia/Jayapura")))
                .isEqualTo(LocalDateTime.of(2026, 10, 22, 17, 30));
        assertThat(timeService.parseClientDateTime("2026-10-22T17:30:00Z", JAKARTA))
                .isEqualTo(LocalDateTime.of(2026, 10, 22, 17, 30));
    }

    @Test
    void parseClientDateTime_withoutOffsetUsesTheGivenZone() {
        assertThat(timeService.parseClientDateTime("2026-10-23T00:30:00", JAKARTA))
                .isEqualTo(LocalDateTime.of(2026, 10, 22, 17, 30));
        assertThat(timeService.parseClientDateTime("2026-10-23T00:30:00.123", ZoneId.of("Asia/Makassar")))
                .isEqualTo(LocalDateTime.of(2026, 10, 22, 16, 30, 0, 123_000_000));
    }

    @Test
    void parseClientDateTime_blankMeansNow() {
        assertThat(timeService.parseClientDateTime(" ", JAKARTA)).isEqualTo(LocalDateTime.of(2026, 10, 22, 17, 30));
        assertThat(timeService.parseClientDateTime(null, JAKARTA)).isEqualTo(LocalDateTime.of(2026, 10, 22, 17, 30));
    }

    @Test
    void parseClientDateTime_invalidThrowsBadRequest() {
        BadRequestException ex = assertThrows(BadRequestException.class,
                () -> timeService.parseClientDateTime("kemarin", JAKARTA));
        assertThat(ex.getErrorConstant()).isEqualTo(ErrorConstant.INVALID_DATE_TIME);
    }

    @Test
    void requireValidZoneId_acceptsRegionIdsOnly() {
        assertThat(timeService.requireValidZoneId(" Asia/Jakarta ")).isEqualTo("Asia/Jakarta");

        for (String invalid : new String[]{"Mars/Base", "+07:00", "Z", "", null}) {
            BadRequestException ex = assertThrows(BadRequestException.class,
                    () -> timeService.requireValidZoneId(invalid), String.valueOf(invalid));
            assertThat(ex.getErrorConstant()).isEqualTo(ErrorConstant.INVALID_TIMEZONE);
        }
    }
}
