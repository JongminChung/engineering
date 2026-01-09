package io.github.jongminchung.distributedlock.spring.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface DistributedLock {
    String key() default "";

    long waitTimeMs() default -1;

    long leaseTimeMs() default -1;

    boolean fair() default false;
}
