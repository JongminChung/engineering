package io.github.jongminchung.study.apicommunication.config;

import java.time.Clock;
import java.util.UUID;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.github.jongminchung.study.apicommunication.metrics.RequestMetrics;
import io.github.jongminchung.study.apicommunication.orders.cache.OrderReadModelCache;
import io.github.jongminchung.study.apicommunication.orders.domain.OrderRepository;
import io.github.jongminchung.study.apicommunication.orders.infrastructure.InMemoryOrderRepository;
import io.github.jongminchung.study.apicommunication.orders.service.OrderService;
import io.github.jongminchung.study.apicommunication.ratelimit.RateLimiter;
import io.github.jongminchung.study.apicommunication.ratelimit.SlidingWindowRateLimiter;
import io.github.jongminchung.study.apicommunication.security.ApiSecurityProperties;

@Configuration
@EnableConfigurationProperties({ApiSecurityProperties.class, ApiStudyProperties.class})
public class ApiStudyConfig {

    @Bean
    public Clock clock() {
        return Clock.systemUTC();
    }

    @Bean
    public RateLimiter rateLimiter(ApiStudyProperties properties, Clock clock) {
        return new SlidingWindowRateLimiter(
                properties.getRateLimit().getMaxRequests(),
                properties.getRateLimit().getWindow(),
                clock);
    }

    @Bean
    public OrderReadModelCache orderReadModelCache(ApiStudyProperties properties, Clock clock) {
        return new OrderReadModelCache(properties.getCache().getTtl(), clock);
    }

    @Bean
    public OrderRepository orderRepository() {
        return new InMemoryOrderRepository();
    }

    @Bean
    public RequestMetrics requestMetrics(Clock clock) {
        return new RequestMetrics(clock);
    }

    @Bean
    public OrderService orderService(
            OrderRepository repository,
            RateLimiter rateLimiter,
            OrderReadModelCache cache,
            RequestMetrics metrics,
            Clock clock) {
        return new OrderService(repository, rateLimiter, cache, metrics, clock, UUID::randomUUID);
    }
}
