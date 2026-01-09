package io.github.jongminchung.distributedlock.core.key;

public interface LockKeyStrategy {
    LockKey create(String rawKey);
}
