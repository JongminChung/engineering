package io.github.jongminchung.study.apicommunication.orders.api;

import java.net.URI;
import java.util.UUID;

import jakarta.validation.Valid;

import org.springframework.http.HttpHeaders;
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

@RestController
@RequestMapping("/api/v1/orders")
public class OrderController {

    private final OrderService orderService;
    private final ResponseLanguageResolver responseLanguageResolver;

    public OrderController(OrderService orderService, ResponseLanguageResolver responseLanguageResolver) {
        this.orderService = orderService;
        this.responseLanguageResolver = responseLanguageResolver;
    }

    @PostMapping
    public ResponseEntity<OrderResponse> createOrder(
            @Valid @RequestBody CreateOrderRequest request,
            @RequestHeader(value = HttpHeaders.ACCEPT_LANGUAGE, required = false) String acceptLanguage) {
        Order order = orderService.createOrder(request.toCommand(), ApiRequestContextHolder.requireContext());
        String responseLanguage = responseLanguageResolver.resolve(acceptLanguage);
        return ResponseEntity.created(URI.create("/api/v1/orders/" + order.id()))
                .header(HttpHeaders.CONTENT_LANGUAGE, responseLanguage)
                .body(OrderResponse.from(order));
    }

    @GetMapping("/{orderId}")
    public ResponseEntity<OrderResponse> getOrder(
            @PathVariable UUID orderId,
            @RequestHeader(value = HttpHeaders.ACCEPT_LANGUAGE, required = false) String acceptLanguage) {
        Order order = orderService.getOrder(orderId, ApiRequestContextHolder.requireContext());
        String responseLanguage = responseLanguageResolver.resolve(acceptLanguage);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_LANGUAGE, responseLanguage)
                .body(OrderResponse.from(order));
    }
}
