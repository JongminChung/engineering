package io.github.jongminchung.study.cloud.iam;

import org.springframework.stereotype.Component;

@Component
public class PolicyEnforcementPoint {
    private final PolicyDecisionPoint policyDecisionPoint;

    public PolicyEnforcementPoint(PolicyDecisionPoint policyDecisionPoint) {
        this.policyDecisionPoint = policyDecisionPoint;
    }

    public void require(AccessPrincipal principal, Permission permission) {
        if (!policyDecisionPoint.decide(principal, permission)) {
            throw new PolicyDeniedException(
                    "Policy denied for %s:%s".formatted(permission.resource(), permission.action()));
        }
    }
}
