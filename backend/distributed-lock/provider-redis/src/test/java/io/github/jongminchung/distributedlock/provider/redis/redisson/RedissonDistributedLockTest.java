package io.github.jongminchung.distributedlock.provider.redis.redisson;

import java.time.Duration;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.utility.DockerImageName;

import io.github.jongminchung.distributedlock.core.api.LockHandle;
import io.github.jongminchung.distributedlock.core.api.LockRequest;
import io.github.jongminchung.distributedlock.core.key.LockKey;
import io.github.jongminchung.distributedlock.provider.redis.config.RedisLockProviderConfig;

class RedissonDistributedLockTest {
    private static final GenericContainer<?> REDIS =
            new GenericContainer(DockerImageName.parse("redis:7.2-alpine")).withExposedPorts(6379);

    @BeforeAll
    static void startRedis() {
        REDIS.start();
    }

    @AfterAll
    static void stopRedis() {
        REDIS.stop();
    }

    @Test
    void acquiresAndReleasesLock() throws ExecutionException, InterruptedException {
        RedisLockProviderConfig config = new RedisLockProviderConfig();
        config.setDefaultWaitTime(Duration.ofSeconds(1));
        config.setDefaultLeaseTime(Duration.ofSeconds(2));
        RedissonClient client = createClient();
        ExecutorService executor = Executors.newSingleThreadExecutor();
        try {
            RedissonDistributedLock lock = new RedissonDistributedLock(client, config);
            LockRequest request =
                    LockRequest.of(LockKey.of("order:1"), Duration.ofSeconds(1), Duration.ofSeconds(2), false);
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
            client.shutdown();
        }
    }

    private RedissonClient createClient() {
        Config config = new Config();
        config.useSingleServer().setAddress("redis://" + REDIS.getHost() + ":" + REDIS.getMappedPort(6379));
        return Redisson.create(config);
    }
}
