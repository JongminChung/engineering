package io.github.jongminchung.distributedlock.spring.support;

import java.util.Objects;
import java.util.function.Supplier;

import io.github.jongminchung.distributedlock.core.api.DistributedLock;
import io.github.jongminchung.distributedlock.core.api.LockHandle;
import io.github.jongminchung.distributedlock.core.api.LockRequest;

public class LockTemplate implements LockingExecutor {
    private final DistributedLock distributedLock;

    public LockTemplate(DistributedLock distributedLock) {
        this.distributedLock = Objects.requireNonNull(distributedLock, "distributedLock");
    }

    @Override
    public <T> T execute(LockRequest request, Supplier<T> task) {
        LockHandle handle = distributedLock.acquire(request);
        try {
            return task.get();
        } finally {
            handle.release();
        }
    }

    @Override
    public void execute(LockRequest request, Runnable task) {
        execute(request, () -> {
            task.run();
            return null;
        });
    }
}
