package io.github.jongminchung.study.apicommunication.orders.api;

import java.net.URI;
import java.util.UUID;

import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import io.github.jongminchung.study.apicommunication.context.ApiRequestContextHolder;
import io.github.jongminchung.study.apicommunication.context.ResponseLanguageResolver;
import io.github.jongminchung.study.apicommunication.orders.domain.Order;
import io.github.jongminchung.study.apicommunication.orders.service.OrderService;
import io.github.jongminchung.study.apicommunication.proto.CreateOrderRequest;
import io.github.jongminchung.study.apicommunication.proto.OrderMessage;

@RestController
@RequestMapping("/api/v1/orders")
public class OrderProtoController {

    private static final MediaType PROTOBUF = MediaType.parseMediaType("application/x-protobuf");

    private final OrderService orderService;
    private final OrderProtoMapper mapper;
    private final ResponseLanguageResolver responseLanguageResolver;

    public OrderProtoController(
            OrderService orderService, OrderProtoMapper mapper, ResponseLanguageResolver responseLanguageResolver) {
        this.orderService = orderService;
        this.mapper = mapper;
        this.responseLanguageResolver = responseLanguageResolver;
    }

    @PostMapping(
            value = "/proto",
            consumes = {"application/x-protobuf", "application/x-protobuf;charset=UTF-8"},
            produces = {"application/x-protobuf", "application/x-protobuf;charset=UTF-8"})
    public ResponseEntity<OrderMessage> createOrderProto(
            @RequestBody CreateOrderRequest request,
            @RequestHeader(value = HttpHeaders.ACCEPT_LANGUAGE, required = false) String acceptLanguage) {
        Order order = orderService.createOrder(mapper.toCommand(request), ApiRequestContextHolder.requireContext());
        OrderMessage payload = mapper.toMessage(order);
        String responseLanguage = responseLanguageResolver.resolve(acceptLanguage);
        return ResponseEntity.created(URI.create("/api/v1/orders/" + order.id()))
                .contentType(PROTOBUF)
                .header(HttpHeaders.CONTENT_LANGUAGE, responseLanguage)
                .body(payload);
    }

    @GetMapping(
            value = "/{orderId}/proto",
            produces = {"application/x-protobuf", "application/x-protobuf;charset=UTF-8"})
    public ResponseEntity<OrderMessage> getOrderProto(
            @PathVariable UUID orderId,
            @RequestHeader(value = HttpHeaders.ACCEPT_LANGUAGE, required = false) String acceptLanguage) {
        Order order = orderService.getOrder(orderId, ApiRequestContextHolder.requireContext());
        String responseLanguage = responseLanguageResolver.resolve(acceptLanguage);
        return ResponseEntity.ok()
                .contentType(PROTOBUF)
                .header(HttpHeaders.CONTENT_LANGUAGE, responseLanguage)
                .body(mapper.toMessage(order));
    }
}
