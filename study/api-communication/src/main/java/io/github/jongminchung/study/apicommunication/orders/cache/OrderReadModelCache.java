package io.github.jongminchung.study.apicommunication.orders.cache;

import io.github.jongminchung.study.apicommunication.orders.domain.Order;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class OrderReadModelCache {

    private final Duration ttl;
    private final Clock clock;
    private final Map<UUID, CachedOrder> cache = new ConcurrentHashMap<>();

    public OrderReadModelCache(Duration ttl, Clock clock) {
        this.ttl = ttl;
        this.clock = clock;
    }

    public Optional<Order> get(UUID orderId) {
        CachedOrder cachedOrder = cache.get(orderId);
        if (cachedOrder == null) {
            return Optional.empty();
        }
        if (cachedOrder.isExpired(clock.instant())) {
            cache.remove(orderId);
            return Optional.empty();
        }
        return Optional.of(cachedOrder.order());
    }

    public Order cache(Order order) {
        cache.put(order.id(), new CachedOrder(order, clock.instant().plus(ttl)));
        return order;
    }

    public void invalidate(UUID orderId) {
        cache.remove(orderId);
    }

    private record CachedOrder(Order order, Instant expiresAt) {
        boolean isExpired(Instant now) {
            return now.isAfter(expiresAt);
        }
    }
}
