package io.github.jongminchung.distributedlock.autoconfigure.properties;

import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "distributed-lock")
public class DistributedLockProperties {
    private String provider = "redis";
    private Duration waitTime = Duration.ofSeconds(5);
    private Duration leaseTime = Duration.ofSeconds(30);
    private boolean fair = false;
    private final Redis redis = new Redis();
    private final Jdbc jdbc = new Jdbc();

    public String provider() {
        return provider;
    }

    public void setProvider(String provider) {
        this.provider = provider;
    }

    public Duration waitTime() {
        return waitTime;
    }

    public void setWaitTime(Duration waitTime) {
        this.waitTime = waitTime;
    }

    public Duration leaseTime() {
        return leaseTime;
    }

    public void setLeaseTime(Duration leaseTime) {
        this.leaseTime = leaseTime;
    }

    public boolean fair() {
        return fair;
    }

    public void setFair(boolean fair) {
        this.fair = fair;
    }

    public Redis redis() {
        return redis;
    }

    public Jdbc jdbc() {
        return jdbc;
    }

    public static class Redis {
        private String keyPrefix = "lock:";

        public String keyPrefix() {
            return keyPrefix;
        }

        public void setKeyPrefix(String keyPrefix) {
            this.keyPrefix = keyPrefix;
        }
    }

    public static class Jdbc {
        private String tableName = "distributed_locks";

        public String tableName() {
            return tableName;
        }

        public void setTableName(String tableName) {
            this.tableName = tableName;
        }
    }
}
