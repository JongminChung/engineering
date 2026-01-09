package io.github.jongminchung.distributedlock.core.policy;

import java.time.Duration;

public interface LeasePolicy {
    Duration leaseTime();

    boolean watchdogEnabled();
}
