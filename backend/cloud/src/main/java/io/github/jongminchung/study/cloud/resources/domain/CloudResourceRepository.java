package io.github.jongminchung.study.cloud.resources.domain;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CloudResourceRepository extends JpaRepository<CloudResource, Long> {}
