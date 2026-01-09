package io.github.jongminchung.study.apicommunication.context;

import java.util.UUID;

/** Rich metadata that accompanies every inbound request. */
public record ApiRequestContext(String tenantId, String clientId, String traceId) {

    public ApiRequestContext(String tenantId, String clientId, String traceId) {
        this.tenantId = requireNonBlank(tenantId, "tenantId");
        this.clientId = requireNonBlank(clientId, "clientId");
        this.traceId =
                (traceId == null || traceId.isBlank()) ? UUID.randomUUID().toString() : traceId;
    }

    private static String requireNonBlank(String value, String label) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(label + " must be provided");
        }
        return value;
    }

    public String asRateLimitKey(String operation) {
        return tenantId + ':' + clientId + ':' + operation;
    }

    public String asCacheKey() {
        return tenantId + ':' + clientId;
    }
}
