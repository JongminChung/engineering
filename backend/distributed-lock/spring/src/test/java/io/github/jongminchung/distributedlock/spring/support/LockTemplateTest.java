package io.github.jongminchung.distributedlock.spring.support;

import java.util.Optional;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import io.github.jongminchung.distributedlock.core.api.LockHandle;
import io.github.jongminchung.distributedlock.core.api.LockRequest;
import io.github.jongminchung.distributedlock.test.fake.InMemoryDistributedLock;
import io.github.jongminchung.distributedlock.test.fixture.LockTestFixture;

class LockTemplateTest {
    private final InMemoryDistributedLock distributedLock = new InMemoryDistributedLock();
    private final LockTemplate lockTemplate = new LockTemplate(distributedLock);

    @Nested
    class ExecuteSupplier {
        @Test
        void releasesLockAfterSuccessfulExecution() {
            LockRequest request = LockTestFixture.defaultRequest("template:success");

            String result = lockTemplate.execute(request, () -> "ok");

            Optional<LockHandle> reacquired = distributedLock.tryAcquire(request);
            Assertions.assertThat(result).isEqualTo("ok");
            Assertions.assertThat(reacquired).isPresent();
            reacquired.get().release();
        }

        @Test
        void releasesLockAfterFailure() {
            LockRequest request = LockTestFixture.defaultRequest("template:failure");

            Assertions.assertThatThrownBy(() -> lockTemplate.execute(request, () -> {
                        throw new IllegalStateException("boom");
                    }))
                    .isInstanceOf(IllegalStateException.class);

            Optional<LockHandle> reacquired = distributedLock.tryAcquire(request);
            Assertions.assertThat(reacquired).isPresent();
            reacquired.get().release();
        }
    }

    @Nested
    class ExecuteRunnable {
        @Test
        void executesAndReleasesLock() {
            LockRequest request = LockTestFixture.defaultRequest("template:runnable");

            lockTemplate.execute(request, () -> {
                // no-op
            });

            Optional<LockHandle> reacquired = distributedLock.tryAcquire(request);
            Assertions.assertThat(reacquired).isPresent();
            reacquired.get().release();
        }
    }
}
