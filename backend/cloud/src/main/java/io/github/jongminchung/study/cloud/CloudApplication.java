package io.github.jongminchung.study.cloud;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.modulith.Modulith;

@Modulith
@EnableJpaAuditing
@SpringBootApplication
public class CloudApplication {

    static void main(String[] args) {
        SpringApplication.run(CloudApplication.class, args);
    }
}
