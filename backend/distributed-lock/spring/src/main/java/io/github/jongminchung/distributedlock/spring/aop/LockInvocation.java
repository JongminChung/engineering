package io.github.jongminchung.distributedlock.spring.aop;

import java.lang.reflect.Method;

public record LockInvocation(Object target, Method method, Object[] args) {}
