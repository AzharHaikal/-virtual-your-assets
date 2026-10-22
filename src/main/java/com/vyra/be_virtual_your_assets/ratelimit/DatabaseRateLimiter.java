package com.vyra.be_virtual_your_assets.ratelimit;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.concurrent.atomic.AtomicLong;

/**
 * Rate limiter whose counters live in PostgreSQL, so the limit holds across all backend
 * instances (an in-memory counter would multiply the limit by the number of instances).
 *
 * One atomic {@code INSERT ... ON CONFLICT DO UPDATE} per counted request; the window clock is
 * the database clock, so instances with skewed clocks agree. If the database call fails the
 * request is counted by a local in-memory limiter instead (degraded, never fully open).
 * Setting {@code vyra.security.rate-limit.store=memory} uses only the local limiter.
 */
@Component
@Slf4j
public class DatabaseRateLimiter implements RateLimiter {

    private static final String UPSERT = """
            WITH t AS (SELECT CAST(EXTRACT(EPOCH FROM clock_timestamp()) * 1000 AS BIGINT) AS now_ms)
            INSERT INTO idp.rate_limit_bucket AS b (bucket_key, window_start_millis, hits)
            SELECT ?, t.now_ms, 1 FROM t
            ON CONFLICT (bucket_key) DO UPDATE SET
                hits = CASE WHEN b.window_start_millis + ? <= EXCLUDED.window_start_millis
                            THEN 1 ELSE b.hits + 1 END,
                window_start_millis = CASE WHEN b.window_start_millis + ? <= EXCLUDED.window_start_millis
                            THEN EXCLUDED.window_start_millis ELSE b.window_start_millis END
            RETURNING b.hits, b.window_start_millis, (SELECT now_ms FROM t) AS now_ms
            """;

    private static final String PURGE = """
            DELETE FROM idp.rate_limit_bucket
            WHERE window_start_millis < CAST(EXTRACT(EPOCH FROM clock_timestamp()) * 1000 AS BIGINT) - 86400000
            """;

    private static final int PURGE_EVERY = 500;

    private final JdbcTemplate jdbcTemplate;
    private final boolean databaseStore;
    private final InMemoryRateLimiter fallback = new InMemoryRateLimiter();
    private final AtomicLong calls = new AtomicLong();

    @Autowired
    public DatabaseRateLimiter(JdbcTemplate jdbcTemplate,
                               @Value("${vyra.security.rate-limit.store:database}") String store) {
        this.jdbcTemplate = jdbcTemplate;
        this.databaseStore = !"memory".equalsIgnoreCase(store);
    }

    @Override
    public long tryAcquire(String key, int limit, long windowMillis) {
        if (!databaseStore) return fallback.tryAcquire(key, limit, windowMillis);

        try {
            long[] row = jdbcTemplate.queryForObject(UPSERT,
                    (rs, i) -> new long[]{rs.getLong("hits"), rs.getLong("window_start_millis"), rs.getLong("now_ms")},
                    key, windowMillis, windowMillis);
            maybePurge();
            if (row == null || row[0] <= limit) return 0;
            return Math.max(1, (row[1] + windowMillis - row[2] + 999) / 1000);
        } catch (DataAccessException e) {
            log.warn("Rate limit store unavailable, counting locally: {}", e.getMessage());
            return fallback.tryAcquire(key, limit, windowMillis);
        }
    }

    private void maybePurge() {
        if (calls.incrementAndGet() % PURGE_EVERY != 0) return;
        try {
            jdbcTemplate.update(PURGE);
        } catch (DataAccessException e) {
            log.debug("Rate limit purge skipped: {}", e.getMessage());
        }
    }
}
