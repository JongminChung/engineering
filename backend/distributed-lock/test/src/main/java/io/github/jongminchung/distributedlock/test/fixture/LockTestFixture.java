package io.github.jongminchung.distributedlock.test.fixture;

import java.time.Duration;

import io.github.jongminchung.distributedlock.core.api.LockRequest;
import io.github.jongminchung.distributedlock.core.key.LockKey;

public final class LockTestFixture {
    private LockTestFixture() {}

    public static LockRequest defaultRequest(String key) {
        return LockRequest.of(LockKey.of(key), Duration.ofSeconds(1), Duration.ofSeconds(5), false);
    }
}
