package io.github.jongminchung.study.apicommunication.orders.service;

import io.github.jongminchung.study.apicommunication.context.ApiRequestContext;
import io.github.jongminchung.study.apicommunication.metrics.RequestMetrics;
import io.github.jongminchung.study.apicommunication.orders.cache.OrderReadModelCache;
import io.github.jongminchung.study.apicommunication.orders.domain.CreateOrderCommand;
import io.github.jongminchung.study.apicommunication.orders.domain.Order;
import io.github.jongminchung.study.apicommunication.orders.domain.OrderNotFoundException;
import io.github.jongminchung.study.apicommunication.orders.domain.OrderRepository;
import io.github.jongminchung.study.apicommunication.ratelimit.RateLimitExceededException;
import io.github.jongminchung.study.apicommunication.ratelimit.RateLimiter;
import io.github.jongminchung.study.apicommunication.ratelimit.RateLimitedOperation;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Supplier;

@Service
public class OrderService {

    private final OrderRepository repository;
    private final RateLimiter rateLimiter;
    private final OrderReadModelCache cache;
    private final RequestMetrics metrics;
    private final Clock clock;
    private final Supplier<UUID> idGenerator;

    public OrderService(OrderRepository repository,
                        RateLimiter rateLimiter,
                        OrderReadModelCache cache,
                        RequestMetrics metrics,
                        Clock clock,
                        Supplier<UUID> idGenerator) {
        this.repository = Objects.requireNonNull(repository);
        this.rateLimiter = Objects.requireNonNull(rateLimiter);
        this.cache = Objects.requireNonNull(cache);
        this.metrics = Objects.requireNonNull(metrics);
        this.clock = Objects.requireNonNull(clock);
        this.idGenerator = Objects.requireNonNull(idGenerator);
    }

    public Order createOrder(CreateOrderCommand command, ApiRequestContext context) {
        enforceRateLimit(context, RateLimitedOperation.WRITE);
        Instant start = clock.instant();
        try {
            Instant now = clock.instant();
            Order order = new Order(
                idGenerator.get(),
                context.tenantId(),
                context.clientId(),
                command.getCustomerId(),
                command.getProductCodes(),
                command.getTotalAmount(),
                now,
                now,
                1L
            );
            repository.save(order);
            cache.cache(order);
            metrics.recordSuccess(Duration.between(start, clock.instant()));
            return order;
        } catch (RuntimeException ex) {
            metrics.recordFailure(Duration.between(start, clock.instant()));
            throw ex;
        }
    }

    public Order getOrder(UUID orderId, ApiRequestContext context) {
        enforceRateLimit(context, RateLimitedOperation.READ);
        Instant start = clock.instant();
        try {
            return cache.get(orderId)
                .map(order -> {
                    metrics.recordCacheHit();
                    metrics.recordSuccess(Duration.between(start, clock.instant()));
                    return order;
                })
                .orElseGet(() -> {
                    metrics.recordCacheMiss();
                    Order order = repository.findById(orderId).orElseThrow(() -> new OrderNotFoundException(orderId));
                    cache.cache(order);
                    metrics.recordSuccess(Duration.between(start, clock.instant()));
                    return order;
                });
        } catch (RuntimeException ex) {
            metrics.recordFailure(Duration.between(start, clock.instant()));
            throw ex;
        }
    }

    private void enforceRateLimit(ApiRequestContext context, RateLimitedOperation operation) {
        if (!rateLimiter.tryAcquire(context, operation)) {
            throw new RateLimitExceededException("Rate limit exceeded for " + context.clientId());
        }
    }
}
