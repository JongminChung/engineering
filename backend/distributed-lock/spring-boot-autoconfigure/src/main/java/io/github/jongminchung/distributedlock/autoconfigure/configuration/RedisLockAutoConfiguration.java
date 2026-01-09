package io.github.jongminchung.distributedlock.autoconfigure.configuration;

import org.redisson.api.RedissonClient;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;

import io.github.jongminchung.distributedlock.autoconfigure.properties.DistributedLockProperties;
import io.github.jongminchung.distributedlock.core.api.DistributedLock;
import io.github.jongminchung.distributedlock.provider.redis.config.RedisLockProviderConfig;
import io.github.jongminchung.distributedlock.provider.redis.redisson.RedissonDistributedLock;

@AutoConfiguration
@ConditionalOnClass(RedissonClient.class)
@ConditionalOnProperty(prefix = "distributed-lock", name = "provider", havingValue = "redis", matchIfMissing = true)
public class RedisLockAutoConfiguration {
    @Bean
    @ConditionalOnMissingBean
    public RedisLockProviderConfig redisLockProviderConfig(DistributedLockProperties properties) {
        RedisLockProviderConfig config = new RedisLockProviderConfig();
        config.setKeyPrefix(properties.redis().keyPrefix());
        config.setDefaultWaitTime(properties.waitTime());
        config.setDefaultLeaseTime(properties.leaseTime());
        return config;
    }

    @Bean
    @ConditionalOnMissingBean(DistributedLock.class)
    @ConditionalOnBean(RedissonClient.class)
    public DistributedLock redisDistributedLock(RedissonClient redissonClient, RedisLockProviderConfig config) {
        return new RedissonDistributedLock(redissonClient, config);
    }
}
