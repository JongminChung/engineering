package io.github.jongminchung.study.apicommunication.orders.api;

import io.github.jongminchung.study.apicommunication.orders.domain.Order;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record OrderResponse(UUID orderId,
                            String customerId,
                            List<String> productCodes,
                            BigDecimal totalAmount,
                            String tenantId,
                            String clientId,
                            Instant createdAt,
                            Instant updatedAt,
                            long version) {

    public static OrderResponse from(Order order) {
        return new OrderResponse(
            order.id(),
            order.customerId(),
            order.productCodes(),
            order.totalAmount(),
            order.tenantId(),
            order.clientId(),
            order.createdAt(),
            order.updatedAt(),
            order.version()
        );
    }
}
