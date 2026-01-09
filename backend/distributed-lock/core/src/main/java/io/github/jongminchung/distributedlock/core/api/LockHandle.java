package io.github.jongminchung.distributedlock.core.api;

import io.github.jongminchung.distributedlock.core.key.LockKey;

public interface LockHandle {
    LockKey key();

    void release();
}
