package io.github.jongminchung.study.cloud.resources.api;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CloudResourceUpdateRequest(
        @NotBlank String name,
        @NotBlank String region,
        @NotBlank String status,
        @NotNull Long version) {}
