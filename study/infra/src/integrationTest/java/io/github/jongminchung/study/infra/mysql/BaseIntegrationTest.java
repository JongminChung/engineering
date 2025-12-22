package io.github.jongminchung.study.infra.mysql;

import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.mysql.MySQLContainer;
import org.testcontainers.utility.DockerImageName;

public abstract class BaseIntegrationTest {

    protected static final MySQLContainer mysql =
            new MySQLContainer(DockerImageName.parse("mysql:8.4.0")).withReuse(true);

    static {
        if (!mysql.isRunning()) {
            mysql.start();
        }
    }

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", mysql::getJdbcUrl);
        registry.add("spring.datasource.username", mysql::getUsername);
        registry.add("spring.datasource.password", mysql::getPassword);
    }

}
