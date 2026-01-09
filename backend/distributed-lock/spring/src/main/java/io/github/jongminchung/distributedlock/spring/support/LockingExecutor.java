package io.github.jongminchung.distributedlock.spring.support;

import java.util.function.Supplier;

import io.github.jongminchung.distributedlock.core.api.LockRequest;

public interface LockingExecutor {
    <T> T execute(LockRequest request, Supplier<T> task);

    void execute(LockRequest request, Runnable task);
}
