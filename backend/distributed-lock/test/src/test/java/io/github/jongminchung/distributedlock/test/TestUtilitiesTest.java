package io.github.jongminchung.distributedlock.test;

import java.time.Duration;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.GenericContainer;

import io.github.jongminchung.distributedlock.core.api.LockHandle;
import io.github.jongminchung.distributedlock.core.api.LockRequest;
import io.github.jongminchung.distributedlock.core.key.LockKey;
import io.github.jongminchung.distributedlock.test.assertion.LockAssertions;
import io.github.jongminchung.distributedlock.test.containers.RedisContainerSupport;
import io.github.jongminchung.distributedlock.test.fake.InMemoryDistributedLock;
import io.github.jongminchung.distributedlock.test.fixture.LockTestFixture;

class TestUtilitiesTest {
    @Nested
    class InMemoryLock {
        @Test
        void acquiresAndReleasesLock() throws ExecutionException, InterruptedException {
            InMemoryDistributedLock lock = new InMemoryDistributedLock();
            LockRequest request = LockTestFixture.defaultRequest("inmemory:1");
            ExecutorService executor = Executors.newSingleThreadExecutor();

            try {
                LockHandle handle = lock.acquire(request);
                Optional<LockHandle> secondAttempt =
                        executor.submit(() -> lock.tryAcquire(request)).get();

                Assertions.assertThat(secondAttempt).isEmpty();
                handle.release();

                Optional<LockHandle> reacquired = lock.tryAcquire(request);
                Assertions.assertThat(reacquired).isPresent();
                reacquired.get().release();
            } finally {
                executor.shutdownNow();
            }
        }
    }

    @Nested
    class LockAssertionsSupport {
        @Test
        void verifiesAcquiredHandle() {
            InMemoryDistributedLock lock = new InMemoryDistributedLock();
            LockRequest request = LockRequest.of(LockKey.of("assert:1"), Duration.ZERO, Duration.ZERO, false);

            Optional<LockHandle> handle = lock.tryAcquire(request);

            LockAssertions.assertAcquired(handle);
            handle.ifPresent(LockHandle::release);
        }
    }

    @Nested
    class LockTestFixtureSupport {
        @Test
        void createsDefaultRequest() {
            LockRequest request = LockTestFixture.defaultRequest("fixture:1");

            Assertions.assertThat(request.key().value()).isEqualTo("fixture:1");
            Assertions.assertThat(request.waitTime()).isEqualTo(Duration.ofSeconds(1));
            Assertions.assertThat(request.leaseTime()).isEqualTo(Duration.ofSeconds(5));
            Assertions.assertThat(request.fair()).isFalse();
        }
    }

    @Nested
    class RedisContainerSupportTest {
        @Test
        void exposesDefaultRedisPort() {
            GenericContainer<?> container = RedisContainerSupport.createRedis();

            Assertions.assertThat(container.getExposedPorts()).contains(6379);
        }
    }
}
