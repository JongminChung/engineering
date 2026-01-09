package io.github.jongminchung.distributedlock.starter.jdbc;

import java.lang.reflect.Proxy;
import javax.sql.DataSource;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import io.github.jongminchung.distributedlock.autoconfigure.configuration.DistributedLockAutoConfiguration;
import io.github.jongminchung.distributedlock.autoconfigure.configuration.JdbcLockAutoConfiguration;
import io.github.jongminchung.distributedlock.core.api.DistributedLock;
import io.github.jongminchung.distributedlock.provider.jdbc.impl.JdbcDistributedLock;

class SpringBootStarterJdbcTest {
    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withConfiguration(
                    AutoConfigurations.of(DistributedLockAutoConfiguration.class, JdbcLockAutoConfiguration.class));

    @Nested
    class JdbcProvider {
        @Test
        void createsJdbcDistributedLock() {
            contextRunner
                    .withPropertyValues("distributed-lock.provider=jdbc")
                    .withBean(DataSource.class, SpringBootStarterJdbcTest::dataSource)
                    .run(context -> {
                        DistributedLock distributedLock = context.getBean(DistributedLock.class);
                        Assertions.assertThat(distributedLock).isInstanceOf(JdbcDistributedLock.class);
                    });
        }
    }

    private static DataSource dataSource() {
        return (DataSource) Proxy.newProxyInstance(
                DataSource.class.getClassLoader(), new Class[] {DataSource.class}, (proxy, method, args) -> null);
    }
}
