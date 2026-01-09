package io.github.jongminchung.distributedlock.spring.aop;

import java.time.Duration;

public record LockOperation(String keyExpression, Duration waitTime, Duration leaseTime, boolean fair) {}
