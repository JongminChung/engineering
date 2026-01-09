package io.github.jongminchung.distributedlock.core.key;

import java.util.Objects;

public class SimpleLockKeyStrategy implements LockKeyStrategy {
    @Override
    public LockKey create(String rawKey) {
        return LockKey.of(Objects.requireNonNull(rawKey, "rawKey"));
    }
}
