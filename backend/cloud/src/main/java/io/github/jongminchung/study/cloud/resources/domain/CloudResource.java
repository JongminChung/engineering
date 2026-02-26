package io.github.jongminchung.study.cloud.resources.domain;

import java.time.OffsetDateTime;

import jakarta.persistence.*;

import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "cloud_resources")
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@NoArgsConstructor
public class CloudResource {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String type; // e.g., "EC2", "S3", "VPC"

    @Column(nullable = false)
    private String provider; // e.g., "AWS", "GCP", "AZURE"

    @Column(nullable = false)
    private String region;

    @Column(nullable = false)
    private String status; // e.g., "PROVISIONING", "RUNNING", "STOPPED", "DELETED"

    @Version
    private Long version;

    @CreatedDate
    @Column(nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @LastModifiedDate
    @Column(nullable = false)
    private OffsetDateTime updatedAt;

    public CloudResource(String name, String type, String provider, String region, String status) {
        this.name = name;
        this.type = type;
        this.provider = provider;
        this.region = region;
        this.status = status;
    }
}
