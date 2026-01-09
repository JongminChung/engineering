package io.github.jongminchung.distributedlock.provider.redis.config;

import java.time.Duration;

public class RedisLockProviderConfig {
    private String keyPrefix = "lock:";
    private Duration defaultWaitTime = Duration.ofSeconds(5);
    private Duration defaultLeaseTime = Duration.ofSeconds(30);

    public String keyPrefix() {
        return keyPrefix;
    }

    public void setKeyPrefix(String keyPrefix) {
        this.keyPrefix = keyPrefix;
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
