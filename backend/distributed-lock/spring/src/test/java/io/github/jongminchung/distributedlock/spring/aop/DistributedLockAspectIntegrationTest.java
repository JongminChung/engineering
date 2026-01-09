package io.github.jongminchung.distributedlock.spring.aop;

import java.time.Duration;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicReference;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.EnableAspectJAutoProxy;

import io.github.jongminchung.distributedlock.core.api.DistributedLock;
import io.github.jongminchung.distributedlock.core.api.LockHandle;
import io.github.jongminchung.distributedlock.core.api.LockRequest;
import io.github.jongminchung.distributedlock.core.key.LockKey;
import io.github.jongminchung.distributedlock.core.key.LockKeyStrategy;
import io.github.jongminchung.distributedlock.core.key.SimpleLockKeyStrategy;
import io.github.jongminchung.distributedlock.spring.expression.LockKeyExpressionEvaluator;
import io.github.jongminchung.distributedlock.spring.resolver.LockKeyResolver;
import io.github.jongminchung.distributedlock.spring.resolver.SimpleLockKeyResolver;
import io.github.jongminchung.distributedlock.test.fake.InMemoryDistributedLock;

class DistributedLockAspectIntegrationTest {
    private static ExecutorService executor;

    @BeforeAll
    static void setUpExecutor() {
        executor = Executors.newSingleThreadExecutor();
    }

    @AfterAll
    static void shutdownExecutor() {
        executor.shutdownNow();
    }

    @Nested
    class WhenAnnotatedMethodInvoked {
        @Test
        void locksAroundInvocation() throws ExecutionException, InterruptedException {
            try (AnnotationConfigApplicationContext context =
                    new AnnotationConfigApplicationContext(TestConfig.class)) {
                TestService service = context.getBean(TestService.class);
                DistributedLock lock = context.getBean(DistributedLock.class);
                CountDownLatch entered = new CountDownLatch(1);
                CountDownLatch release = new CountDownLatch(1);

                Future<?> future = executor.submit(() -> service.runWithLock(entered, release));
                entered.await();

                LockRequest request = LockRequest.of(LockKey.of("aop:1"), Duration.ZERO, Duration.ZERO, false);
                Optional<LockHandle> secondAttempt = lock.tryAcquire(request);
                Assertions.assertThat(secondAttempt).isEmpty();

                release.countDown();
                future.get();

                Optional<LockHandle> reacquired = lock.tryAcquire(request);
                Assertions.assertThat(reacquired).isPresent();
                reacquired.get().release();
            }
        }
    }

    @Nested
    class WhenSpelKeyUsed {
        @Test
        void resolvesParameterExpression() {
            try (AnnotationConfigApplicationContext context =
                    new AnnotationConfigApplicationContext(SpelConfig.class)) {
                SpelService service = context.getBean(SpelService.class);
                RecordingDistributedLock lock = context.getBean(RecordingDistributedLock.class);

                service.lockWithArg("order-9");

                Assertions.assertThat(lock.lastRequest())
                        .isPresent()
                        .get()
                        .extracting(value -> value.key().value())
                        .isEqualTo("order-9");
            }
        }

        @Test
        void resolvesMethodExpression() {
            try (AnnotationConfigApplicationContext context =
                    new AnnotationConfigApplicationContext(SpelConfig.class)) {
                SpelService service = context.getBean(SpelService.class);
                RecordingDistributedLock lock = context.getBean(RecordingDistributedLock.class);

                service.lockWithMethodInfo();

                Assertions.assertThat(lock.lastRequest())
                        .isPresent()
                        .get()
                        .extracting(value -> value.key().value())
                        .isEqualTo("lockWithMethodInfo");
            }
        }
    }

    @Configuration
    @EnableAspectJAutoProxy(proxyTargetClass = true)
    static class TestConfig {
        @Bean
        public DistributedLock distributedLock() {
            return new InMemoryDistributedLock();
        }

        @Bean
        public LockKeyExpressionEvaluator lockKeyExpressionEvaluator() {
            return new LockKeyExpressionEvaluator();
        }

        @Bean
        public LockKeyStrategy lockKeyStrategy() {
            return new SimpleLockKeyStrategy();
        }

        @Bean
        public LockKeyResolver lockKeyResolver(LockKeyExpressionEvaluator evaluator, LockKeyStrategy keyStrategy) {
            return new SimpleLockKeyResolver(evaluator, keyStrategy);
        }

        @Bean
        public DistributedLockAspect distributedLockAspect(
                DistributedLock distributedLock, LockKeyResolver lockKeyResolver) {
            return new DistributedLockAspect(distributedLock, lockKeyResolver);
        }

        @Bean
        public TestService testService() {
            return new TestService();
        }
    }

    @Configuration
    @EnableAspectJAutoProxy(proxyTargetClass = true)
    static class SpelConfig {
        @Bean
        public RecordingDistributedLock recordingDistributedLock() {
            return new RecordingDistributedLock();
        }

        @Bean
        public LockKeyExpressionEvaluator lockKeyExpressionEvaluator() {
            return new LockKeyExpressionEvaluator();
        }

        @Bean
        public LockKeyStrategy lockKeyStrategy() {
            return new SimpleLockKeyStrategy();
        }

        @Bean
        public LockKeyResolver lockKeyResolver(LockKeyExpressionEvaluator evaluator, LockKeyStrategy keyStrategy) {
            return new SimpleLockKeyResolver(evaluator, keyStrategy);
        }

        @Bean
        public DistributedLockAspect distributedLockAspect(
                DistributedLock distributedLock, LockKeyResolver lockKeyResolver) {
            return new DistributedLockAspect(distributedLock, lockKeyResolver);
        }

        @Bean
        public SpelService spelService() {
            return new SpelService();
        }
    }

    static class TestService {
        @io.github.jongminchung.distributedlock.spring.annotation.DistributedLock(
                key = "'aop:1'",
                waitTimeMs = 0,
                leaseTimeMs = 0)
        public void runWithLock(CountDownLatch entered, CountDownLatch release) {
            entered.countDown();
            try {
                release.await();
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
                throw new IllegalStateException("Interrupted", ex);
            }
        }
    }

    static class SpelService {
        @io.github.jongminchung.distributedlock.spring.annotation.DistributedLock(key = "#args[0]")
        public void lockWithArg(String key) {
            // no-op
        }

        @io.github.jongminchung.distributedlock.spring.annotation.DistributedLock(key = "#method.name")
        public void lockWithMethodInfo() {
            // no-op
        }
    }

    static class RecordingDistributedLock implements DistributedLock {
        private final AtomicReference<LockRequest> lastRequest = new AtomicReference<>();

        Optional<LockRequest> lastRequest() {
            return Optional.ofNullable(lastRequest.get());
        }

        @Override
        public LockHandle acquire(LockRequest request) {
            lastRequest.set(request);
            return new LockHandle() {
                @Override
                public LockKey key() {
                    return request.key();
                }

                @Override
                public void release() {
                    // no-op
                }
            };
        }

        @Override
        public Optional<LockHandle> tryAcquire(LockRequest request) {
            lastRequest.set(request);
            return Optional.empty();
        }
    }
}
