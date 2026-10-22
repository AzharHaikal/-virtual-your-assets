package com.vyra.be_virtual_your_assets.ratelimit;

/** Counts requests per key inside a fixed window. */
public interface RateLimiter {

    /**
     * Counts one request.
     *
     * @return 0 when the request is allowed, otherwise the seconds until the window resets
     */
    long tryAcquire(String key, int limit, long windowMillis);
}
