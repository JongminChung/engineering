package io.github.jongminchung.study.cloud;

import org.junit.jupiter.api.Test;
import org.springframework.modulith.core.ApplicationModules;

class ModulithTests {
    private final ApplicationModules modules = ApplicationModules.of(CloudApplication.class);

    @Test
    void verifyModules() {
        modules.verify();
    }
}
