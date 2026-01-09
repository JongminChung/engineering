package io.github.jongminchung.distributedlock.autoconfigure.configuration;

import java.lang.reflect.Proxy;
import javax.sql.DataSource;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.redisson.api.RedissonClient;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import io.github.jongminchung.distributedlock.core.api.DistributedLock;
import io.github.jongminchung.distributedlock.provider.jdbc.impl.JdbcDistributedLock;
import io.github.jongminchung.distributedlock.provider.redis.redisson.RedissonDistributedLock;

class DistributedLockAutoConfigurationTest {
    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(
                    DistributedLockAutoConfiguration.class,
                    RedisLockAutoConfiguration.class,
                    JdbcLockAutoConfiguration.class));

    @Test
    void configuresRedisLockWhenRedissonClientProvided() {
        contextRunner
                .withPropertyValues("distributed-lock.provider=redis")
                .withBean(RedissonClient.class, DistributedLockAutoConfigurationTest::redissonClient)
                .run(context -> {
                    DistributedLock distributedLock = context.getBean(DistributedLock.class);
                    Assertions.assertThat(distributedLock).isInstanceOf(RedissonDistributedLock.class);
                });
    }

    @Test
    void configuresJdbcLockWhenDataSourceProvided() {
        contextRunner
                .withPropertyValues("distributed-lock.provider=jdbc")
                .withBean(DataSource.class, DistributedLockAutoConfigurationTest::dataSource)
                .run(context -> {
                    DistributedLock distributedLock = context.getBean(DistributedLock.class);
                    Assertions.assertThat(distributedLock).isInstanceOf(JdbcDistributedLock.class);
                });
    }

    private static RedissonClient redissonClient() {
        return (RedissonClient) Proxy.newProxyInstance(
                RedissonClient.class.getClassLoader(), new Class[] {RedissonClient.class}, (proxy, _, args) -> null);
    }

    private static DataSource dataSource() {
        return (DataSource) Proxy.newProxyInstance(
                DataSource.class.getClassLoader(), new Class[] {DataSource.class}, (proxy, _, args) -> null);
    }
}
