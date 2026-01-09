package io.github.jongminchung.distributedlock.autoconfigure.customizer;

import java.time.Duration;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;

import io.github.jongminchung.distributedlock.autoconfigure.configuration.DistributedLockAutoConfiguration;
import io.github.jongminchung.distributedlock.core.api.DistributedLock;
import io.github.jongminchung.distributedlock.core.api.LockHandle;
import io.github.jongminchung.distributedlock.core.api.LockRequest;
import io.github.jongminchung.distributedlock.core.key.LockKey;

class LockCustomizerAutoConfigurationTest {
    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(DistributedLockAutoConfiguration.class))
            .withUserConfiguration(TestConfig.class);

    @Nested
    class WhenCustomizerRegistered {
        @Test
        void appliesCustomizerToRequest() {
            contextRunner.run(context -> {
                DistributedLock distributedLock = context.getBean(DistributedLock.class);
                RecordingDistributedLock delegate = context.getBean(RecordingDistributedLock.class);

                LockRequest request =
                        LockRequest.of(LockKey.of("base"), Duration.ofSeconds(1), Duration.ofSeconds(2), false);

                distributedLock.tryAcquire(request);

                Assertions.assertThat(delegate.lastRequest())
                        .isPresent()
                        .get()
                        .extracting(value -> value.key().value())
                        .isEqualTo("base:customized");
            });
        }
    }

    @Nested
    class WhenMultipleCustomizersRegistered {
        private final ApplicationContextRunner chainedRunner = new ApplicationContextRunner()
                .withConfiguration(AutoConfigurations.of(DistributedLockAutoConfiguration.class))
                .withUserConfiguration(ChainedConfig.class);

        @Test
        void appliesCustomizersInOrder() {
            chainedRunner.run(context -> {
                DistributedLock distributedLock = context.getBean(DistributedLock.class);
                RecordingDistributedLock delegate = context.getBean(RecordingDistributedLock.class);

                LockRequest request =
                        LockRequest.of(LockKey.of("base"), Duration.ofSeconds(1), Duration.ofSeconds(2), false);

                distributedLock.tryAcquire(request);

                Assertions.assertThat(delegate.lastRequest())
                        .isPresent()
                        .get()
                        .extracting(value -> value.key().value())
                        .isEqualTo("base:first:second");
            });
        }
    }

    @Configuration
    static class TestConfig {
        private final RecordingDistributedLock recordingLock = new RecordingDistributedLock();

        @Bean
        public RecordingDistributedLock recordingDistributedLock() {
            return recordingLock;
        }

        @Bean
        public LockCustomizer lockCustomizer() {
            return request -> LockRequest.of(
                    LockKey.of(request.key().value() + ":customized"),
                    request.waitTime(),
                    request.leaseTime(),
                    request.fair());
        }
    }

    @Configuration
    static class ChainedConfig {
        private final RecordingDistributedLock recordingLock = new RecordingDistributedLock();

        @Bean
        public RecordingDistributedLock recordingDistributedLock() {
            return recordingLock;
        }

        @Bean
        @Order(1)
        public LockCustomizer firstCustomizer() {
            return request -> LockRequest.of(
                    LockKey.of(request.key().value() + ":first"),
                    request.waitTime(),
                    request.leaseTime(),
                    request.fair());
        }

        @Bean
        @Order(2)
        public LockCustomizer secondCustomizer() {
            return request -> LockRequest.of(
                    LockKey.of(request.key().value() + ":second"),
                    request.waitTime(),
                    request.leaseTime(),
                    request.fair());
        }
    }

    static class RecordingDistributedLock implements DistributedLock {
        private final AtomicReference<LockRequest> lastRequest = new AtomicReference<>();

        Optional<LockRequest> lastRequest() {
            return Optional.ofNullable(lastRequest.get());
        }

        @Override
        public LockHandle acquire(LockRequest request) {
            lastRequest.set(request);
            return new LockHandle() {
                @Override
                public LockKey key() {
                    return request.key();
                }

                @Override
                public void release() {
                    // no-op
                }
            };
        }

        @Override
        public Optional<LockHandle> tryAcquire(LockRequest request) {
            lastRequest.set(request);
            return Optional.empty();
        }
    }
}
