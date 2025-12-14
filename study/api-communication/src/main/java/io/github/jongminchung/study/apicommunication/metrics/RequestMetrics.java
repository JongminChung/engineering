package io.github.jongminchung.study.apicommunication.metrics;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.atomic.LongAdder;

public class RequestMetrics {

    private final Clock clock;
    private final LongAdder successfulRequests = new LongAdder();
    private final LongAdder failedRequests = new LongAdder();
    private final LongAdder cacheHits = new LongAdder();
    private final LongAdder cacheMisses = new LongAdder();
    private final LongAdder totalLatencyNanos = new LongAdder();
    private final AtomicReference<Instant> lastUpdated = new AtomicReference<>(Instant.EPOCH);

    public RequestMetrics(Clock clock) {
        this.clock = clock;
    }

    public void recordSuccess(Duration latency) {
        successfulRequests.increment();
        totalLatencyNanos.add(latency.toNanos());
        lastUpdated.set(clock.instant());
    }

    public void recordFailure(Duration latency) {
        failedRequests.increment();
        totalLatencyNanos.add(latency.toNanos());
        lastUpdated.set(clock.instant());
    }

    public void recordCacheHit() {
        cacheHits.increment();
        lastUpdated.set(clock.instant());
    }

    public void recordCacheMiss() {
        cacheMisses.increment();
        lastUpdated.set(clock.instant());
    }

    public RequestMetricsSnapshot snapshot() {
        long success = successfulRequests.sum();
        long failure = failedRequests.sum();
        long totalSamples = success + failure;
        Duration averageLatency = totalSamples == 0
            ? Duration.ZERO
            : Duration.ofNanos(totalLatencyNanos.sum() / totalSamples);
        return new RequestMetricsSnapshot(success, failure, cacheHits.sum(), cacheMisses.sum(), averageLatency, lastUpdated.get());
    }
}
