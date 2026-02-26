package io.github.jongminchung.study.cloud.resources.api;

import jakarta.validation.Valid;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import io.github.jongminchung.study.cloud.resources.service.CloudResourceService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/cloud-resources")
@RequiredArgsConstructor
public class CloudResourceController {

    private final CloudResourceService service;

    @PostMapping
    public ResponseEntity<CloudResourceResponse> createResource(
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
            @RequestBody @Valid CloudResourceCreateRequest request) {
        // In a real scenario, use the idempotencyKey to prevent duplicate processing
        CloudResourceResponse response = service.createResource(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    public Page<CloudResourceResponse> getAllResources(Pageable pageable) {
        return service.getAllResources(pageable);
    }

    @GetMapping("/{id}")
    public CloudResourceResponse getResource(@PathVariable Long id) {
        return service.getResource(id);
    }

    @PutMapping("/{id}")
    public CloudResourceResponse updateResource(
            @PathVariable Long id, @RequestBody @Valid CloudResourceUpdateRequest request) {
        return service.updateResource(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteResource(@PathVariable Long id) {
        service.deleteResource(id);
    }
}
