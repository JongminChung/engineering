package io.github.jongminchung.distributedlock.core.policy;

import java.time.Duration;

public interface RetryPolicy {
    boolean shouldRetry(int attempt, Duration elapsedTime);

    Duration backoffDelay(int attempt);
}
