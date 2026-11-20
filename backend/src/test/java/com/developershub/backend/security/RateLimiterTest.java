package com.developershub.backend.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

class RateLimiterTest {

    /** The limiter only starts evicting once more clients than this are tracked. */
    private static final int TRACKED_CLIENT_LIMIT = 50_000;

    private RateLimiter rateLimiter;

    @BeforeEach
    void setUp() {
        rateLimiter = new RateLimiter();
    }

    @Test
    void allowsExactlyMaxRequestsWithinTheWindow() {
        Duration window = Duration.ofSeconds(30);

        for (int i = 0; i < 3; i++) {
            assertThat(rateLimiter.tryAcquire("client", 3, window)).isTrue();
        }

        assertThat(rateLimiter.tryAcquire("client", 3, window)).isFalse();
    }

    @Test
    void tracksDifferentKeysIndependently() {
        Duration window = Duration.ofSeconds(30);

        assertThat(rateLimiter.tryAcquire("client-a", 1, window)).isTrue();
        assertThat(rateLimiter.tryAcquire("client-b", 1, window)).isTrue();

        assertThat(rateLimiter.tryAcquire("client-a", 1, window)).isFalse();
        assertThat(rateLimiter.tryAcquire("client-b", 1, window)).isFalse();
    }

    @Test
    void allowsTheClientAgainAfterTheWindowElapses() throws InterruptedException {
        Duration window = Duration.ofMillis(50);

        assertThat(rateLimiter.tryAcquire("client", 1, window)).isTrue();
        assertThat(rateLimiter.tryAcquire("client", 1, window)).isFalse();

        Thread.sleep(120);

        assertThat(rateLimiter.tryAcquire("client", 1, window)).isTrue();
    }

    @Test
    void evictStaleDropsWindowsWithoutRecentHits() throws InterruptedException {
        Duration window = Duration.ofMillis(50);
        for (int i = 0; i <= TRACKED_CLIENT_LIMIT; i++) {
            rateLimiter.tryAcquire("client-" + i, 1, window);
        }
        assertThat(trackedWindows()).hasSize(TRACKED_CLIENT_LIMIT + 1);

        Thread.sleep(120);
        rateLimiter.evictStale(window);

        assertThat(trackedWindows()).isEmpty();
        // The limiter keeps working after the map has been pruned.
        assertThat(rateLimiter.tryAcquire("client-0", 1, window)).isTrue();
    }

    @Test
    void evictStaleKeepsWindowsThatStillHaveRecentHits() {
        Duration window = Duration.ofMinutes(5);
        for (int i = 0; i <= TRACKED_CLIENT_LIMIT; i++) {
            rateLimiter.tryAcquire("client-" + i, 1, window);
        }

        rateLimiter.evictStale(window);

        assertThat(trackedWindows()).hasSize(TRACKED_CLIENT_LIMIT + 1);
    }

    @Test
    void evictStaleIsANoOpWhileTheClientCountIsBelowTheBound() {
        Duration window = Duration.ofMinutes(5);
        rateLimiter.tryAcquire("client-a", 1, window);
        rateLimiter.tryAcquire("client-b", 1, window);

        rateLimiter.evictStale(window);

        assertThat(trackedWindows()).hasSize(2);
    }

    @Test
    void resetClearsAllState() {
        Duration window = Duration.ofSeconds(30);
        rateLimiter.tryAcquire("client", 1, window);
        assertThat(rateLimiter.tryAcquire("client", 1, window)).isFalse();

        rateLimiter.reset();

        assertThat(trackedWindows()).isEmpty();
        assertThat(rateLimiter.tryAcquire("client", 1, window)).isTrue();
    }

    @Test
    void neverExceedsTheLimitWhenManyThreadsHitTheSameKey() throws InterruptedException {
        int limit = 50;
        int threadCount = 16;
        int attemptsPerThread = 25;
        Duration window = Duration.ofMinutes(5);
        ExecutorService pool = Executors.newFixedThreadPool(threadCount);
        CountDownLatch start = new CountDownLatch(1);
        CountDownLatch finished = new CountDownLatch(threadCount);
        AtomicInteger allowed = new AtomicInteger();

        try {
            for (int i = 0; i < threadCount; i++) {
                pool.submit(() -> {
                    try {
                        start.await();
                        for (int attempt = 0; attempt < attemptsPerThread; attempt++) {
                            if (rateLimiter.tryAcquire("hot-key", limit, window)) {
                                allowed.incrementAndGet();
                            }
                        }
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    } finally {
                        finished.countDown();
                    }
                });
            }
            start.countDown();
            assertThat(finished.await(30, TimeUnit.SECONDS)).isTrue();
        } finally {
            pool.shutdownNow();
        }

        assertThat(allowed.get()).isEqualTo(limit);
    }

    @SuppressWarnings("unchecked")
    private Map<String, ?> trackedWindows() {
        return (Map<String, ?>) ReflectionTestUtils.getField(rateLimiter, "windows");
    }
}
