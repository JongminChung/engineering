package io.github.jongminchung.distributedlock.spring.resolver;

import java.util.Objects;

import io.github.jongminchung.distributedlock.core.key.LockKey;
import io.github.jongminchung.distributedlock.core.key.LockKeyStrategy;
import io.github.jongminchung.distributedlock.spring.aop.LockInvocation;
import io.github.jongminchung.distributedlock.spring.aop.LockOperation;
import io.github.jongminchung.distributedlock.spring.expression.LockKeyExpressionEvaluator;

public class SimpleLockKeyResolver implements LockKeyResolver {
    private final LockKeyExpressionEvaluator evaluator;
    private final LockKeyStrategy keyStrategy;

    public SimpleLockKeyResolver(LockKeyExpressionEvaluator evaluator, LockKeyStrategy keyStrategy) {
        this.evaluator = Objects.requireNonNull(evaluator, "evaluator");
        this.keyStrategy = Objects.requireNonNull(keyStrategy, "keyStrategy");
    }

    @Override
    public LockKey resolve(LockInvocation invocation, LockOperation operation) {
        String resolved = evaluator.evaluate(operation.keyExpression(), invocation.method(), invocation.args());
        if (resolved == null || resolved.isBlank()) {
            resolved = invocation.method().getDeclaringClass().getName() + "#"
                    + invocation.method().getName();
        }
        return keyStrategy.create(resolved);
    }
}
