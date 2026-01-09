package io.github.jongminchung.distributedlock.core.api;

import java.time.Duration;
import java.util.Objects;

import io.github.jongminchung.distributedlock.core.key.LockKey;
import io.github.jongminchung.distributedlock.core.policy.LeasePolicy;
import io.github.jongminchung.distributedlock.core.policy.RetryPolicy;

public final class LockRequest {
    private final LockKey key;
    private final Duration waitTime;
    private final Duration leaseTime;
    private final boolean fair;
    private final RetryPolicy retryPolicy;
    private final LeasePolicy leasePolicy;

    private LockRequest(
            LockKey key,
            Duration waitTime,
            Duration leaseTime,
            boolean fair,
            RetryPolicy retryPolicy,
            LeasePolicy leasePolicy) {
        this.key = Objects.requireNonNull(key, "key");
        this.waitTime = Objects.requireNonNull(waitTime, "waitTime");
        this.leaseTime = Objects.requireNonNull(leaseTime, "leaseTime");
        this.fair = fair;
        this.retryPolicy = retryPolicy;
        this.leasePolicy = leasePolicy;
    }

    public static LockRequest of(LockKey key) {
        return new LockRequest(key, Duration.ZERO, Duration.ZERO, false, null, null);
    }

    public static LockRequest of(LockKey key, Duration waitTime, Duration leaseTime, boolean fair) {
        return new LockRequest(key, waitTime, leaseTime, fair, null, null);
    }

    public LockKey key() {
        return key;
    }

    public Duration waitTime() {
        return waitTime;
    }

    public Duration leaseTime() {
        return leaseTime;
    }

    public boolean fair() {
        return fair;
    }

    public RetryPolicy retryPolicy() {
        return retryPolicy;
    }

    public LeasePolicy leasePolicy() {
        return leasePolicy;
    }
}
