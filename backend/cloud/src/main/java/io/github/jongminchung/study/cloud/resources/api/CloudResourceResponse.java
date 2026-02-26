package io.github.jongminchung.study.cloud.resources.api;

import java.time.OffsetDateTime;

public record CloudResourceResponse(
        Long id,
        String name,
        String type,
        String provider,
        String region,
        String status,
        Long version,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt) {}
