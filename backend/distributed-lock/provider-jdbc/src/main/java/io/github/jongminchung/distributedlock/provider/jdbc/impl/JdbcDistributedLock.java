package io.github.jongminchung.distributedlock.provider.jdbc.impl;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.SQLIntegrityConstraintViolationException;
import java.sql.Timestamp;
import java.time.Duration;
import java.time.Instant;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import javax.sql.DataSource;

import io.github.jongminchung.distributedlock.core.api.DistributedLock;
import io.github.jongminchung.distributedlock.core.api.LockHandle;
import io.github.jongminchung.distributedlock.core.api.LockRequest;
import io.github.jongminchung.distributedlock.core.exception.LockAcquisitionException;
import io.github.jongminchung.distributedlock.core.exception.LockReleaseException;
import io.github.jongminchung.distributedlock.core.exception.LockTimeoutException;
import io.github.jongminchung.distributedlock.core.key.LockKey;
import io.github.jongminchung.distributedlock.provider.jdbc.config.JdbcLockProviderConfig;

public class JdbcDistributedLock implements DistributedLock {
    private static final Duration DEFAULT_RETRY_DELAY = Duration.ofMillis(50);

    private final DataSource dataSource;
    private final JdbcLockProviderConfig config;

    public JdbcDistributedLock(DataSource dataSource, JdbcLockProviderConfig config) {
        this.dataSource = Objects.requireNonNull(dataSource, "dataSource");
        this.config = Objects.requireNonNull(config, "config");
    }

    @Override
    public LockHandle acquire(LockRequest request) {
        Duration waitTime = resolveWaitTime(request);
        Duration leaseTime = resolveLeaseTime(request);
        Instant deadline = Instant.now().plus(waitTime);
        String owner = UUID.randomUUID().toString();

        while (true) {
            if (tryAcquireInternal(request.key(), owner, leaseTime)) {
                return new JdbcLockHandle(request.key(), owner);
            }
            if (Instant.now().isAfter(deadline)) {
                throw new LockTimeoutException("Failed to acquire lock within wait time: "
                        + request.key().value());
            }
            sleep(DEFAULT_RETRY_DELAY);
        }
    }

    @Override
    public Optional<LockHandle> tryAcquire(LockRequest request) {
        Duration leaseTime = resolveLeaseTime(request);
        String owner = UUID.randomUUID().toString();
        if (tryAcquireInternal(request.key(), owner, leaseTime)) {
            return Optional.of(new JdbcLockHandle(request.key(), owner));
        }
        return Optional.empty();
    }

    private boolean tryAcquireInternal(LockKey key, String owner, Duration leaseTime) {
        Instant now = Instant.now();
        Instant expiresAt = now.plus(leaseTime);
        String table = config.tableName();
        try (Connection connection = dataSource.getConnection()) {
            connection.setAutoCommit(true);
            if (insertLock(connection, table, key, owner, now, expiresAt)) {
                return true;
            }
            return updateIfExpired(connection, table, key, owner, now, expiresAt);
        } catch (SQLException ex) {
            throw new LockAcquisitionException("Failed to acquire JDBC lock: " + key.value(), ex);
        }
    }

    private boolean insertLock(
            Connection connection, String table, LockKey key, String owner, Instant lockedAt, Instant expiresAt)
            throws SQLException {
        String sql = "insert into " + table + " (lock_key, owner, expires_at, locked_at) values (?, ?, ?, ?)";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, key.value());
            statement.setString(2, owner);
            statement.setTimestamp(3, Timestamp.from(expiresAt));
            statement.setTimestamp(4, Timestamp.from(lockedAt));
            return statement.executeUpdate() == 1;
        } catch (SQLException ex) {
            if (isDuplicateKey(ex)) {
                return false;
            }
            throw ex;
        }
    }

    private boolean updateIfExpired(
            Connection connection, String table, LockKey key, String owner, Instant lockedAt, Instant expiresAt)
            throws SQLException {
        String sql = "update " + table + " set owner = ?, expires_at = ?, locked_at = ? "
                + "where lock_key = ? and expires_at < ?";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, owner);
            statement.setTimestamp(2, Timestamp.from(expiresAt));
            statement.setTimestamp(3, Timestamp.from(lockedAt));
            statement.setString(4, key.value());
            statement.setTimestamp(5, Timestamp.from(lockedAt));
            return statement.executeUpdate() == 1;
        }
    }

    private Duration resolveWaitTime(LockRequest request) {
        if (request.waitTime() == null || request.waitTime().isNegative()) {
            return config.defaultWaitTime();
        }
        if (request.waitTime().isZero()) {
            return config.defaultWaitTime();
        }
        return request.waitTime();
    }

    private Duration resolveLeaseTime(LockRequest request) {
        if (request.leaseTime() == null || request.leaseTime().isNegative()) {
            return config.defaultLeaseTime();
        }
        if (request.leaseTime().isZero()) {
            return config.defaultLeaseTime();
        }
        return request.leaseTime();
    }

    private void release(LockKey key, String owner) {
        String sql = "delete from " + config.tableName() + " where lock_key = ? and owner = ?";
        try (Connection connection = dataSource.getConnection();
                PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, key.value());
            statement.setString(2, owner);
            statement.executeUpdate();
        } catch (SQLException ex) {
            throw new LockReleaseException("Failed to release JDBC lock: " + key.value(), ex);
        }
    }

    private void sleep(Duration delay) {
        try {
            Thread.sleep(delay.toMillis());
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
        }
    }

    private boolean isDuplicateKey(SQLException ex) {
        if (ex instanceof SQLIntegrityConstraintViolationException) {
            return true;
        }
        String sqlState = ex.getSQLState();
        return "23505".equals(sqlState) || "23000".equals(sqlState);
    }

    private class JdbcLockHandle implements LockHandle {
        private final LockKey key;
        private final String owner;

        private JdbcLockHandle(LockKey key, String owner) {
            this.key = key;
            this.owner = owner;
        }

        @Override
        public LockKey key() {
            return key;
        }

        @Override
        public void release() {
            JdbcDistributedLock.this.release(key, owner);
        }
    }
}
