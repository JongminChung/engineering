package io.github.jongminchung.distributedlock.autoconfigure.customizer;

import io.github.jongminchung.distributedlock.core.api.LockRequest;

@FunctionalInterface
public interface LockCustomizer {
    LockRequest customize(LockRequest request);
}
