package io.github.jongminchung.distributedlock.spring.aop;

import java.lang.reflect.Method;
import java.time.Duration;
import java.util.Objects;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;

import io.github.jongminchung.distributedlock.core.api.DistributedLock;
import io.github.jongminchung.distributedlock.core.api.LockHandle;
import io.github.jongminchung.distributedlock.core.api.LockRequest;
import io.github.jongminchung.distributedlock.core.key.LockKey;
import io.github.jongminchung.distributedlock.spring.resolver.LockKeyResolver;

@Aspect
public class DistributedLockAspect {
    private final DistributedLock distributedLock;
    private final LockKeyResolver lockKeyResolver;

    public DistributedLockAspect(DistributedLock distributedLock, LockKeyResolver lockKeyResolver) {
        this.distributedLock = Objects.requireNonNull(distributedLock, "distributedLock");
        this.lockKeyResolver = Objects.requireNonNull(lockKeyResolver, "lockKeyResolver");
    }

    @Around("@annotation(io.github.jongminchung.distributedlock.spring.annotation.DistributedLock)")
    public Object around(ProceedingJoinPoint joinPoint) throws Throwable {
        Method method = ((MethodSignature) joinPoint.getSignature()).getMethod();
        io.github.jongminchung.distributedlock.spring.annotation.DistributedLock annotation =
                method.getAnnotation(io.github.jongminchung.distributedlock.spring.annotation.DistributedLock.class);
        LockOperation operation = new LockOperation(
                annotation.key(),
                durationFromMs(annotation.waitTimeMs()),
                durationFromMs(annotation.leaseTimeMs()),
                annotation.fair());
        LockInvocation invocation = new LockInvocation(joinPoint.getTarget(), method, joinPoint.getArgs());
        LockKey key = lockKeyResolver.resolve(invocation, operation);
        LockRequest request = LockRequest.of(key, operation.waitTime(), operation.leaseTime(), operation.fair());

        LockHandle handle = distributedLock.acquire(request);
        try {
            return joinPoint.proceed();
        } finally {
            handle.release();
        }
    }

    private Duration durationFromMs(long value) {
        if (value < 0) {
            return Duration.ZERO;
        }
        return Duration.ofMillis(value);
    }
}
