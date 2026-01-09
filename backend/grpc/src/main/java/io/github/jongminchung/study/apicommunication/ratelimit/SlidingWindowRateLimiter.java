package io.github.jongminchung.study.apicommunication.ratelimit;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Deque;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;

import io.github.jongminchung.study.apicommunication.context.ApiRequestContext;

public class SlidingWindowRateLimiter implements RateLimiter {

    private final int maxRequests;
    private final Duration window;
    private final Clock clock;
    private final Map<String, Deque<Instant>> attempts = new ConcurrentHashMap<>();

    public SlidingWindowRateLimiter(int maxRequests, Duration window, Clock clock) {
        this.maxRequests = maxRequests;
        this.window = window;
        this.clock = clock;
    }

    @Override
    public boolean tryAcquire(ApiRequestContext context, RateLimitedOperation operation) {
        String key = context.asRateLimitKey(operation.name());
        Deque<Instant> deque = attempts.computeIfAbsent(key, _ -> new ConcurrentLinkedDeque<>());
        Instant now = clock.instant();
        synchronized (deque) {
            evictExpired(now, deque);
            if (deque.size() >= maxRequests) {
                return false;
            }
            deque.addLast(now);
            return true;
        }
    }

    private void evictExpired(Instant now, Deque<Instant> deque) {
        Instant boundary = now.minus(window);
        while (true) {
            Instant head = deque.peekFirst();
            if (head == null || !head.isBefore(boundary)) {
                break;
            }
            deque.removeFirst();
        }
    }
}
