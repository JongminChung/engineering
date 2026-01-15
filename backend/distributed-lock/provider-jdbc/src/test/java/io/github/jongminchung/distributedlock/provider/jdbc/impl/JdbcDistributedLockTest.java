package io.github.jongminchung.distributedlock.provider.jdbc.impl;

import java.sql.Connection;
import java.sql.Statement;
import java.time.Duration;
import java.util.Optional;
import javax.sql.DataSource;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.postgresql.ds.PGSimpleDataSource;
import org.testcontainers.postgresql.PostgreSQLContainer;

import io.github.jongminchung.distributedlock.core.api.LockHandle;
import io.github.jongminchung.distributedlock.core.api.LockRequest;
import io.github.jongminchung.distributedlock.core.key.LockKey;
import io.github.jongminchung.distributedlock.provider.jdbc.config.JdbcLockProviderConfig;

class JdbcDistributedLockTest {
    private static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer("postgres:18");

    @BeforeAll
    static void setUpSchema() throws Exception {
        POSTGRES.start();
        try (Connection connection = createDataSource().getConnection();
                Statement statement = connection.createStatement()) {
            statement.executeUpdate("create table if not exists distributed_locks ("
                    + "lock_key varchar(255) primary key,"
                    + "owner varchar(255) not null,"
                    + "expires_at timestamp(6) not null,"
                    + "locked_at timestamp(6) not null"
                    + ")");
        }
    }

    @AfterAll
    static void tearDown() {
        POSTGRES.stop();
    }

    @BeforeEach
    void clearLocks() throws Exception {
        try (Connection connection = createDataSource().getConnection();
                Statement statement = connection.createStatement()) {
            statement.executeUpdate("delete from distributed_locks");
        }
    }

    @Test
    void acquiresAndReleasesLock() {
        JdbcLockProviderConfig config = new JdbcLockProviderConfig();
        config.setDefaultWaitTime(Duration.ofSeconds(1));
        config.setDefaultLeaseTime(Duration.ofSeconds(2));
        JdbcDistributedLock lock = new JdbcDistributedLock(createDataSource(), config);

        LockRequest request =
                LockRequest.of(LockKey.of("invoice:9"), Duration.ofSeconds(1), Duration.ofSeconds(2), false);
        LockHandle handle = lock.acquire(request);

        Optional<LockHandle> secondAttempt = lock.tryAcquire(request);
        Assertions.assertThat(secondAttempt).isEmpty();

        handle.release();
        Optional<LockHandle> reacquired = lock.tryAcquire(request);
        Assertions.assertThat(reacquired).isPresent();
        reacquired.get().release();
    }

    private static DataSource createDataSource() {
        PGSimpleDataSource dataSource = new PGSimpleDataSource();
        dataSource.setUrl(POSTGRES.getJdbcUrl());
        dataSource.setUser(POSTGRES.getUsername());
        dataSource.setPassword(POSTGRES.getPassword());
        return dataSource;
    }
}
