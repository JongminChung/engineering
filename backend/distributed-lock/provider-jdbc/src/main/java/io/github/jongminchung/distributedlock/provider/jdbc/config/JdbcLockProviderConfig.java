package io.github.jongminchung.distributedlock.provider.jdbc.config;

import java.time.Duration;

public class JdbcLockProviderConfig {
    private String tableName = "distributed_locks";
    private Duration defaultWaitTime = Duration.ofSeconds(5);
    private Duration defaultLeaseTime = Duration.ofSeconds(30);

    public String tableName() {
        return tableName;
    }

    public void setTableName(String tableName) {
        this.tableName = tableName;
    }

    public Duration defaultWaitTime() {
        return defaultWaitTime;
    }

    public void setDefaultWaitTime(Duration defaultWaitTime) {
        this.defaultWaitTime = defaultWaitTime;
    }

    public Duration defaultLeaseTime() {
        return defaultLeaseTime;
    }

    public void setDefaultLeaseTime(Duration defaultLeaseTime) {
        this.defaultLeaseTime = defaultLeaseTime;
    }
}
