package io.github.jongminchung.distributedlock.core.key;

import java.util.Objects;

public final class LockKey {
    private final String value;

    private LockKey(String value) {
        this.value = Objects.requireNonNull(value, "value");
    }

    public static LockKey of(String value) {
        return new LockKey(value);
    }

    public String value() {
        return value;
    }

    @Override
    public String toString() {
        return value;
    }
}
