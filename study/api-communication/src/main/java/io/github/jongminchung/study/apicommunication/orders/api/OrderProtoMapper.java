package io.github.jongminchung.study.apicommunication.orders.api;

import com.google.protobuf.Timestamp;
import io.github.jongminchung.study.apicommunication.orders.domain.CreateOrderCommand;
import io.github.jongminchung.study.apicommunication.orders.domain.Order;
import io.github.jongminchung.study.apicommunication.proto.CreateOrderRequest;
import io.github.jongminchung.study.apicommunication.proto.OrderMessage;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.Instant;

@Component
public class OrderProtoMapper {

    public CreateOrderCommand toCommand(CreateOrderRequest request) {
        return new CreateOrderCommand(request.getCustomerId(), request.getProductCodesList(), BigDecimal.valueOf(request.getTotalAmount()));
    }

    public OrderMessage toMessage(Order order) {
        return OrderMessage.newBuilder()
            .setOrderId(order.id().toString())
            .setCustomerId(order.customerId())
            .addAllProductCodes(order.productCodes())
            .setTotalAmount(order.totalAmount().doubleValue())
            .setTenantId(order.tenantId())
            .setClientId(order.clientId())
            .setCreatedAt(toTimestamp(order.createdAt()))
            .setUpdatedAt(toTimestamp(order.updatedAt()))
            .setVersion(order.version())
            .build();
    }

    private Timestamp toTimestamp(Instant instant) {
        return Timestamp.newBuilder()
            .setSeconds(instant.getEpochSecond())
            .setNanos(instant.getNano())
            .build();
    }
}
