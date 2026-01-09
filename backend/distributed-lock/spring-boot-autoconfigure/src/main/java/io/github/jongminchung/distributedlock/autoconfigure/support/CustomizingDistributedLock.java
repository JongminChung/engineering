package io.github.jongminchung.distributedlock.autoconfigure.support;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

import io.github.jongminchung.distributedlock.autoconfigure.customizer.LockCustomizer;
import io.github.jongminchung.distributedlock.core.api.DistributedLock;
import io.github.jongminchung.distributedlock.core.api.LockHandle;
import io.github.jongminchung.distributedlock.core.api.LockRequest;

public class CustomizingDistributedLock implements DistributedLock {
    private final DistributedLock delegate;
    private final List<LockCustomizer> customizers;

    public CustomizingDistributedLock(DistributedLock delegate, List<LockCustomizer> customizers) {
        this.delegate = Objects.requireNonNull(delegate, "delegate");
        this.customizers = List.copyOf(customizers);
    }

    @Override
    public LockHandle acquire(LockRequest request) {
        return delegate.acquire(applyCustomizers(request));
    }

    @Override
    public Optional<LockHandle> tryAcquire(LockRequest request) {
        return delegate.tryAcquire(applyCustomizers(request));
    }

    private LockRequest applyCustomizers(LockRequest request) {
        LockRequest current = Objects.requireNonNull(request, "request");
        for (LockCustomizer customizer : customizers) {
            current = Objects.requireNonNull(customizer.customize(current), "customized request");
        }
        return current;
    }
}
