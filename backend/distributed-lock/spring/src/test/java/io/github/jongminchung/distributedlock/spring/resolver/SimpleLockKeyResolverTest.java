package io.github.jongminchung.distributedlock.spring.resolver;

import java.lang.reflect.Method;
import java.time.Duration;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

import io.github.jongminchung.distributedlock.core.key.SimpleLockKeyStrategy;
import io.github.jongminchung.distributedlock.spring.aop.LockInvocation;
import io.github.jongminchung.distributedlock.spring.aop.LockOperation;
import io.github.jongminchung.distributedlock.spring.expression.LockKeyExpressionEvaluator;

class SimpleLockKeyResolverTest {
    @Test
    void resolvesKeyFromExpression() throws Exception {
        LockKeyExpressionEvaluator evaluator = new LockKeyExpressionEvaluator();
        SimpleLockKeyStrategy strategy = new SimpleLockKeyStrategy();
        SimpleLockKeyResolver resolver = new SimpleLockKeyResolver(evaluator, strategy);
        Method method = Sample.class.getDeclaredMethod("handle", String.class);
        LockInvocation invocation = new LockInvocation(new Sample(), method, new Object[] {"user-1"});
        LockOperation operation = new LockOperation("#args[0]", Duration.ZERO, Duration.ZERO, false);

        Assertions.assertThat(resolver.resolve(invocation, operation).value()).isEqualTo("user-1");
    }

    @Test
    void fallsBackToMethodSignatureWhenExpressionBlank() throws Exception {
        LockKeyExpressionEvaluator evaluator = new LockKeyExpressionEvaluator();
        SimpleLockKeyStrategy strategy = new SimpleLockKeyStrategy();
        SimpleLockKeyResolver resolver = new SimpleLockKeyResolver(evaluator, strategy);
        Method method = Sample.class.getDeclaredMethod("handle", String.class);
        LockInvocation invocation = new LockInvocation(new Sample(), method, new Object[] {"user-1"});
        LockOperation operation = new LockOperation("", Duration.ZERO, Duration.ZERO, false);

        Assertions.assertThat(resolver.resolve(invocation, operation).value())
                .contains("SimpleLockKeyResolverTest$Sample#handle");
    }

    private static class Sample {
        void handle(String ignored) {}
    }
}
