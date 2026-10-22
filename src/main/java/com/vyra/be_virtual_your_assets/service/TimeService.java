package com.vyra.be_virtual_your_assets.service;

import com.vyra.be_virtual_your_assets.constant.ErrorConstant;
import com.vyra.be_virtual_your_assets.entity.Member;
import com.vyra.be_virtual_your_assets.exception.BadRequestException;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.DateTimeException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.format.DateTimeParseException;

/**
 * Single place for "what time / which day is it" so different time zones never conflict.
 * <ul>
 *   <li>Every {@link LocalDateTime} in the application is a <b>UTC</b> instant (JVM, JDBC session and
 *       Hibernate run in UTC; columns are timestamptz; JSON carries a trailing {@code Z}).</li>
 *   <li>A day that belongs to a member ("today", a transaction's date) is a {@link LocalDate} in the
 *       member's own zone ({@code member.timezone}), fixed when the data is written.</li>
 *   <li>Day-based admin filters/reports use the default zone ({@code vyra.time.default-zone}).</li>
 * </ul>
 */
@Service
public class TimeService {

    public static final String FALLBACK_ZONE_ID = "Asia/Jakarta";

    private final Clock clock;
    private final ZoneId defaultZone;

    public TimeService(Clock clock, @Value("${vyra.time.default-zone:" + FALLBACK_ZONE_ID + "}") String defaultZone) {
        this.clock = clock;
        this.defaultZone = ZoneId.of(defaultZone);
    }

    /** Current instant, UTC. */
    public LocalDateTime nowUtc() {
        return LocalDateTime.now(clock.withZone(ZoneOffset.UTC));
    }

    public ZoneId defaultZone() {
        return defaultZone;
    }

    /** The member's zone; the default zone when missing or invalid. */
    public ZoneId zoneOf(Member member) {
        return parseZoneOrDefault(member != null ? member.getTimezone() : null);
    }

    public LocalDate today(ZoneId zone) {
        return LocalDate.now(clock.withZone(zone));
    }

    /** First UTC instant of {@code date} in {@code zone}. */
    public LocalDateTime startOfDayUtc(LocalDate date, ZoneId zone) {
        return date.atStartOfDay(zone).withZoneSameInstant(ZoneOffset.UTC).toLocalDateTime();
    }

    /** The date in {@code zone} of a UTC instant. */
    public LocalDate toLocalDate(LocalDateTime utc, ZoneId zone) {
        return utc.atOffset(ZoneOffset.UTC).atZoneSameInstant(zone).toLocalDate();
    }

    /**
     * Parses a date-time sent by a client into a UTC instant.
     * ISO-8601 with an offset ({@code 2026-10-22T08:30:00+07:00}, {@code ...Z}) is exact; without an offset
     * ({@code 2026-10-22T08:30:00}, older app versions) it is read in {@code zoneWithoutOffset}.
     * Blank means now.
     */
    public LocalDateTime parseClientDateTime(String value, ZoneId zoneWithoutOffset) {
        if (StringUtils.isBlank(value)) {
            return nowUtc();
        }
        String text = value.trim();
        try {
            return OffsetDateTime.parse(text).atZoneSameInstant(ZoneOffset.UTC).toLocalDateTime();
        } catch (DateTimeParseException withoutOffset) {
            try {
                return LocalDateTime.parse(text).atZone(zoneWithoutOffset)
                        .withZoneSameInstant(ZoneOffset.UTC).toLocalDateTime();
            } catch (DateTimeParseException invalid) {
                throw new BadRequestException(ErrorConstant.INVALID_DATE_TIME);
            }
        }
    }

    /** Validates an IANA zone id (e.g. Asia/Jakarta) and returns it normalised. */
    public String requireValidZoneId(String zoneId) {
        try {
            ZoneId zone = ZoneId.of(StringUtils.trimToEmpty(zoneId));
            if (zone instanceof ZoneOffset) {
                // A fixed offset ignores daylight saving rules; insist on a region id.
                throw new BadRequestException(ErrorConstant.INVALID_TIMEZONE);
            }
            return zone.getId();
        } catch (DateTimeException e) {
            throw new BadRequestException(ErrorConstant.INVALID_TIMEZONE);
        }
    }

    private ZoneId parseZoneOrDefault(String zoneId) {
        if (StringUtils.isBlank(zoneId)) {
            return defaultZone;
        }
        try {
            return ZoneId.of(zoneId);
        } catch (DateTimeException e) {
            return defaultZone;
        }
    }
}
