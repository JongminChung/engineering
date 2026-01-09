package io.github.jongminchung.study.cloud.auth;

import org.springframework.stereotype.Component;

import io.github.jongminchung.study.cloud.iam.AccessPrincipal;
import io.github.jongminchung.study.cloud.iam.Permission;
import io.github.jongminchung.study.cloud.iam.PolicyEnforcementPoint;

@Component
public class AuthorizationService {
    private final PolicyEnforcementPoint policyEnforcementPoint;

    public AuthorizationService(PolicyEnforcementPoint policyEnforcementPoint) {
        this.policyEnforcementPoint = policyEnforcementPoint;
    }

    public void require(AccessPrincipal principal, Permission permission) {
        policyEnforcementPoint.require(principal, permission);
    }
}
