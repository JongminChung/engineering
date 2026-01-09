package io.github.jongminchung.distributedlock.test.containers;

import org.testcontainers.containers.GenericContainer;
import org.testcontainers.utility.DockerImageName;

public final class RedisContainerSupport {
    private RedisContainerSupport() {}

    public static GenericContainer<?> createRedis() {
        return new GenericContainer(DockerImageName.parse("redis:7.2-alpine")).withExposedPorts(6379);
    }
}
