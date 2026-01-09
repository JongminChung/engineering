package io.github.jongminchung.distributedlock.core.key;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

class LockKeyTest {
    @Test
    void createsLockKeyWithValue() {
        LockKey key = LockKey.of("order:1");

        Assertions.assertThat(key.value()).isEqualTo("order:1");
        Assertions.assertThat(key).hasToString("order:1");
    }
}
