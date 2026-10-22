package com.vyra.be_virtual_your_assets.ratelimit;

import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicLong;

import static org.assertj.core.api.Assertions.assertThat;

class InMemoryRateLimiterTest {

    private final AtomicLong now = new AtomicLong(1_000_000);
    private final InMemoryRateLimiter limiter = new InMemoryRateLimiter(now::get);

    @Test
    void allowsUpToTheLimitThenBlocksWithRetryAfter() {
        for (int i = 0; i < 3; i++) {
            assertThat(limiter.tryAcquire("login|1.1.1.1", 3, 60_000)).isZero();
        }

        long retry = limiter.tryAcquire("login|1.1.1.1", 3, 60_000);

        assertThat(retry).isEqualTo(60);
    }

    @Test
    void retryAfterShrinksAsTheWindowRuns() {
        for (int i = 0; i < 2; i++) limiter.tryAcquire("k", 2, 60_000);
        now.addAndGet(45_000);

        assertThat(limiter.tryAcquire("k", 2, 60_000)).isEqualTo(15);
    }

    @Test
    void windowResetsAfterItExpires() {
        for (int i = 0; i < 2; i++) limiter.tryAcquire("k", 2, 60_000);
        assertThat(limiter.tryAcquire("k", 2, 60_000)).isPositive();

        now.addAndGet(60_000);

        assertThat(limiter.tryAcquire("k", 2, 60_000)).isZero();
    }

    @Test
    void keysAreIndependent() {
        limiter.tryAcquire("a", 1, 60_000);

        assertThat(limiter.tryAcquire("a", 1, 60_000)).isPositive();
        assertThat(limiter.tryAcquire("b", 1, 60_000)).isZero();
    }

    @Test
    void expiredWindowsArePurgedSoMemoryStaysBounded() {
        for (int i = 0; i < 10_001; i++) limiter.tryAcquire("ip-" + i, 5, 1_000);
        assertThat(limiter.size()).isGreaterThan(10_000);

        now.addAndGet(5_000);
        limiter.tryAcquire("fresh", 5, 1_000);

        assertThat(limiter.size()).isLessThan(10);
    }
}
