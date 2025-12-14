package io.github.jongminchung.study.apicommunication.orders.api;

import io.github.jongminchung.study.apicommunication.context.ApiHeaders;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import tools.jackson.databind.ObjectMapper;

import java.math.BigDecimal;
import java.util.List;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class OrderControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void createOrderThroughRestApi() throws Exception {
        String payload = objectMapper.writeValueAsString(sampleRequest());

        mockMvc.perform(authorized(post("/api/v1/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.customerId").value("alpha"))
                .andExpect(jsonPath("$.tenantId").value("tenant-alpha"));
    }

    @Test
    void missingApiKeyReturnsUnauthorized() throws Exception {
        mockMvc.perform(post("/api/v1/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(sampleRequest())))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void rateLimitIsEnforced() throws Exception {
        String payload = objectMapper.writeValueAsString(sampleRequest());

        mockMvc.perform(authorized(post("/api/v1/orders").contentType(MediaType.APPLICATION_JSON).content(payload)))
                .andExpect(status().isCreated());

        mockMvc.perform(authorized(post("/api/v1/orders").contentType(MediaType.APPLICATION_JSON).content(payload)))
                .andExpect(status().isCreated());

        mockMvc.perform(authorized(post("/api/v1/orders").contentType(MediaType.APPLICATION_JSON).content(payload)))
                .andExpect(status().isTooManyRequests());
    }

    private CreateOrderRequest sampleRequest() {
        CreateOrderRequest request = new CreateOrderRequest();
        request.setCustomerId("alpha");
        request.setProductCodes(List.of("api", "grpc"));
        request.setTotalAmount(new BigDecimal("42.50"));
        return request;
    }

    private MockHttpServletRequestBuilder authorized(MockHttpServletRequestBuilder builder) {
        return builder
                .header(ApiHeaders.API_KEY, "local-api-key")
                .header(ApiHeaders.TENANT_ID, "tenant-alpha")
                .header(ApiHeaders.CLIENT_ID, "web-client");
    }
}
