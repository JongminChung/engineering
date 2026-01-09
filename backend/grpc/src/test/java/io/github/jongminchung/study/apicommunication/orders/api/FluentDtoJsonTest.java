package io.github.jongminchung.study.apicommunication.orders.api;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.json.JsonTest;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Getter;
import lombok.experimental.Accessors;

import tools.jackson.databind.ObjectMapper;

@JsonTest
class FluentDtoJsonTest {

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void deserializeFluentDto() {
        var json = """
            {
              "customerId": "alpha",
              "totalAmount": 42.5,
              "active": true,
              "createdAt": "2024-01-02T03:04:05"
            }
            """;

        var dto = objectMapper.readValue(json, FluentOrderDto.class);

        assertThat(dto.customerId()).isEqualTo("alpha");
        assertThat(dto.totalAmount()).isEqualByComparingTo("42.5");
        assertThat(dto.active()).isTrue();
        assertThat(dto.createdAt()).isEqualTo(LocalDateTime.of(2024, 1, 2, 3, 4, 5));
    }

    @Test
    void serializeFluentDto() {
        var dto = new FluentOrderDto("beta", new BigDecimal("19.99"), false, LocalDateTime.of(2024, 2, 3, 4, 5, 6));

        var json = objectMapper.writeValueAsString(dto);

        assertThat(json)
                .contains("\"customerId\":\"beta\"")
                .contains("\"totalAmount\":19.99")
                .contains("\"active\":false")
                .contains("\"createdAt\":\"2024-02-03T04:05:06\"");
    }

    @Getter
    @Accessors(fluent = true)
    static class FluentOrderDto {

        @JsonProperty("customerId")
        private final String customerId;

        @JsonProperty("totalAmount")
        private final BigDecimal totalAmount;

        @JsonProperty("active")
        private final boolean active;

        @JsonProperty("createdAt") // serialize
        private final LocalDateTime createdAt;

        @JsonCreator // deserialize
        FluentOrderDto(
                @JsonProperty("customerId") String customerId,
                @JsonProperty("totalAmount") BigDecimal totalAmount,
                @JsonProperty("active") boolean active,
                @JsonProperty("createdAt") LocalDateTime createdAt) {
            this.customerId = customerId;
            this.totalAmount = totalAmount;
            this.active = active;
            this.createdAt = createdAt;
        }
    }
}
