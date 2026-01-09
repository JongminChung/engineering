package io.github.jongminchung.distributedlock.autoconfigure.configuration;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

import io.github.jongminchung.distributedlock.autoconfigure.customizer.LockCustomizer;
import io.github.jongminchung.distributedlock.autoconfigure.properties.DistributedLockProperties;
import io.github.jongminchung.distributedlock.autoconfigure.support.CustomizingDistributedLock;
import io.github.jongminchung.distributedlock.core.api.DistributedLock;
import io.github.jongminchung.distributedlock.core.key.LockKeyStrategy;
import io.github.jongminchung.distributedlock.core.key.SimpleLockKeyStrategy;
import io.github.jongminchung.distributedlock.spring.aop.DistributedLockAspect;
import io.github.jongminchung.distributedlock.spring.expression.LockKeyExpressionEvaluator;
import io.github.jongminchung.distributedlock.spring.resolver.LockKeyResolver;
import io.github.jongminchung.distributedlock.spring.resolver.SimpleLockKeyResolver;
import io.github.jongminchung.distributedlock.spring.support.LockTemplate;

@AutoConfiguration
@EnableConfigurationProperties(DistributedLockProperties.class)
public class DistributedLockAutoConfiguration {
    @Bean
    @ConditionalOnMissingBean
    public LockKeyExpressionEvaluator lockKeyExpressionEvaluator() {
        return new LockKeyExpressionEvaluator();
    }

    @Bean
    @ConditionalOnMissingBean
    public LockKeyStrategy lockKeyStrategy() {
        return new SimpleLockKeyStrategy();
    }

    @Bean
    @ConditionalOnMissingBean
    public LockKeyResolver lockKeyResolver(LockKeyExpressionEvaluator evaluator, LockKeyStrategy keyStrategy) {
        return new SimpleLockKeyResolver(evaluator, keyStrategy);
    }

    @Bean
    @ConditionalOnMissingBean
    public LockTemplate lockTemplate(DistributedLock distributedLock) {
        return new LockTemplate(distributedLock);
    }

    @Bean
    @ConditionalOnMissingBean
    public DistributedLockAspect distributedLockAspect(
            DistributedLock distributedLock, LockKeyResolver lockKeyResolver) {
        return new DistributedLockAspect(distributedLock, lockKeyResolver);
    }

    @Bean
    @Primary
    @ConditionalOnBean({DistributedLock.class, LockCustomizer.class})
    public DistributedLock customizedDistributedLock(
            DistributedLock distributedLock, java.util.List<LockCustomizer> customizers) {
        return new CustomizingDistributedLock(distributedLock, customizers);
    }
}
