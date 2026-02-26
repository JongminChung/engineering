package io.github.jongminchung.study.cloud.resources.api;

import jakarta.validation.constraints.NotBlank;

public record CloudResourceCreateRequest(
        @NotBlank String name,
        @NotBlank String type,
        @NotBlank String provider,
        @NotBlank String region) {}
