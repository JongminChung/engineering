package io.github.jongminchung.study.apicommunication.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

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
