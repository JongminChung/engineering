package io.github.jongminchung.study.apicommunication.orders.api;

import io.github.jongminchung.study.apicommunication.context.ApiRequestContextHolder;
import io.github.jongminchung.study.apicommunication.orders.domain.Order;
import io.github.jongminchung.study.apicommunication.orders.service.OrderService;
import io.github.jongminchung.study.apicommunication.proto.CreateOrderRequest;
import io.github.jongminchung.study.apicommunication.proto.OrderMessage;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/orders")
public class OrderProtoController {

    private static final MediaType PROTOBUF = MediaType.parseMediaType("application/x-protobuf");

    private final OrderService orderService;
    private final OrderProtoMapper mapper;

    public OrderProtoController(OrderService orderService, OrderProtoMapper mapper) {
        this.orderService = orderService;
        this.mapper = mapper;
    }

    @PostMapping(value = "/proto", consumes = {"application/x-protobuf", "application/x-protobuf;charset=UTF-8"}, produces = {"application/x-protobuf", "application/x-protobuf;charset=UTF-8"})
    public ResponseEntity<OrderMessage> createOrderProto(@RequestBody CreateOrderRequest request) {
        Order order = orderService.createOrder(mapper.toCommand(request), ApiRequestContextHolder.requireContext());
        OrderMessage payload = mapper.toMessage(order);
        return ResponseEntity.created(URI.create("/api/v1/orders/" + order.id())).contentType(PROTOBUF).body(payload);
    }

    @GetMapping(value = "/{orderId}/proto", produces = {"application/x-protobuf", "application/x-protobuf;charset=UTF-8"})
    public ResponseEntity<OrderMessage> getOrderProto(@PathVariable UUID orderId) {
        Order order = orderService.getOrder(orderId, ApiRequestContextHolder.requireContext());
        return ResponseEntity.ok().contentType(PROTOBUF).body(mapper.toMessage(order));
    }
}
