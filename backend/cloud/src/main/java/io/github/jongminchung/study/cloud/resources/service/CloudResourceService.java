package io.github.jongminchung.study.cloud.resources.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import io.github.jongminchung.study.cloud.resources.ResourceNotFoundException;
import io.github.jongminchung.study.cloud.resources.api.CloudResourceCreateRequest;
import io.github.jongminchung.study.cloud.resources.api.CloudResourceResponse;
import io.github.jongminchung.study.cloud.resources.api.CloudResourceUpdateRequest;
import io.github.jongminchung.study.cloud.resources.domain.CloudResource;
import io.github.jongminchung.study.cloud.resources.domain.CloudResourceRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class CloudResourceService {

    private final CloudResourceRepository repository;

    @Transactional
    public CloudResourceResponse createResource(CloudResourceCreateRequest request) {
        CloudResource resource =
                new CloudResource(request.name(), request.type(), request.provider(), request.region(), "PROVISIONING");
        CloudResource saved = repository.save(resource);
        return mapToResponse(saved);
    }

    @Transactional(readOnly = true)
    public Page<CloudResourceResponse> getAllResources(Pageable pageable) {
        return repository.findAll(pageable).map(this::mapToResponse);
    }

    @Transactional(readOnly = true)
    public CloudResourceResponse getResource(Long id) {
        return repository.findById(id).map(this::mapToResponse).orElseThrow(() -> new ResourceNotFoundException(id));
    }

    @Transactional
    public CloudResourceResponse updateResource(Long id, CloudResourceUpdateRequest request) {
        CloudResource resource = repository.findById(id).orElseThrow(() -> new ResourceNotFoundException(id));

        if (!resource.getVersion().equals(request.version())) {
            throw new ObjectOptimisticLockingFailureException(CloudResource.class, id);
        }

        resource.setName(request.name());
        resource.setRegion(request.region());
        resource.setStatus(request.status());

        return mapToResponse(repository.save(resource));
    }

    @Transactional
    public void deleteResource(Long id) {
        if (!repository.existsById(id)) {
            throw new ResourceNotFoundException(id);
        }
        repository.deleteById(id);
    }

    private CloudResourceResponse mapToResponse(CloudResource resource) {
        return new CloudResourceResponse(
                resource.getId(),
                resource.getName(),
                resource.getType(),
                resource.getProvider(),
                resource.getRegion(),
                resource.getStatus(),
                resource.getVersion(),
                resource.getCreatedAt(),
                resource.getUpdatedAt());
    }
}
