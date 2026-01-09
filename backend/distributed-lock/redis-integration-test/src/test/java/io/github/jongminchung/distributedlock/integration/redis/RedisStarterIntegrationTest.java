package io.github.jongminchung.distributedlock.integration.redis;

import java.time.Duration;
import java.util.Optional;
import java.util.concurrent.*;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.testcontainers.containers.GenericContainer;

import io.github.jongminchung.distributedlock.autoconfigure.configuration.DistributedLockAutoConfiguration;
import io.github.jongminchung.distributedlock.autoconfigure.configuration.RedisLockAutoConfiguration;
import io.github.jongminchung.distributedlock.core.api.DistributedLock;
import io.github.jongminchung.distributedlock.core.api.LockHandle;
import io.github.jongminchung.distributedlock.core.api.LockRequest;
import io.github.jongminchung.distributedlock.core.exception.LockAcquisitionException;
import io.github.jongminchung.distributedlock.core.exception.LockTimeoutException;
import io.github.jongminchung.distributedlock.core.key.LockKey;
import io.github.jongminchung.distributedlock.test.containers.RedisContainerSupport;

class RedisStarterIntegrationTest {
    private static final GenericContainer<?> REDIS = RedisContainerSupport.createRedis();

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withConfiguration(
                    AutoConfigurations.of(DistributedLockAutoConfiguration.class, RedisLockAutoConfiguration.class))
            .withBean(RedissonClient.class, RedisStarterIntegrationTest::createClient);

    @BeforeAll
    static void startRedis() {
        REDIS.start();
    }

    @AfterAll
    static void stopRedis() {
        REDIS.stop();
    }

    @Nested
    class WhenRedisIsAvailable {
        @Test
        void acquiresAndReacquiresLock() {
            contextRunner.run(context -> {
                RedissonClient client = context.getBean(RedissonClient.class);
                ExecutorService executor = Executors.newSingleThreadExecutor();
                try {
                    DistributedLock distributedLock = context.getBean(DistributedLock.class);
                    LockRequest request = LockRequest.of(
                            LockKey.of("integration:1"), Duration.ofSeconds(1), Duration.ofSeconds(2), false);

                    LockHandle handle = distributedLock.acquire(request);
                    Optional<LockHandle> secondAttempt = executor.submit(() -> distributedLock.tryAcquire(request))
                            .get();
                    Assertions.assertThat(secondAttempt).isEmpty();

                    handle.release();
                    Optional<LockHandle> reacquired = distributedLock.tryAcquire(request);
                    Assertions.assertThat(reacquired).isPresent();
                    reacquired.get().release();
                } finally {
                    executor.shutdownNow();
                    client.shutdown();
                }
            });
        }

        @Test
        void timesOutWhenLockHeld() {
            contextRunner.run(context -> {
                RedissonClient client = context.getBean(RedissonClient.class);
                ExecutorService executor = Executors.newSingleThreadExecutor();
                try {
                    DistributedLock distributedLock = context.getBean(DistributedLock.class);
                    CountDownLatch entered = new CountDownLatch(1);
                    CountDownLatch release = new CountDownLatch(1);
                    LockRequest holderRequest = LockRequest.of(
                            LockKey.of("integration:timeout"), Duration.ofSeconds(1), Duration.ofSeconds(5), false);

                    Future<?> holder = executor.submit(() -> {
                        LockHandle handle = distributedLock.acquire(holderRequest);
                        entered.countDown();
                        try {
                            release.await();
                        } catch (InterruptedException _) {
                            Thread.currentThread().interrupt();
                        } finally {
                            handle.release();
                        }
                    });

                    entered.await(2, TimeUnit.SECONDS);
                    LockRequest timeoutRequest = LockRequest.of(
                            LockKey.of("integration:timeout"), Duration.ofMillis(200), Duration.ofSeconds(1), false);

                    Assertions.assertThatThrownBy(() -> distributedLock.acquire(timeoutRequest))
                            .isInstanceOfAny(LockTimeoutException.class, LockAcquisitionException.class);

                    release.countDown();
                    holder.get(2, TimeUnit.SECONDS);
                } finally {
                    executor.shutdownNow();
                    client.shutdown();
                }
            });
        }

        @Test
        void reacquiresAfterLeaseExpiry() {
            contextRunner.run(context -> {
                RedissonClient client = context.getBean(RedissonClient.class);
                ExecutorService executor = Executors.newSingleThreadExecutor();
                try {
                    DistributedLock distributedLock = context.getBean(DistributedLock.class);
                    LockRequest request = LockRequest.of(
                            LockKey.of("integration:lease"), Duration.ofSeconds(1), Duration.ofMillis(200), false);

                    LockHandle handle = distributedLock.acquire(request);
                    Thread.sleep(400);

                    Optional<LockHandle> reacquired = executor.submit(() -> distributedLock.tryAcquire(request))
                            .get();
                    Assertions.assertThat(reacquired).isPresent();
                    reacquired.get().release();
                    handle.release();
                } finally {
                    executor.shutdownNow();
                    client.shutdown();
                }
            });
        }
    }

    private static RedissonClient createClient() {
        Config config = new Config();
        config.useSingleServer().setAddress("redis://" + REDIS.getHost() + ":" + REDIS.getMappedPort(6379));
        return Redisson.create(config);
    }
}
