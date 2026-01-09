package io.github.jongminchung.study.apicommunication.orders.api;

import java.time.Instant;

public record ApiErrorResponse(String message, String traceId, Instant timestamp) {
    public static ApiErrorResponse of(String message, String traceId) {
        return new ApiErrorResponse(message, traceId, Instant.now());
    }
}
