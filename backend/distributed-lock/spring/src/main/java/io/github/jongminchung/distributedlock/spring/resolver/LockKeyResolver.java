package io.github.jongminchung.distributedlock.spring.resolver;

import io.github.jongminchung.distributedlock.core.key.LockKey;
import io.github.jongminchung.distributedlock.spring.aop.LockInvocation;
import io.github.jongminchung.distributedlock.spring.aop.LockOperation;

public interface LockKeyResolver {
    LockKey resolve(LockInvocation invocation, LockOperation operation);
}
