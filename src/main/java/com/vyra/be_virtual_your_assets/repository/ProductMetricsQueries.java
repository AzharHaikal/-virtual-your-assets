package com.vyra.be_virtual_your_assets.repository;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;

/**
 * Aggregate counts for the product metrics. Every query returns a number of members, never member data.
 * Times are UTC instants.
 */
@Repository
@RequiredArgsConstructor
public class ProductMetricsQueries {

    /** Any member activity that shows the app is used: transactions, mood check-ins, habit ticks, goal progress. */
    private static final String ACTIVITY = """
        SELECT member_id, created_at AS at FROM transaction_finance.transaction
        UNION ALL SELECT member_id, checked_in_at FROM wellbeing.mood_entry
        UNION ALL SELECT member_id, completed_at FROM wellbeing.habit_completion
        UNION ALL SELECT g.member_id, l.created_at FROM wellbeing.goal_progress_log l
                  JOIN wellbeing.goal g ON g.id = l.goal_id
        """;

    private final NamedParameterJdbcTemplate jdbc;

    public long activeMembers() {
        return count("SELECT COUNT(*) FROM idp.member WHERE status = 'ACTIVE'", Map.of());
    }

    public long newMembers(LocalDateTime since) {
        return count("SELECT COUNT(*) FROM idp.member WHERE created_at >= :since", Map.of("since", utc(since)));
    }

    /** New members since {@code since} with a first core action (onboarding not counted). */
    public long activatedMembers(LocalDateTime since) {
        return count("""
            SELECT COUNT(*) FROM idp.member m
            WHERE m.created_at >= :since
              AND EXISTS (SELECT 1 FROM idp.member_activation a
                          WHERE a.member_id = m.id AND a.event <> 'ONBOARDING_COMPLETED')
            """, Map.of("since", utc(since)));
    }

    public long onboardedMembers(LocalDateTime since) {
        return count("""
            SELECT COUNT(*) FROM idp.member m
            WHERE m.created_at >= :since AND m.onboarding_completed_at IS NOT NULL
            """, Map.of("since", utc(since)));
    }

    /**
     * Members registered in [from, until) and how many of them were active {@code days} or more days after
     * registering. Returns [cohort, retained].
     */
    public long[] retention(LocalDateTime from, LocalDateTime until, int days) {
        Map<String, Object> row = jdbc.queryForMap("""
            WITH cohort AS (SELECT id, created_at FROM idp.member WHERE created_at >= :from AND created_at < :until),
                 activity AS (%s)
            SELECT COUNT(*) AS cohort,
                   COUNT(*) FILTER (WHERE EXISTS (
                       SELECT 1 FROM activity a
                       WHERE a.member_id = c.id AND a.at >= c.created_at + make_interval(days => :days))) AS retained
            FROM cohort c
            """.formatted(ACTIVITY), new MapSqlParameterSource()
                .addValue("from", utc(from)).addValue("until", utc(until)).addValue("days", days));
        return new long[]{number(row.get("cohort")), number(row.get("retained"))};
    }

    /** [members with an active habit, of those with a completion since {@code since}]. */
    public long[] habitEngagement(LocalDateTime since) {
        return pair("""
            SELECT COUNT(DISTINCT h.member_id) AS eligible,
                   COUNT(DISTINCT h.member_id) FILTER (WHERE EXISTS (
                       SELECT 1 FROM wellbeing.habit_completion c
                       WHERE c.member_id = h.member_id AND c.completed_at >= :since)) AS engaged
            FROM wellbeing.habit h JOIN idp.member m ON m.id = h.member_id AND m.status = 'ACTIVE'
            WHERE h.deleted_at IS NULL AND h.archived_at IS NULL
            """, since);
    }

