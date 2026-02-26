package io.github.jongminchung.study.cloud.resources.api;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.time.OffsetDateTime;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import io.github.jongminchung.study.cloud.common.api.GlobalExceptionHandler;
import io.github.jongminchung.study.cloud.resources.ResourceNotFoundException;
import io.github.jongminchung.study.cloud.resources.service.CloudResourceService;

import tools.jackson.databind.ObjectMapper;

@SpringBootTest
@AutoConfigureMockMvc
@Import(GlobalExceptionHandler.class)
@WithMockUser
public class CloudResourceControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private CloudResourceService service;

    @Test
    void createResourceShouldReturnCreated() throws Exception {
        CloudResourceCreateRequest request = new CloudResourceCreateRequest("my-ec2", "EC2", "AWS", "us-east-1");
        CloudResourceResponse response = new CloudResourceResponse(
                1L,
                "my-ec2",
                "EC2",
                "AWS",
                "us-east-1",
                "PROVISIONING",
                0L,
                OffsetDateTime.now(),
                OffsetDateTime.now());

        when(service.createResource(any(CloudResourceCreateRequest.class))).thenReturn(response);

        mockMvc.perform(post("/api/v1/cloud-resources")
                        .with(csrf())
                        .header("Idempotency-Key", "uuid-1234")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1L))
                .andExpect(jsonPath("$.name").value("my-ec2"));
    }

    @Test
    void getAllResourcesShouldReturnPage() throws Exception {
        CloudResourceResponse response = new CloudResourceResponse(
                1L, "my-ec2", "EC2", "AWS", "us-east-1", "RUNNING", 0L, OffsetDateTime.now(), OffsetDateTime.now());
        PageImpl<CloudResourceResponse> page = new PageImpl<>(List.of(response), PageRequest.of(0, 10), 1);

        when(service.getAllResources(any(PageRequest.class))).thenReturn(page);

        mockMvc.perform(get("/api/v1/cloud-resources").param("page", "0").param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(1L));
    }

    @Test
    void getResourceShouldReturnResource() throws Exception {
        CloudResourceResponse response = new CloudResourceResponse(
                1L, "my-ec2", "EC2", "AWS", "us-east-1", "RUNNING", 0L, OffsetDateTime.now(), OffsetDateTime.now());

        when(service.getResource(1L)).thenReturn(response);

        mockMvc.perform(get("/api/v1/cloud-resources/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1L));
    }

    @Test
    void getResourceShouldReturnNotFoundWhenMissing() throws Exception {
        when(service.getResource(1L)).thenThrow(new ResourceNotFoundException(1L));

        mockMvc.perform(get("/api/v1/cloud-resources/1"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.title").value("Resource Not Found"))
                .andExpect(jsonPath("$.resourceId").value(1L));
    }

    @Test
    void updateResourceShouldHandleConcurrencyFailure() throws Exception {
        CloudResourceUpdateRequest request = new CloudResourceUpdateRequest("updated-ec2", "us-west-2", "RUNNING", 0L);

        when(service.updateResource(eq(1L), any(CloudResourceUpdateRequest.class)))
                .thenThrow(new ObjectOptimisticLockingFailureException("CloudResource", 1L));

        mockMvc.perform(put("/api/v1/cloud-resources/1")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.title").value("Concurrency Failure"));
    }

    @Test
    void deleteResourceShouldReturnNoContent() throws Exception {
        mockMvc.perform(delete("/api/v1/cloud-resources/1").with(csrf())).andExpect(status().isNoContent());
    }
}
