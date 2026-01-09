package io.github.jongminchung.distributedlock.autoconfigure.configuration;

import javax.sql.DataSource;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;

import io.github.jongminchung.distributedlock.autoconfigure.properties.DistributedLockProperties;
import io.github.jongminchung.distributedlock.core.api.DistributedLock;
import io.github.jongminchung.distributedlock.provider.jdbc.config.JdbcLockProviderConfig;
import io.github.jongminchung.distributedlock.provider.jdbc.impl.JdbcDistributedLock;

@AutoConfiguration
@ConditionalOnClass({JdbcDistributedLock.class, DataSource.class})
@ConditionalOnProperty(prefix = "distributed-lock", name = "provider", havingValue = "jdbc")
public class JdbcLockAutoConfiguration {
    @Bean
    @ConditionalOnMissingBean
    public JdbcLockProviderConfig jdbcLockProviderConfig(DistributedLockProperties properties) {
        JdbcLockProviderConfig config = new JdbcLockProviderConfig();
        config.setTableName(properties.jdbc().tableName());
        config.setDefaultWaitTime(properties.waitTime());
        config.setDefaultLeaseTime(properties.leaseTime());
        return config;
    }

    @Bean
    @ConditionalOnMissingBean(DistributedLock.class)
    @ConditionalOnBean(DataSource.class)
    public DistributedLock jdbcDistributedLock(DataSource dataSource, JdbcLockProviderConfig config) {
        return new JdbcDistributedLock(dataSource, config);
    }
}
