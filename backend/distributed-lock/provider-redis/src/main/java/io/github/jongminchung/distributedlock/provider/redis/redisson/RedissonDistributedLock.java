package io.github.jongminchung.distributedlock.provider.redis.redisson;

import java.time.Duration;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.redisson.client.RedisTimeoutException;

import io.github.jongminchung.distributedlock.core.api.DistributedLock;
import io.github.jongminchung.distributedlock.core.api.LockHandle;
import io.github.jongminchung.distributedlock.core.api.LockRequest;
import io.github.jongminchung.distributedlock.core.exception.LockAcquisitionException;
import io.github.jongminchung.distributedlock.core.exception.LockReleaseException;
import io.github.jongminchung.distributedlock.core.exception.LockTimeoutException;
import io.github.jongminchung.distributedlock.core.key.LockKey;
import io.github.jongminchung.distributedlock.provider.redis.config.RedisLockProviderConfig;

public class RedissonDistributedLock implements DistributedLock {
    private final RedissonClient redissonClient;
    private final RedisLockProviderConfig config;

    public RedissonDistributedLock(RedissonClient redissonClient, RedisLockProviderConfig config) {
        this.redissonClient = Objects.requireNonNull(redissonClient, "redissonClient");
        this.config = Objects.requireNonNull(config, "config");
    }

    @Override
    public LockHandle acquire(LockRequest request) {
        RLock lock = obtainLock(request);
        Duration waitTime = resolveWaitTime(request);
        Duration leaseTime = resolveLeaseTime(request);
        try {
            boolean acquired = lock.tryLock(
                    Math.max(waitTime.toMillis(), 0), Math.max(leaseTime.toMillis(), 0), TimeUnit.MILLISECONDS);
            if (!acquired) {
                throw new LockTimeoutException("Failed to acquire lock within wait time: "
                        + request.key().value());
            }
            return new RedissonLockHandle(request.key(), lock);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new LockAcquisitionException(
                    "Interrupted while acquiring lock: " + request.key().value(), ex);
        } catch (RuntimeException ex) {
            if (isTimeoutException(ex)) {
                throw new LockTimeoutException(
                        "Failed to acquire lock within wait time: "
                                + request.key().value(),
                        ex);
            }
            throw new LockAcquisitionException(
                    "Failed to acquire lock: " + request.key().value(), ex);
        }
    }

    @Override
    public Optional<LockHandle> tryAcquire(LockRequest request) {
        RLock lock = obtainLock(request);
        Duration leaseTime = resolveLeaseTime(request);
        boolean acquired;
        try {
            if (!leaseTime.isZero() && !leaseTime.isNegative()) {
                acquired = lock.tryLock(0, leaseTime.toMillis(), TimeUnit.MILLISECONDS);
            } else {
                acquired = lock.tryLock();
            }
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new LockAcquisitionException(
                    "Interrupted while acquiring lock: " + request.key().value(), ex);
        }
        if (!acquired) {
            return Optional.empty();
        }
        return Optional.of(new RedissonLockHandle(request.key(), lock));
    }

    private boolean isTimeoutException(RuntimeException ex) {
        if (ex instanceof RedisTimeoutException) {
            return true;
        }
        Throwable cause = ex.getCause();
        return cause instanceof RedisTimeoutException;
    }

    private RLock obtainLock(LockRequest request) {
        String key = config.keyPrefix() + request.key().value();
        if (request.fair()) {
            return redissonClient.getFairLock(key);
        }
        return redissonClient.getLock(key);
    }

    private Duration resolveWaitTime(LockRequest request) {
        if (request.waitTime() == null || request.waitTime().isNegative()) {
            return config.defaultWaitTime();
        }
        if (request.waitTime().isZero()) {
            return config.defaultWaitTime();
        }
        return request.waitTime();
    }

    private Duration resolveLeaseTime(LockRequest request) {
        if (request.leaseTime() == null || request.leaseTime().isNegative()) {
            return config.defaultLeaseTime();
        }
        if (request.leaseTime().isZero()) {
            return config.defaultLeaseTime();
        }
        return request.leaseTime();
    }

    private static class RedissonLockHandle implements LockHandle {
        private final LockKey key;
        private final RLock lock;

        private RedissonLockHandle(LockKey key, RLock lock) {
            this.key = key;
            this.lock = lock;
        }

        @Override
        public LockKey key() {
            return key;
        }

        @Override
        public void release() {
            try {
                if (lock.isHeldByCurrentThread()) {
                    lock.unlock();
                }
            } catch (RuntimeException ex) {
                throw new LockReleaseException("Failed to release lock: " + key.value(), ex);
            }
        }
    }
}
