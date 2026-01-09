package io.github.jongminchung.distributedlock.core.key;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

class SimpleLockKeyStrategyTest {
    @Test
    void createsKeyFromRawValue() {
        SimpleLockKeyStrategy strategy = new SimpleLockKeyStrategy();

        LockKey key = strategy.create("inventory:42");

        Assertions.assertThat(key.value()).isEqualTo("inventory:42");
    }
}
