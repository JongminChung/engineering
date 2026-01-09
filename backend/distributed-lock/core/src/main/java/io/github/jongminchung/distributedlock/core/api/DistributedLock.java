package io.github.jongminchung.distributedlock.core.api;

import java.util.Optional;

import io.github.jongminchung.distributedlock.core.exception.LockAcquisitionException;
import io.github.jongminchung.distributedlock.core.exception.LockTimeoutException;

public interface DistributedLock {
    LockHandle acquire(LockRequest request) throws LockAcquisitionException, LockTimeoutException;

    Optional<LockHandle> tryAcquire(LockRequest request);
}
