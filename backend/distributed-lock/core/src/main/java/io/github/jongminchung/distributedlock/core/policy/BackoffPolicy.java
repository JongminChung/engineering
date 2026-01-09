package io.github.jongminchung.distributedlock.core.policy;

import java.time.Duration;

public interface BackoffPolicy {
    Duration delayFor(int attempt);
}
