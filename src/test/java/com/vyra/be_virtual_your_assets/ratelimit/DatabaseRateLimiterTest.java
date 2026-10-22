package com.vyra.be_virtual_your_assets.ratelimit;

import org.junit.jupiter.api.Test;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class DatabaseRateLimiterTest {

    private final JdbcTemplate jdbcTemplate = mock(JdbcTemplate.class);

    @SuppressWarnings("unchecked")
    private void databaseReturns(long hits, long windowStart, long now) throws Exception {
        when(jdbcTemplate.queryForObject(anyString(), any(RowMapper.class), any(), any(), any()))
                .thenAnswer(invocation -> {
                    RowMapper<long[]> mapper = invocation.getArgument(1);
                    java.sql.ResultSet rs = mock(java.sql.ResultSet.class);
                    when(rs.getLong("hits")).thenReturn(hits);
                    when(rs.getLong("window_start_millis")).thenReturn(windowStart);
                    when(rs.getLong("now_ms")).thenReturn(now);
                    return mapper.mapRow(rs, 0);
                });
    }

    @Test
    void underTheLimit_isAllowed() throws Exception {
        databaseReturns(3, 1_000_000, 1_010_000);

        assertThat(new DatabaseRateLimiter(jdbcTemplate, "database").tryAcquire("login|1.1.1.1", 10, 60_000)).isZero();
    }

    @Test
    void exactlyAtTheLimit_isStillAllowed() throws Exception {
        databaseReturns(10, 1_000_000, 1_010_000);

        assertThat(new DatabaseRateLimiter(jdbcTemplate, "database").tryAcquire("k", 10, 60_000)).isZero();
    }

    @Test
    void overTheLimit_isRefusedWithTheRemainingWindowInSeconds() throws Exception {
        // window started 20 s ago, lasts 60 s -> 40 s left
        databaseReturns(11, 1_000_000, 1_020_000);

        assertThat(new DatabaseRateLimiter(jdbcTemplate, "database").tryAcquire("k", 10, 60_000)).isEqualTo(40);
    }

    @Test
    void whenTheDatabaseFails_requestsAreStillCountedLocally() {
        when(jdbcTemplate.queryForObject(anyString(), any(RowMapper.class), any(), any(), any()))
                .thenThrow(new DataAccessResourceFailureException("connection refused"));
        DatabaseRateLimiter limiter = new DatabaseRateLimiter(jdbcTemplate, "database");

        assertThat(limiter.tryAcquire("k", 2, 60_000)).isZero();
        assertThat(limiter.tryAcquire("k", 2, 60_000)).isZero();
        assertThat(limiter.tryAcquire("k", 2, 60_000)).isPositive();
    }

    @Test
    void memoryStore_neverTouchesTheDatabase() {
        DatabaseRateLimiter limiter = new DatabaseRateLimiter(jdbcTemplate, "memory");

        assertThat(limiter.tryAcquire("k", 1, 60_000)).isZero();
        assertThat(limiter.tryAcquire("k", 1, 60_000)).isPositive();

        verify(jdbcTemplate, never()).queryForObject(anyString(), any(RowMapper.class), any(), any(), any());
    }
}
