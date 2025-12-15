package io.github.jongminchung.study.apicommunication.orders.infrastructure;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import io.github.jongminchung.study.apicommunication.orders.domain.Order;
import io.github.jongminchung.study.apicommunication.orders.domain.OrderRepository;

public class InMemoryOrderRepository implements OrderRepository {

    private final Map<UUID, Order> storage = new ConcurrentHashMap<>();

    @Override
    public Order save(Order order) {
        storage.put(order.id(), order);
        return order;
    }

    @Override
    public Optional<Order> findById(UUID id) {
        return Optional.ofNullable(storage.get(id));
    }
}
