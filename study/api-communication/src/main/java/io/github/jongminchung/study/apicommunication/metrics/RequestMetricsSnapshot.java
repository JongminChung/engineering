package io.github.jongminchung.study.apicommunication.metrics;

import java.time.Duration;
import java.time.Instant;

public record RequestMetricsSnapshot(long successfulRequests,
                                     long failedRequests,
                                     long cacheHits,
                                     long cacheMisses,
                                     Duration averageLatency,
                                     Instant lastUpdated) {
}
