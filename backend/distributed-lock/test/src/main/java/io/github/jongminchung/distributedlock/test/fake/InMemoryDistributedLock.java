package io.github.jongminchung.distributedlock.test.fake;

import java.time.Duration;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

import io.github.jongminchung.distributedlock.core.api.DistributedLock;
import io.github.jongminchung.distributedlock.core.api.LockHandle;
import io.github.jongminchung.distributedlock.core.api.LockRequest;
import io.github.jongminchung.distributedlock.core.exception.LockTimeoutException;
import io.github.jongminchung.distributedlock.core.key.LockKey;

public class InMemoryDistributedLock implements DistributedLock {
    private final ConcurrentHashMap<String, ReentrantLock> locks = new ConcurrentHashMap<>();

    @Override
    public LockHandle acquire(LockRequest request) {
        LockKey key = request.key();
        ReentrantLock lock = locks.computeIfAbsent(key.value(), ignored -> new ReentrantLock(request.fair()));
        Duration waitTime = request.waitTime();

        boolean acquired;
        try {
            if (waitTime.isZero() || waitTime.isNegative()) {
                acquired = lock.tryLock();
            } else {
                acquired = lock.tryLock(waitTime.toMillis(), TimeUnit.MILLISECONDS);
            }
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new LockTimeoutException("Interrupted while waiting for lock: " + key.value(), ex);
        }

        if (!acquired) {
            throw new LockTimeoutException("Failed to acquire lock within wait time: " + key.value());
        }

        return new InMemoryLockHandle(key, lock);
    }

    @Override
    public Optional<LockHandle> tryAcquire(LockRequest request) {
        LockKey key = request.key();
        ReentrantLock lock = locks.computeIfAbsent(key.value(), ignored -> new ReentrantLock(request.fair()));
        if (!lock.tryLock()) {
            return Optional.empty();
        }
        return Optional.of(new InMemoryLockHandle(key, lock));
    }

    private static class InMemoryLockHandle implements LockHandle {
        private final LockKey key;
        private final ReentrantLock lock;

        private InMemoryLockHandle(LockKey key, ReentrantLock lock) {
            this.key = key;
            this.lock = lock;
        }

        @Override
        public LockKey key() {
            return key;
        }

        @Override
        public void release() {
            if (lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
    }
}
