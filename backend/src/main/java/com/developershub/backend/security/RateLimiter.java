package com.developershub.backend.security;

import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * In-memory sliding-window rate limiter for the authentication endpoints.
 * All access to a client's window happens under a lock on that window, and
 * the number of tracked clients is bounded so the map cannot grow unbounded.
 */
@Component
public class RateLimiter {

    private static final int MAX_TRACKED_CLIENTS = 50_000;

    private final Map<String, Deque<Instant>> windows = new ConcurrentHashMap<>();

    public boolean tryAcquire(String key, int maxRequests, Duration window) {
        Instant now = Instant.now();
        Deque<Instant> hits = windows.computeIfAbsent(key, k -> new ArrayDeque<>());
        synchronized (hits) {
            Instant cutoff = now.minus(window);
            while (!hits.isEmpty() && hits.peekFirst().isBefore(cutoff)) {
                hits.pollFirst();
            }
            if (hits.size() >= maxRequests) {
                return false;
            }
            hits.addLast(now);
            return true;
        }
    }

    /** Drops windows that are past the given window length; keeps the map bounded. */
    public void evictStale(Duration window) {
        if (windows.size() <= MAX_TRACKED_CLIENTS) {
            return;
        }
        Instant cutoff = Instant.now().minus(window);
        windows.entrySet().removeIf(entry -> {
            synchronized (entry.getValue()) {
                Deque<Instant> hits = entry.getValue();
                while (!hits.isEmpty() && hits.peekFirst().isBefore(cutoff)) {
                    hits.pollFirst();
                }
                return hits.isEmpty();
            }
        });
    }

    /** Used by tests to start every test from a clean slate. */
    public void reset() {
        windows.clear();
    }
}
