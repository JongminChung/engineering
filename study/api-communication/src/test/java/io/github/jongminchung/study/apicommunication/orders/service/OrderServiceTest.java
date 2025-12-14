package io.github.jongminchung.study.apicommunication.orders.service;

import io.github.jongminchung.study.apicommunication.context.ApiRequestContext;
import io.github.jongminchung.study.apicommunication.metrics.RequestMetrics;
import io.github.jongminchung.study.apicommunication.metrics.RequestMetricsSnapshot;
import io.github.jongminchung.study.apicommunication.orders.cache.OrderReadModelCache;
import io.github.jongminchung.study.apicommunication.orders.domain.CreateOrderCommand;
import io.github.jongminchung.study.apicommunication.orders.domain.Order;
import io.github.jongminchung.study.apicommunication.orders.domain.OrderNotFoundException;
import io.github.jongminchung.study.apicommunication.orders.domain.OrderRepository;
import io.github.jongminchung.study.apicommunication.ratelimit.RateLimitExceededException;
import io.github.jongminchung.study.apicommunication.ratelimit.RateLimiter;
import io.github.jongminchung.study.apicommunication.ratelimit.RateLimitedOperation;
import io.github.jongminchung.study.apicommunication.ratelimit.SlidingWindowRateLimiter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class OrderServiceTest {

    private final Clock clock = Clock.fixed(Instant.parse("2024-01-01T00:00:00Z"), ZoneOffset.UTC);
    private final ApiRequestContext context = new ApiRequestContext("tenant-alpha", "web-client", "trace-test");

    private RateLimiter rateLimiter;
    private OrderReadModelCache cache;
    private CountingOrderRepository repository;
    private RequestMetrics metrics;
    private AtomicLong idSequence;
    private OrderService orderService;

    @BeforeEach
    void setUp() {
        this.rateLimiter = new SlidingWindowRateLimiter(2, Duration.ofMinutes(1), clock);
        this.cache = new OrderReadModelCache(Duration.ofMinutes(5), clock);
        this.repository = new CountingOrderRepository();
        this.metrics = new RequestMetrics(clock);
        this.idSequence = new AtomicLong();
        orderService = new OrderService(repository, rateLimiter, cache, metrics, clock, this::nextId);
    }

    @Test
    void cachesReadModelAfterFirstHit() {
        Order created = orderService.createOrder(sampleCommand("alpha"), context);
        cache.invalidate(created.id());

        orderService.getOrder(created.id(), context);
        orderService.getOrder(created.id(), context);

        assertThat(repository.readCount()).isEqualTo(1);
    }

    @Test
    void rateLimiterBlocksAfterThreshold() {
        orderService.createOrder(sampleCommand("alpha"), context);
        orderService.createOrder(sampleCommand("beta"), context);

        assertThatThrownBy(() -> orderService.createOrder(sampleCommand("gamma"), context))
            .isInstanceOf(RateLimitExceededException.class);
    }

    @Test
    void metricsCaptureSuccessFailureAndCacheMiss() {
        orderService.createOrder(sampleCommand("alpha"), context);

        assertThatThrownBy(() -> orderService.getOrder(UUID.randomUUID(), context))
            .isInstanceOf(OrderNotFoundException.class);

        RequestMetricsSnapshot snapshot = metrics.snapshot();
        assertThat(snapshot.successfulRequests()).isEqualTo(1);
        assertThat(snapshot.failedRequests()).isEqualTo(1);
        assertThat(snapshot.cacheHits()).isZero();
        assertThat(snapshot.cacheMisses()).isEqualTo(1);
    }

    private CreateOrderCommand sampleCommand(String customerId) {
        return new CreateOrderCommand(customerId, List.of("svc", "grpc"), BigDecimal.TEN);
    }

    private UUID nextId() {
        return new UUID(0L, idSequence.incrementAndGet());
    }

    private static final class CountingOrderRepository implements OrderRepository {

        private final ConcurrentHashMap<UUID, Order> storage = new ConcurrentHashMap<>();
        private int readCount;

        @Override
        public Order save(Order order) {
            storage.put(order.id(), order);
            return order;
        }

        @Override
        public Optional<Order> findById(UUID id) {
            readCount++;
            return Optional.ofNullable(storage.get(id));
        }

        int readCount() {
            return readCount;
        }
    }
}
