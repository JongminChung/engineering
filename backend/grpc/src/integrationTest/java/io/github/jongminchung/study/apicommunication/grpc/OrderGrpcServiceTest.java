package io.github.jongminchung.study.apicommunication.grpc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Clock;
import java.time.Duration;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.github.jongminchung.study.apicommunication.context.ApiHeaders;
import io.github.jongminchung.study.apicommunication.metrics.RequestMetrics;
import io.github.jongminchung.study.apicommunication.orders.api.OrderProtoMapper;
import io.github.jongminchung.study.apicommunication.orders.cache.OrderReadModelCache;
import io.github.jongminchung.study.apicommunication.orders.infrastructure.InMemoryOrderRepository;
import io.github.jongminchung.study.apicommunication.orders.service.OrderService;
import io.github.jongminchung.study.apicommunication.proto.CreateOrderRequest;
import io.github.jongminchung.study.apicommunication.proto.GetOrderRequest;
import io.github.jongminchung.study.apicommunication.proto.OrderMessage;
import io.github.jongminchung.study.apicommunication.proto.OrderServiceGrpc;
import io.github.jongminchung.study.apicommunication.ratelimit.RateLimiter;
import io.github.jongminchung.study.apicommunication.ratelimit.SlidingWindowRateLimiter;
import io.github.jongminchung.study.apicommunication.security.ApiKeyAuthenticator;
import io.github.jongminchung.study.apicommunication.security.ApiSecurityProperties;
import io.grpc.*;
import io.grpc.inprocess.InProcessChannelBuilder;
import io.grpc.inprocess.InProcessServerBuilder;
import io.grpc.stub.MetadataUtils;

class OrderGrpcServiceTest {

    private Server server;
    private ManagedChannel channel;
    private OrderServiceGrpc.OrderServiceBlockingStub stub;

    @BeforeEach
    void setUp() throws Exception {
        Clock clock = Clock.systemUTC();
        RateLimiter rateLimiter = new SlidingWindowRateLimiter(5, Duration.ofMinutes(1), clock);
        OrderReadModelCache cache = new OrderReadModelCache(Duration.ofMinutes(5), clock);
        RequestMetrics metrics = new RequestMetrics(clock);
        OrderService orderService = new OrderService(
                new InMemoryOrderRepository(), rateLimiter, cache, metrics, clock, java.util.UUID::randomUUID);
        var protoMapper = new OrderProtoMapper();
        OrderGrpcService grpcService = new OrderGrpcService(orderService, protoMapper);

        ApiSecurityProperties securityProperties = new ApiSecurityProperties();
        ApiSecurityProperties.Client client = new ApiSecurityProperties.Client();
        client.setTenantId("tenant-alpha");
        client.setClientId("web-client");
        client.setHashedApiKey("024f6c9525465fbec0047e2686f02a413c52241fde8af273148c419fa18fb312");
        securityProperties.getClients().add(client);
        ApiKeyAuthenticator authenticator = new ApiKeyAuthenticator(securityProperties);

        String serverName = InProcessServerBuilder.generateName();
        server = InProcessServerBuilder.forName(serverName)
                .directExecutor()
                .addService(ServerInterceptors.intercept(grpcService, new GrpcApiKeyServerInterceptor(authenticator)))
                .build()
                .start();
        channel = InProcessChannelBuilder.forName(serverName).directExecutor().build();
        stub = OrderServiceGrpc.newBlockingStub(channel);
    }

    @AfterEach
    void tearDown() throws Exception {
        if (channel != null) {
            channel.shutdownNow().awaitTermination(5, TimeUnit.SECONDS);
        }
        if (server != null) {
            server.shutdownNow().awaitTermination(5, TimeUnit.SECONDS);
        }
    }

    @Test
    void createAndFetchOrder() {
        OrderServiceGrpc.OrderServiceBlockingStub authorizedStub =
                stub.withInterceptors(MetadataUtils.newAttachHeadersInterceptor(metadata()));

        OrderMessage created = authorizedStub.createOrder(CreateOrderRequest.newBuilder()
                .setCustomerId("alpha")
                .addProductCodes("async")
                .addProductCodes("grpc")
                .setTotalAmount(42.25)
                .build());

        OrderMessage fetched = authorizedStub.getOrder(
                GetOrderRequest.newBuilder().setOrderId(created.getOrderId()).build());

        assertThat(fetched.getCustomerId()).isEqualTo("alpha");
        assertThat(fetched.getProductCodesList()).containsExactly("async", "grpc");
        assertThat(fetched.getTenantId()).isEqualTo("tenant-alpha");
    }

    @Test
    void rejectsMissingMetadata() {
        assertThatThrownBy(() -> stub.createOrder(CreateOrderRequest.newBuilder()
                        .setCustomerId("alpha")
                        .addProductCodes("grpc")
                        .setTotalAmount(1)
                        .build()))
                .isInstanceOf(StatusRuntimeException.class)
                .hasMessageContaining("UNAUTHENTICATED");
    }

    private Metadata metadata() {
        Metadata metadata = new Metadata();
        metadata.put(ApiHeaders.GRPC_TENANT_ID, "tenant-alpha");
        metadata.put(ApiHeaders.GRPC_CLIENT_ID, "web-client");
        metadata.put(ApiHeaders.GRPC_API_KEY, "local-api-key");
        metadata.put(ApiHeaders.GRPC_TRACE_ID, "grpc-trace");
        return metadata;
    }
}
