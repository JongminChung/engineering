package io.github.jongminchung.study.cloud.iam;

import java.util.List;

import org.springframework.stereotype.Component;

@Component
public class PolicyDecisionPoint {
    private final List<PolicyRule> rules;

    public PolicyDecisionPoint() {
        this.rules = List.of(
                new PolicyRule("ADMIN", "user", Action.READ),
                new PolicyRule("ADMIN", "user", Action.CREATE),
                new PolicyRule("USER", "user", Action.READ));
    }

    public boolean decide(AccessPrincipal principal, Permission permission) {
        return rules.stream().anyMatch(rule -> rule.matches(principal, permission));
    }
}
