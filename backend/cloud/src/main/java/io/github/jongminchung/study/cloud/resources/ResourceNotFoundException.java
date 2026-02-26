package io.github.jongminchung.study.cloud.resources;

import lombok.Getter;

@Getter
public class ResourceNotFoundException extends RuntimeException {
    private final Long resourceId;

    public ResourceNotFoundException(Long resourceId) {
        super("Cloud resource with ID " + resourceId + " not found.");
        this.resourceId = resourceId;
    }
}
