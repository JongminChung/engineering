package io.github.jongminchung.distributedlock.provider.jdbc.impl;

import java.sql.Connection;
import java.sql.Statement;
import java.time.Duration;
import java.util.Optional;
import javax.sql.DataSource;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.testcontainers.mysql.MySQLContainer;

import com.mysql.cj.jdbc.MysqlDataSource;

import io.github.jongminchung.distributedlock.core.api.LockHandle;
import io.github.jongminchung.distributedlock.core.api.LockRequest;
import io.github.jongminchung.distributedlock.core.key.LockKey;
import io.github.jongminchung.distributedlock.provider.jdbc.config.JdbcLockProviderConfig;

class JdbcDistributedLockTest {
    private static final MySQLContainer MYSQL = new MySQLContainer("mysql:8.4.0");

    @BeforeAll
    static void setUpSchema() throws Exception {
        MYSQL.start();
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
        MYSQL.stop();
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
        MysqlDataSource dataSource = new MysqlDataSource();
        dataSource.setUrl(MYSQL.getJdbcUrl());
        dataSource.setUser(MYSQL.getUsername());
        dataSource.setPassword(MYSQL.getPassword());
        return dataSource;
    }
}
