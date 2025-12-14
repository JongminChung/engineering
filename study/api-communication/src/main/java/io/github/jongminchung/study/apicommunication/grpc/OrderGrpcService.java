package io.github.jongminchung.study.apicommunication.grpc;

import io.github.jongminchung.study.apicommunication.context.ApiRequestContext;
import io.github.jongminchung.study.apicommunication.orders.api.OrderProtoMapper;
import io.github.jongminchung.study.apicommunication.orders.domain.Order;
import io.github.jongminchung.study.apicommunication.orders.domain.OrderNotFoundException;
import io.github.jongminchung.study.apicommunication.orders.service.OrderService;
import io.github.jongminchung.study.apicommunication.proto.CreateOrderRequest;
import io.github.jongminchung.study.apicommunication.proto.GetOrderRequest;
import io.github.jongminchung.study.apicommunication.proto.OrderMessage;
import io.github.jongminchung.study.apicommunication.proto.OrderServiceGrpc;
import io.github.jongminchung.study.apicommunication.ratelimit.RateLimitExceededException;
import io.grpc.Status;
import io.grpc.stub.StreamObserver;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class OrderGrpcService extends OrderServiceGrpc.OrderServiceImplBase {

    private final OrderService orderService;
    private final OrderProtoMapper mapper;

    public OrderGrpcService(OrderService orderService, OrderProtoMapper mapper) {
        this.orderService = orderService;
        this.mapper = mapper;
    }

    @Override
    public void createOrder(CreateOrderRequest request, StreamObserver<OrderMessage> responseObserver) {
        ApiRequestContext context = GrpcRequestContext.requireContext();
        try {
            Order order = orderService.createOrder(mapper.toCommand(request), context);
            responseObserver.onNext(mapper.toMessage(order));
            responseObserver.onCompleted();
        } catch (RateLimitExceededException ex) {
            responseObserver.onError(Status.RESOURCE_EXHAUSTED.withDescription(ex.getMessage()).asRuntimeException());
        } catch (IllegalArgumentException ex) {
            responseObserver.onError(Status.INVALID_ARGUMENT.withDescription(ex.getMessage()).asRuntimeException());
        } catch (Exception ex) {
            responseObserver.onError(Status.INTERNAL.withDescription("Unable to create order").withCause(ex).asRuntimeException());
        }
    }

    @Override
    public void getOrder(GetOrderRequest request, StreamObserver<OrderMessage> responseObserver) {
        ApiRequestContext context = GrpcRequestContext.requireContext();
        try {
            UUID orderId = UUID.fromString(request.getOrderId());
            Order order = orderService.getOrder(orderId, context);
            responseObserver.onNext(mapper.toMessage(order));
            responseObserver.onCompleted();
        } catch (IllegalArgumentException ex) {
            responseObserver.onError(Status.INVALID_ARGUMENT.withDescription("order_id must be a valid UUID").withCause(ex).asRuntimeException());
        } catch (OrderNotFoundException ex) {
            responseObserver.onError(Status.NOT_FOUND.withDescription(ex.getMessage()).asRuntimeException());
        } catch (RateLimitExceededException ex) {
            responseObserver.onError(Status.RESOURCE_EXHAUSTED.withDescription(ex.getMessage()).asRuntimeException());
        } catch (Exception ex) {
            responseObserver.onError(Status.INTERNAL.withDescription("Unable to fetch order").withCause(ex).asRuntimeException());
        }
    }
}
