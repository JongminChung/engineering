package io.github.jongminchung.study.apicommunication.orders.domain;

import java.util.UUID;

public class OrderNotFoundException extends RuntimeException {
    public OrderNotFoundException(UUID orderId) {
        super("Order %s does not exist".formatted(orderId));
    }
}
