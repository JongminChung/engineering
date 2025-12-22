package io.github.jongminchung.study.apicommunication.orders.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import io.github.jongminchung.study.apicommunication.context.ApiHeaders;
import io.github.jongminchung.study.apicommunication.proto.CreateOrderRequest;
import io.github.jongminchung.study.apicommunication.proto.OrderMessage;
import io.github.jongminchung.study.apicommunication.ratelimit.RateLimiter;

@SpringBootTest(properties = "test.context=OrderProtoControllerTest")
@AutoConfigureMockMvc
class OrderProtoControllerTest {

    private static final MediaType PROTOBUF = MediaType.parseMediaType("application/x-protobuf");

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private RateLimiter rateLimiter;

    @BeforeEach
    void setUpRateLimiter() {
        Mockito.when(rateLimiter.tryAcquire(ArgumentMatchers.any(), ArgumentMatchers.any()))
                .thenReturn(true);
    }

    @Test
    void createOrderViaRestProto() throws Exception {
        CreateOrderRequest request = sampleProtoRequest();

        MvcResult createResult = mockMvc.perform(authorized(post("/api/v1/orders/proto")
                        .contentType(PROTOBUF)
                        .accept(PROTOBUF)
                        .content(request.toByteArray())))
                .andExpect(status().isCreated())
                .andReturn();

        OrderMessage created = OrderMessage.parseFrom(createResult.getResponse().getContentAsByteArray());
        assertThat(created.getCustomerId()).isEqualTo("proto-user");
        assertThat(created.getProductCodesList()).containsExactly("rest", "grpc");

        MvcResult getResult = mockMvc.perform(authorized(
                        get("/api/v1/orders/{id}/proto", created.getOrderId()).accept(PROTOBUF)))
                .andExpect(status().isOk())
                .andReturn();

        OrderMessage fetched = OrderMessage.parseFrom(getResult.getResponse().getContentAsByteArray());
        assertThat(fetched.getOrderId()).isEqualTo(created.getOrderId());
        assertThat(fetched.getTenantId()).isEqualTo("tenant-alpha");
    }

    @Test
    void createOrderProtoRespondsWithKoreanContentLanguage() throws Exception {
        CreateOrderRequest request = sampleProtoRequest();

        mockMvc.perform(authorized(post("/api/v1/orders/proto")
                        .contentType(PROTOBUF)
                        .accept(PROTOBUF)
                        .header(HttpHeaders.ACCEPT_LANGUAGE, "ko, en")
                        .content(request.toByteArray())))
                .andExpect(status().isCreated())
                .andExpect(header().string(HttpHeaders.CONTENT_LANGUAGE, "ko"));
    }

    @Test
    void createOrderProtoRespondsWithEnglishContentLanguage() throws Exception {
        CreateOrderRequest request = sampleProtoRequest();

        mockMvc.perform(authorized(post("/api/v1/orders/proto")
                        .contentType(PROTOBUF)
                        .accept(PROTOBUF)
                        .header(HttpHeaders.ACCEPT_LANGUAGE, "en, ko")
                        .content(request.toByteArray())))
                .andExpect(status().isCreated())
                .andExpect(header().string(HttpHeaders.CONTENT_LANGUAGE, "en"));
    }

    private CreateOrderRequest sampleProtoRequest() {
        return CreateOrderRequest.newBuilder()
                .setCustomerId("proto-user")
                .addProductCodes("rest")
                .addProductCodes("grpc")
                .setTotalAmount(33.3)
                .build();
    }

    private MockHttpServletRequestBuilder authorized(
            org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder builder) {
        return builder.header(ApiHeaders.API_KEY, "local-api-key")
                .header(ApiHeaders.TENANT_ID, "tenant-alpha")
                .header(ApiHeaders.CLIENT_ID, "web-client");
    }
}
