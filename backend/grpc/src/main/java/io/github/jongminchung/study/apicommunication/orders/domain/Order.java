package io.github.jongminchung.study.apicommunication.orders.domain;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

public record Order(
        UUID id,
        String tenantId,
        String clientId,
        String customerId,
        List<String> productCodes,
        BigDecimal totalAmount,
        Instant createdAt,
        Instant updatedAt,
        long version) {

    public Order {
        Objects.requireNonNull(id, "id must be provided");
        Objects.requireNonNull(tenantId, "tenantId must be provided");
        Objects.requireNonNull(clientId, "clientId must be provided");
        Objects.requireNonNull(customerId, "customerId must be provided");
        Objects.requireNonNull(productCodes, "productCodes must be provided");
        Objects.requireNonNull(totalAmount, "totalAmount must be provided");
        Objects.requireNonNull(createdAt, "createdAt must be provided");
        Objects.requireNonNull(updatedAt, "updatedAt must be provided");
        productCodes = Collections.unmodifiableList(new ArrayList<>(productCodes));
    }

    public Order withUpdatedMetadata(Instant newUpdatedAt) {
        return new Order(
                id, tenantId, clientId, customerId, productCodes, totalAmount, createdAt, newUpdatedAt, version + 1);
    }
}
