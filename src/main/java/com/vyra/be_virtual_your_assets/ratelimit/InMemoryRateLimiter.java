package com.vyra.be_virtual_your_assets.ratelimit;

import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.LongSupplier;

/**
 * Fixed-window request counter keyed by an arbitrary string (bucket + client address).
 * State lives in this JVM only: with several backend instances every instance counts
 * separately, so the effective limit is (limit x instances). Memory is bounded by
 * purging expired windows once the map grows.
 */
public class InMemoryRateLimiter implements RateLimiter {

    private static final int PURGE_THRESHOLD = 10_000;

    private static final class Window {
        final long start;
        int count;

        Window(long start) {
            this.start = start;
        }
    }

    private final Map<String, Window> windows = new ConcurrentHashMap<>();
    private final LongSupplier clockMillis;

    public InMemoryRateLimiter() {
        this(System::currentTimeMillis);
    }

    public InMemoryRateLimiter(LongSupplier clockMillis) {
        this.clockMillis = clockMillis;
    }

    @Override
    public long tryAcquire(String key, int limit, long windowMillis) {
        long now = clockMillis.getAsLong();
        if (windows.size() > PURGE_THRESHOLD) purge(now, windowMillis);

        long[] retry = {0};
        windows.compute(key, (k, w) -> {
            if (w == null || now - w.start >= windowMillis) {
                w = new Window(now);
            }
            if (w.count >= limit) {
                retry[0] = Math.max(1, (w.start + windowMillis - now + 999) / 1000);
            } else {
                w.count++;
            }
            return w;
        });
        return retry[0];
    }

    private void purge(long now, long windowMillis) {
        Iterator<Map.Entry<String, Window>> it = windows.entrySet().iterator();
        while (it.hasNext()) {
            if (now - it.next().getValue().start >= windowMillis) it.remove();
        }
    }

    int size() {
        return windows.size();
    }
}