    /** [members with an active goal, of those with progress since {@code since}]. */
    public long[] goalEngagement(LocalDateTime since) {
        return pair("""
            SELECT COUNT(DISTINCT g.member_id) AS eligible,
                   COUNT(DISTINCT g.member_id) FILTER (WHERE EXISTS (
                       SELECT 1 FROM wellbeing.goal_progress_log l JOIN wellbeing.goal g2 ON g2.id = l.goal_id
                       WHERE g2.member_id = g.member_id AND l.created_at >= :since)
                     OR EXISTS (
                       SELECT 1 FROM wellbeing.goal_milestone ms JOIN wellbeing.goal g3 ON g3.id = ms.goal_id
                       WHERE g3.member_id = g.member_id AND ms.completed_at >= :since)) AS engaged
            FROM wellbeing.goal g JOIN idp.member m ON m.id = g.member_id AND m.status = 'ACTIVE'
            WHERE g.deleted_at IS NULL AND g.status = 'ACTIVE'
            """, since);
    }

    /** [active members, of those with a transaction since {@code since}]. */
    public long[] financeEngagement(LocalDateTime since) {
        return pair("""
            SELECT COUNT(*) AS eligible,
                   COUNT(*) FILTER (WHERE EXISTS (
                       SELECT 1 FROM transaction_finance.transaction t
                       WHERE t.member_id = m.id AND t.deleted_at IS NULL AND t.created_at >= :since)) AS engaged
            FROM idp.member m WHERE m.status = 'ACTIVE'
            """, since);
    }

    /** [active members, of those with a mood check-in since {@code since}]. */
    public long[] moodEngagement(LocalDateTime since) {
        return pair("""
            SELECT COUNT(*) AS eligible,
                   COUNT(*) FILTER (WHERE EXISTS (
                       SELECT 1 FROM wellbeing.mood_entry e
                       WHERE e.member_id = m.id AND e.deleted_at IS NULL AND e.checked_in_at >= :since)) AS engaged
            FROM idp.member m WHERE m.status = 'ACTIVE'
            """, since);
    }

    /** Last run of each scheduled job (ShedLock rows). Rows: [name, lockedAt UTC]. */
    public List<Object[]> jobRuns() {
        return jdbc.query("SELECT name, locked_at FROM notification.shedlock ORDER BY name", Map.of(),
                (rs, i) -> new Object[]{rs.getString(1), toUtc(rs.getObject(2, OffsetDateTime.class))});
    }

    public LocalDateTime lastPushSentAt() {
        return jdbc.query("SELECT MAX(sent_at) FROM notification.notification_delivery WHERE status = 'SENT'", Map.of(),
                rs -> rs.next() ? toUtc(rs.getObject(1, OffsetDateTime.class)) : null);
    }

    public long pendingPush() {
        return count("SELECT COUNT(*) FROM notification.notification_delivery WHERE status = 'PENDING'", Map.of());
    }

    public long overduePush(LocalDateTime before) {
        return count("""
            SELECT COUNT(*) FROM notification.notification_delivery
            WHERE status = 'PENDING' AND deliver_after < :before
            """, Map.of("before", utc(before)));
    }

    public long pendingExports() {
        return count("SELECT COUNT(*) FROM idp.data_export WHERE status IN ('PENDING', 'PROCESSING')", Map.of());
    }

    /** Round trip to the database. */
    public void ping() {
        jdbc.queryForObject("SELECT 1", Map.of(), Integer.class);
    }

    private long[] pair(String sql, LocalDateTime since) {
        Map<String, Object> row = jdbc.queryForMap(sql, Map.of("since", utc(since)));
        return new long[]{number(row.get("eligible")), number(row.get("engaged"))};
    }

    private long count(String sql, Map<String, ?> params) {
        Long value = jdbc.queryForObject(sql, params, Long.class);
        return value == null ? 0 : value;
    }

    private static long number(Object value) {
        return value == null ? 0 : ((Number) value).longValue();
    }

    private static OffsetDateTime utc(LocalDateTime value) {
        return value.atOffset(ZoneOffset.UTC);
    }

    private static LocalDateTime toUtc(OffsetDateTime value) {
        return value == null ? null : value.withOffsetSameInstant(ZoneOffset.UTC).toLocalDateTime();
    }
}
