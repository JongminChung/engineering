package io.github.jongminchung.study.apicommunication.ratelimit;

import io.github.jongminchung.study.apicommunication.context.ApiRequestContext;

public interface RateLimiter {
    boolean tryAcquire(ApiRequestContext context, RateLimitedOperation operation);
}
