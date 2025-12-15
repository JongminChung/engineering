package io.github.jongminchung.study.apicommunication.config;

import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;

import lombok.Getter;
import lombok.Setter;

@Getter
@ConfigurationProperties(prefix = "app.study")
public class ApiStudyProperties {

    private final RateLimit rateLimit = new RateLimit();
    private final Cache cache = new Cache();

    @Setter
    @Getter
    public static class RateLimit {
        private int maxRequests = 5;
        private Duration window = Duration.ofSeconds(60);
    }

    @Getter
    @Setter
    public static class Cache {
        private Duration ttl = Duration.ofSeconds(30);
    }
}
