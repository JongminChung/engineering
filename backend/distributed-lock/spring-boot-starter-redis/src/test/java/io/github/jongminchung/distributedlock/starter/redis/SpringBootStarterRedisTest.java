package io.github.jongminchung.distributedlock.starter.redis;

import java.lang.reflect.Proxy;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.redisson.api.RedissonClient;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import io.github.jongminchung.distributedlock.autoconfigure.configuration.DistributedLockAutoConfiguration;
import io.github.jongminchung.distributedlock.autoconfigure.configuration.RedisLockAutoConfiguration;
import io.github.jongminchung.distributedlock.core.api.DistributedLock;
import io.github.jongminchung.distributedlock.provider.redis.redisson.RedissonDistributedLock;

class SpringBootStarterRedisTest {
    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withConfiguration(
                    AutoConfigurations.of(DistributedLockAutoConfiguration.class, RedisLockAutoConfiguration.class));

    @Nested
    class RedisProvider {
        @Test
        void createsRedissonDistributedLock() {
            contextRunner
                    .withBean(RedissonClient.class, SpringBootStarterRedisTest::redissonClient)
                    .run(context -> {
                        DistributedLock distributedLock = context.getBean(DistributedLock.class);
                        Assertions.assertThat(distributedLock).isInstanceOf(RedissonDistributedLock.class);
                    });
        }
    }

    private static RedissonClient redissonClient() {
        return (RedissonClient) Proxy.newProxyInstance(
                RedissonClient.class.getClassLoader(), new Class[] {RedissonClient.class}, (proxy, _, args) -> null);
    }
}
