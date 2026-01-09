package io.github.jongminchung.study.infra.util;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class RetryDelayCalculatorTest {

    @Test
    void returnsBaseDelayForFirstAttempt() {
        long delay = RetryDelayCalculator.exponentialBackoffMillis(250, 0, 10_000);

        assertThat(delay).isEqualTo(250);
    }

    @Test
    void doublesDelayPerAttempt() {
        long delay = RetryDelayCalculator.exponentialBackoffMillis(250, 2, 10_000);

        assertThat(delay).isEqualTo(1000);
    }

    @Test
    void clampsDelayToMax() {
        long delay = RetryDelayCalculator.exponentialBackoffMillis(1500, 4, 3000);

        assertThat(delay).isEqualTo(3000);
    }

    @Test
    void returnsZeroWhenBaseOrMaxIsZero() {
        assertThat(RetryDelayCalculator.exponentialBackoffMillis(0, 3, 1000)).isZero();
        assertThat(RetryDelayCalculator.exponentialBackoffMillis(500, 3, 0)).isZero();
    }

    @Test
    void rejectsNegativeInputs() {
        assertThatThrownBy(() -> RetryDelayCalculator.exponentialBackoffMillis(-1, 0, 100))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> RetryDelayCalculator.exponentialBackoffMillis(100, -1, 100))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> RetryDelayCalculator.exponentialBackoffMillis(100, 0, -1))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
