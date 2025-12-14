package io.github.jongminchung.study.apicommunication.orders.domain;

import java.util.Optional;
import java.util.UUID;

public interface OrderRepository {
    Order save(Order order);

    Optional<Order> findById(UUID id);
}
