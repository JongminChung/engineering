package io.github.jongminchung.distributedlock.test.assertion;

import java.util.Optional;

import org.assertj.core.api.Assertions;

import io.github.jongminchung.distributedlock.core.api.LockHandle;

public final class LockAssertions {
    private LockAssertions() {}

    public static void assertAcquired(Optional<LockHandle> handle) {
        Assertions.assertThat(handle).isPresent();
    }
}
