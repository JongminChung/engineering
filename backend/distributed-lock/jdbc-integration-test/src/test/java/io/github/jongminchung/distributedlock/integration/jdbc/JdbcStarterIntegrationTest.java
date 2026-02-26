package io.github.jongminchung.distributedlock.integration.jdbc;

import java.sql.Connection;
import java.sql.Statement;
import java.time.Duration;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import javax.sql.DataSource;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.postgresql.ds.PGSimpleDataSource;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.testcontainers.postgresql.PostgreSQLContainer;

import io.github.jongminchung.distributedlock.autoconfigure.configuration.DistributedLockAutoConfiguration;
import io.github.jongminchung.distributedlock.autoconfigure.configuration.JdbcLockAutoConfiguration;
import io.github.jongminchung.distributedlock.core.api.DistributedLock;
import io.github.jongminchung.distributedlock.core.api.LockHandle;
import io.github.jongminchung.distributedlock.core.api.LockRequest;
import io.github.jongminchung.distributedlock.core.exception.LockTimeoutException;
import io.github.jongminchung.distributedlock.core.key.LockKey;

class JdbcStarterIntegrationTest {
    private static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer("postgres:18");

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withConfiguration(
                    AutoConfigurations.of(DistributedLockAutoConfiguration.class, JdbcLockAutoConfiguration.class))
            .withPropertyValues("distributed-lock.provider=jdbc")
            .withBean(DataSource.class, JdbcStarterIntegrationTest::createDataSource);

    @BeforeAll
    static void startPostgres() throws Exception {
        POSTGRES.start();
        createSchema();
    }

    @AfterAll
    static void stopPostgres() {
        POSTGRES.stop();
    }

    @Nested
    class WhenPostgresIsAvailable {
        @BeforeEach
        void clearLocks() throws Exception {
            try (Connection connection = createDataSource().getConnection();
                    Statement statement = connection.createStatement()) {
                statement.executeUpdate("delete from distributed_locks");
            }
        }

        @Test
        void acquiresAndReacquiresLock() {
            contextRunner.run(context -> {
                var distributedLock = context.getBean(DistributedLock.class);
                var request = LockRequest.of(LockKey.of("jdbc:1"), Duration.ofSeconds(1), Duration.ofSeconds(2), false);

                LockHandle handle = distributedLock.acquire(request);
                Optional<LockHandle> secondAttempt = distributedLock.tryAcquire(request);
                Assertions.assertThat(secondAttempt).isEmpty();

                handle.release();
                Optional<LockHandle> reacquired = distributedLock.tryAcquire(request);
                Assertions.assertThat(reacquired).isPresent();
                reacquired.get().release();
            });
        }

        @Test
        void timesOutWhenLockHeld() {
            contextRunner.run(context -> {
                var distributedLock = context.getBean(DistributedLock.class);
                var executor = Executors.newSingleThreadExecutor();
                var entered = new CountDownLatch(1);
                var release = new CountDownLatch(1);
                var holderRequest =
                        LockRequest.of(LockKey.of("jdbc:timeout"), Duration.ofSeconds(1), Duration.ofSeconds(5), false);

                try {
                    Future<?> holder = executor.submit(() -> {
                        LockHandle handle = distributedLock.acquire(holderRequest);
                        entered.countDown();
                        try {
                            release.await();
                        } catch (InterruptedException ex) {
                            Thread.currentThread().interrupt();
                        } finally {
                            handle.release();
                        }
                    });

                    entered.await(2, TimeUnit.SECONDS);
                    LockRequest timeoutRequest = LockRequest.of(
                            LockKey.of("jdbc:timeout"), Duration.ofMillis(200), Duration.ofSeconds(1), false);

                    Assertions.assertThatThrownBy(() -> distributedLock.acquire(timeoutRequest))
                            .isInstanceOf(LockTimeoutException.class);

                    release.countDown();
                    try {
                        holder.get(2, TimeUnit.SECONDS);
                    } catch (InterruptedException _) {
                        Thread.currentThread().interrupt();
                    } catch (java.util.concurrent.ExecutionException ex) {
                        throw new IllegalStateException(ex);
                    }
                } finally {
                    executor.shutdownNow();
                }
            });
        }

        @Test
        void reacquiresAfterLeaseExpiry() {
            contextRunner.run(context -> {
                DistributedLock distributedLock = context.getBean(DistributedLock.class);
                LockRequest request =
                        LockRequest.of(LockKey.of("jdbc:lease"), Duration.ofSeconds(1), Duration.ofMillis(200), false);

                LockHandle handle = distributedLock.acquire(request);
                Thread.sleep(400);

                Optional<LockHandle> reacquired = distributedLock.tryAcquire(request);
                Assertions.assertThat(reacquired).isPresent();
                reacquired.get().release();
                handle.release();
            });
        }
    }

    private static void createSchema() throws Exception {
        try (var connection = createDataSource().getConnection();
                var statement = connection.createStatement()) {
            statement.executeUpdate("create table if not exists distributed_locks ("
                    + "lock_key varchar(255) primary key,"
                    + "owner varchar(255) not null,"
                    + "expires_at timestamp(6) not null,"
                    + "locked_at timestamp(6) not null"
                    + ")");
        }
    }

    private static DataSource createDataSource() {
        PGSimpleDataSource dataSource = new PGSimpleDataSource();
        dataSource.setUrl(POSTGRES.getJdbcUrl());
        dataSource.setUser(POSTGRES.getUsername());
        dataSource.setPassword(POSTGRES.getPassword());
        return dataSource;
    }
}
